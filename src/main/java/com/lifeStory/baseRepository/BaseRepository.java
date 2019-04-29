package com.lifeStory.baseRepository;

import com.lifeStory.helper.EntityInfo;
import lombok.Getter;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

public class BaseRepository<T, ID> implements Repository<T, ID> {

    @Getter
    private final Class<T> domainClass;
    @Getter
    private final Class<ID> idClass;
    @Getter
    private final EntityInfo entityInfo;
    @Getter
    private final DataSource dataSource;

    public BaseRepository(Class<T> domainClass, Class<ID> idClass, EntityInfo entityInfo, DataSource dataSource) {

        this.domainClass = domainClass;
        this.idClass = idClass;
        this.entityInfo = entityInfo;
        this.dataSource = dataSource;

    }

    private String getEntityDBName() {
        return entityInfo.getEntityDBName();
    }

    // 示例如何做一个基础的方法，比如Repository中的findById方法;
    public T findById(ID id) {

        if (id == null) {
            throw new RuntimeException("id为null或空");
        }
        String sql = generateSelectSql(entityInfo.getEntityDBName(), entityInfo.getIdDBName(), id);
        return domainClass.cast(buildReturnValue(executeSelectSql(sql), domainClass, "findById"));

    }

//    public T save(T entity) {
//
//    }

    /**
     * 演示如何按methodName动态生成sql语句
     * Spring支持select部分字段，有两种方式，一个是返回值直接指定为一个interface，一个是传入一个Type参数
     * 如果返回值是一个接口，那么就需要用cglib来搞，如果传入一个Type参数，那就直接实例化一个就好
     */
    public Object executeSelectSqlByMethodName(Method method, Object[] args) {

        List<String> queryFieldNames = getQueryFiledNamesByMethodName(method.getName());
        List<String> selectFieldNames = getSelectFieldNamesByMethod(method);
        checkParams(queryFieldNames, args, method.getName());
        String sql = generateSelectSql(getEntityDBName(), queryFieldNames, args);
        List<T> results = executeSelectSql(sql);
        return buildReturnValue(results, method.getReturnType(), method.getName());

    }

    private void checkParams(List<String> fieldNames, Object[] args, String methodName) {

        if (args == null || fieldNames.size() != args.length) {
            throw new RuntimeException("参数数量错误：" + methodName);
        }
        for (int i = 0; i != args.length; ++i) {
            if (entityInfo.getFieldName2Type().get(fieldNames.get(i)) == null ||
                    !entityInfo.getFieldName2Type().get(fieldNames.get(i)).equals(args[0].getClass())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + methodName);
            }
        }
    }

    private String generateSelectSql(String entityName, String filedName, Object arg) {
        return generateSelectSql(entityName, new ArrayList<>(Collections.singleton(filedName)), new Object[]{arg});
    }

    private String generateSelectSql(String entityName, List<String> fieldNames, Object[] args) {

        StringBuilder sql = new StringBuilder()
                .append("select * from ")
                .append(entityInfo.getEntityDBName())
                .append(" where ");

        for (int i = 0; i != fieldNames.size(); ++i) {
            sql.append(entityInfo.getFiledName2DBName().get(fieldNames.get(i))).append("=");
            if (entityInfo.getFieldName2Type().get(fieldNames.get(i).toLowerCase()) == String.class
                    || entityInfo.getFieldName2Type().get(fieldNames.get(i).toLowerCase()) == LocalDate.class) {
                sql.append("'").append(args[i].toString()).append("'").append(" and ");
            } else {
                sql.append(args[i].toString()).append(" and ");
            }
        }

        return sql.toString().replaceFirst(" and $", "");
    }

    private List<T> executeSelectSql(String sql) {

        List<T> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            T result;
            try {
                result = domainClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(String.format("%s需要默认构造函数", domainClass.getName()));
            }
            while (resultSet.next()) {
                for (String fieldName : entityInfo.getFieldName2Type().keySet()) {
                    Class fieldType = entityInfo.getFieldName2Type().get(fieldName);
                    PropertyDescriptor prop = new PropertyDescriptor(fieldName, domainClass);
                    switch (fieldType.getName()) {
                        case "java.lang.String":
                            prop.getWriteMethod().invoke(result, resultSet.getString(entityInfo.getFiledName2DBName().get(fieldName)));
                            break;
                        case "java.lang.Integer":
                            prop.getWriteMethod().invoke(result, resultSet.getInt(entityInfo.getFiledName2DBName().get(fieldName)));
                            break;
                        case "java.time.LocalDate":
                            Date date = resultSet.getDate(entityInfo.getFiledName2DBName().get(fieldName));
                            // 真不敢相信Date以前竟然是java标准库的一部分，设计这个的程序员可能是临时工？
                            LocalDate localDate = LocalDate.of(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
                            prop.getWriteMethod().invoke(result, localDate);
                            break;
                        default:
                            throw new UnsupportedOperationException("尚未支持该类型");
                    }
                }
                results.add(result);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("执行%s时发生错误", sql), e);
        }
        return results;

    }

    private Object buildReturnValue(List<T> results, Class returnType, String methodName) {

        if (!Collection.class.isAssignableFrom(returnType) && results.size() > 1) {
            throw new RuntimeException(String.format("%s超过一个返回结果，但该方法的返回值：%s只能容纳一个结果",
                    methodName, returnType.getName()));
        }
        if (returnType == Optional.class) {
            if (results.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(results.get(0));
            }
        }
        if (returnType == domainClass) {
            return results.get(0);
        }
        if (Collection.class.isAssignableFrom(returnType)) {
            Collection<T> ret;
            if (List.class.isAssignableFrom(returnType)) {
                ret = new ArrayList<>();
            } else if (Set.class.isAssignableFrom(returnType)) {
                ret = new HashSet<>();
            } else throw new RuntimeException("集合类型当前只支持List和Set");
            ret.addAll(results);
            return ret;
        }
        throw new RuntimeException("返回值类型不支持，无法匹配");

    }

    public Method getDeclaredMethod(Method method) {
        for (Method declaredMethod : this.getClass().getMethods()) {
            if (declaredMethod.getName().equals(method.getName())
                    && declaredMethod.getParameterCount() == method.getParameterCount()) {
                int count = declaredMethod.getParameterCount();
                for (int index = 0; index != count; ++index) {
                    if (declaredMethod.getParameters()[index].getType() != method.getParameters()[index].getType()) {
                        return null;
                    }
                }
                return declaredMethod;
            }
        }
        return null;
    }
}
