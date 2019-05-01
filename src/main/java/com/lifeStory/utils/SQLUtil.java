package com.lifeStory.utils;

import com.lifeStory.helper.EntityInfo;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class SQLUtil {

    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE IF NOT EXISTS `%s` (%s %s) %s %s;";

    static String generateCreateTableSql(EntityInfo entityInfo) {

        StringBuilder fieldClaus = new StringBuilder();
        for (String fieldName : entityInfo.getFieldName2Type().keySet()) {
            fieldClaus.append(entityInfo.getFiledName2DBName().get(fieldName))
                .append(" ")
                .append(generateDBType(entityInfo.getFieldName2Type().get(fieldName)))
                .append(", ");
        }
        return String.format(CREATE_TABLE_TEMPLATE, entityInfo.getEntityDBName()
            , fieldClaus.toString(), getPrimaryKeyClaus(entityInfo.getIdDBName()),
            getEngineClaus(), getCharSetClaus());

    }

    /**
     * 根据method的相关参数，来生成sql的template String
     */
    public static String buildSQLTemplate(Method method, EntityInfo entityInfo,
                                          Class domainClass, Class repo) {

        if (method.getName().startsWith("find")) {
            return buildSelectSQLTemplate(method, entityInfo, domainClass, repo);
        } else {
            throw new RuntimeException("目前仅支持find方法");
        }

    }

    private static String buildSelectSQLTemplate(Method method, EntityInfo entityInfo, Class domainClass, Class repo) {

        int count = 0;
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (parameterType == Class.class) {
                ++count;
            }
        }
        if (count == 1) {
            return null;
        } else if (count > 1) {
            throw new RuntimeException(String.format("%s.%s()有一个以上的Class参数，请检查", repo.getName(), method.getName()));
        }

        List<String> selectFieldNames = getSelectFieldNamesByMethod(method, domainClass, entityInfo);
        List<String> queryFieldNames = getQueryFiledNamesByMethodName(method, entityInfo);

        StringBuilder sqlSb = new StringBuilder().append("select ");
        for (String selectFieldName : selectFieldNames) {
            sqlSb.append(entityInfo.getFiledName2DBName().get(selectFieldName)).append(", ");
        }
        sqlSb.delete(sqlSb.length() - 2, sqlSb.length()).append(" from ").append(entityInfo.getEntityDBName());
        sqlSb.append(" where ");

        for (int i = 0; i != queryFieldNames.size(); ++i) {
            sqlSb.append(entityInfo.getFiledName2DBName().get(queryFieldNames.get(i))).append("=");
            if (entityInfo.getFieldName2Type().get(queryFieldNames.get(i).toLowerCase()) == String.class
                || entityInfo.getFieldName2Type().get(queryFieldNames.get(i).toLowerCase()) == LocalDate.class) {
                sqlSb.append(" '").append("%s").append("' ").append("and ");
            } else {
                sqlSb.append(" %s ").append(" and ");
            }
        }

        return sqlSb.toString().replaceFirst(" and $", "");

    }

    //检查一下方法名、实体类字段类型和参数类型是否匹配
    private static void checkQueryParams(List<String> queryFieldNames, Method method, EntityInfo entityInfo) {

        if (queryFieldNames.size() != method.getParameters().length) {
            throw new RuntimeException("参数数量错误：" + method.getName());
        }

        // 检查一下参数类型和Entity类型是否匹配
        for (int i = 0; i != method.getParameters().length; ++i) {
            if (entityInfo.getFieldName2Type().get(queryFieldNames.get(i)) == null ||
                !entityInfo.getFieldName2Type().get(queryFieldNames.get(i)).equals(method.getParameters()[i].getType())) {
                throw new RuntimeException("第" + i + "个参数类型错误：" + method.getName());
            }
        }

    }

    /**
     * Spring JPA对于返回字段的支持很丰富，我们这里只实现domainClass、Optional、Collection、List、Set这几种
     * Spring JPA对于只查询部分字段的支持包括两种:
     * 一种是返回值指定为一个接口，该接口中有一些get方法，get的字段名、返回值和domainClass
     * 对应的字段值名字相同、返回值能匹配上。
     * 一种是动态传入一个Class参数，返回该Class或其Optional、Collection、List、Set容器类等。
     * 针对第一种可以静态解析返回的接口，但针对动态传入的Class，显然无法生成静态的SQL，只能推迟到动态来做
     */
    private static List<String> getSelectFieldNamesByMethod(Method method, Class domainClass, EntityInfo entityInfo) {

        //先看返回值是不是一个interface什么的
        List<String> ret;
        Class realReturnType = checkAndGetReturnType(method, method.getGenericReturnType());
        if (realReturnType == domainClass) {
            ret = new ArrayList<>(entityInfo.getFieldName2Type().keySet());
        } else if (realReturnType.isInterface()) {  //是一个interface
            ret = checkAndReturnFieldNames(realReturnType, domainClass, entityInfo);
        } else {
            throw new RuntimeException(String.format("%s.%s()的返回值类型不支持", method.getDeclaringClass().getName(), method.getName()));
        }

        return ret;

    }

    private static List<String> checkAndReturnFieldNames(Class realReturnType, Class domainClass, EntityInfo entityInfo) {

        List<String> ret = new ArrayList<>();
        for (Method declaredMethod : realReturnType.getDeclaredMethods()) {
            String fieldName = NameUtil.firstCharToLowerCase(declaredMethod.getName().replaceFirst("^get", ""));
            if (entityInfo.getFieldName2Type().get(fieldName) != declaredMethod.getReturnType()) {
                throw new RuntimeException(String.format("%s.%s()的返回类型与%s中的字段%s不匹配，请检查", realReturnType.getName(), declaredMethod.getName(), domainClass.getName(), fieldName));
            }
            ret.add(fieldName);
        }
        return ret;

    }


    /**
     * 检查一下返回值，是不是符合Option、Collection、List、Set
     */
    private static Class checkAndGetReturnType(Method method, Type genericReturnType) {

        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            if (parameterizedType.getActualTypeArguments().length != 1) {
                throw new RuntimeException(String.format("%s.%s()的返回值有多个类型化参数", method.getDeclaringClass().getName(), method.getName()));
            }
            Class rawType = (Class) parameterizedType.getRawType();
            if (rawType != Collection.class && List.class.isAssignableFrom(rawType)
                && Set.class.isAssignableFrom(rawType) && rawType != Optional.class) {
                throw new RuntimeException(String.format("%s.%s()的返回格式不支持，仅支持List、Collection、Set和Optional，以及原始类", method.getDeclaringClass().getName(), method.getName()));
            }
            return (Class) parameterizedType.getActualTypeArguments()[0];
        } else if (genericReturnType instanceof TypeVariable) {
            throw new RuntimeException("并不支持解析模板返回值");
        } else return (Class) genericReturnType;

    }

    /**
     * 只实现一个简版的，示例而已，即只允许find[get]XXXByName1AndName2(param1,param2)这种形式
     */
    private static List<String> getQueryFiledNamesByMethodName(Method method, EntityInfo entityInfo) {

        String removedStart = method.getName().replaceFirst("^(find|get)\\w?By", "");
        List<String> queryFieldNames = Arrays.stream(removedStart.split("And")).map(NameUtil::firstCharToLowerCase).collect(Collectors.toList());
        checkQueryParams(queryFieldNames, method, entityInfo);
        return queryFieldNames;

    }

    private static String getPrimaryKeyClaus(String idDBName) {
        return String.format("PRIMARY KEY (%s)", idDBName);
    }

    //这都是数据库相关的，这里先不做定制化处理了
    private static String getEngineClaus() {
        return "";
    }

    private static String getCharSetClaus() {
        return "DEFAULT CHARSET=utf8mb4";
    }

    /**
     * 产生需要的type字句，类似于varchar(255)这样子的东西，显然，我们这里支持的相当有限，也不支持Lob、自定义长度等
     * 意思到了就行
     */
    private static String generateDBType(Class aClass) {
        switch (aClass.getName()) {
            case "java.lang.String":
                return "VARCHAR(255)";
            case "java.lang.Integer":
                return "INT";
            case "java.time.LocalDate":
                return "DATE";
            default:
                throw new UnsupportedOperationException("尚未支持该类型");
        }
    }

    static void executeSQL(String sql, DataSource dataSource) {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("执行%s失败", sql), e);
        }
    }

    public static Object executeSelectSQL(DataSource dataSource, String SQL, Object[] args) {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SQL)) {

            return

        } catch (SQLException e) {
            throw new RuntimeException(String.format("执行%s失败", SQL), e);
        }
    }
}
