package com.toySpring.repository.utils;

import com.toySpring.repository.helper.EntityInfo;
import com.toySpring.repository.helper.ReturnValueHandler;
import com.toySpring.repository.helper.SQLMethodInfo;
import javafx.util.Pair;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
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
    public static SQLMethodInfo buildSQLTemplate(Method method, EntityInfo entityInfo,
                                                 Class domainClass, Class repo) {

        if (method.getName().startsWith("find")) {
            return buildSelectSQLTemplate(method, entityInfo, domainClass, repo);
        } else {
            throw new RuntimeException("目前仅支持find方法");
        }

    }

    private static SQLMethodInfo buildSelectSQLTemplate(Method method, EntityInfo entityInfo, Class domainClass, Class repo) {

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

        SQLMethodInfo methodInfo = new SQLMethodInfo();
        methodInfo.setMethod(method);

        List<String> selectFieldNames = getSelectFieldNamesByMethod(method, domainClass, entityInfo, methodInfo);
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

        methodInfo.setSQL(sqlSb.toString().replaceFirst(" and $", ""));
        return methodInfo;

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
    private static List<String> getSelectFieldNamesByMethod(Method method, Class domainClass, EntityInfo entityInfo, SQLMethodInfo methodInfo) {

        //先看返回值是不是一个interface什么的
        List<String> ret;
        Pair<Class, Class> realReturnType = checkAndGetReturnType(method, method.getGenericReturnType());
        if (realReturnType.getValue() == domainClass) {
            ret = new ArrayList<>(entityInfo.getFieldName2Type().keySet());
        } else if (realReturnType.getValue().isInterface()) {  //是一个interface
            ret = checkAndReturnFieldNames(realReturnType.getValue(), domainClass, entityInfo);
        } else {
            throw new RuntimeException(String.format("%s.%s()的返回值类型不支持", method.getDeclaringClass().getName(), method.getName()));
        }

        methodInfo.setWrapClass(realReturnType.getKey());
        methodInfo.setActualClass(realReturnType.getValue());
        methodInfo.setSelectFieldsNames(ret);

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
    private static Pair<Class, Class> checkAndGetReturnType(Method method, Type genericReturnType) {

        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            if (parameterizedType.getActualTypeArguments().length != 1) {
                throw new RuntimeException(String.format("%s.%s()的返回值有多个类型化参数", method.getDeclaringClass().getName(), method.getName()));
            }
            Class rawType = (Class) parameterizedType.getRawType();
            if (rawType != Collection.class && List.class != rawType && Set.class != rawType && rawType != Optional.class) {
                throw new RuntimeException(String.format("%s.%s()的返回格式不支持，仅支持List、Collection、Set和Optional，以及原始类", method.getDeclaringClass().getName(), method.getName()));
            }
            if (rawType == Collection.class || rawType == List.class) {
                rawType = ArrayList.class;
            } else if (rawType == Set.class) {
                rawType = HashSet.class;
            }
            return new Pair<>(rawType, (Class) parameterizedType.getActualTypeArguments()[0]);
        } else if (genericReturnType instanceof TypeVariable) {
            throw new RuntimeException("并不支持解析模板返回值");
        } else return new Pair<>(null, (Class) genericReturnType);

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

    public static <T> Object executeSelectSQL(DataSource dataSource, SQLMethodInfo methodInfo, Object[] args, Class<T> resultClass, EntityInfo entityInfo) {

        String sql = buildSQLWithArgs(methodInfo.getSQL(), args);
        List<T> results = executeSelectSQL(dataSource, sql, resultClass, entityInfo, methodInfo.getSelectFieldsNames());
        return buildReturnValue(results, methodInfo, methodInfo.getMethod().getName());

    }

    private static String buildSQLWithArgs(String sql, Object[] args) {

        return String.format(sql, args);

    }

    private static <T> List<T> executeSelectSQL(DataSource dataSource, String sql, Class<T> resultClass, EntityInfo entityInfo, List<String> selectFieldsNames) {

        List<T> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Map<String, Object> sqlResultMap = extractSQLMap(entityInfo, resultSet, selectFieldsNames);
                T result = getResultInstance(resultClass, sqlResultMap);
                results.add(result);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("执行%s时发生错误", sql), e);
        }
        return results;

    }

    private static Map<String, Object> extractSQLMap(EntityInfo entityInfo, ResultSet resultSet, List<String> selectFieldNames) throws SQLException {

        Map<String, Object> ret = new HashMap<>();
        for (String fieldName : selectFieldNames) {
            Class fieldType = entityInfo.getFieldName2Type().get(fieldName);
            switch (fieldType.getName()) {
                case "java.lang.String":
                    ret.put(fieldName, resultSet.getString(entityInfo.getFiledName2DBName().get(fieldName)));
                    break;
                case "java.lang.Integer":
                    ret.put(fieldName, resultSet.getInt(entityInfo.getFiledName2DBName().get(fieldName)));
                    break;
                case "java.time.LocalDate":
                    Date date = resultSet.getDate(entityInfo.getFiledName2DBName().get(fieldName));
                    // 真不敢相信Date以前竟然是java标准库的一部分，设计这个的程序员可能是临时工？
                    LocalDate localDate = LocalDate.of(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
                    ret.put(fieldName, localDate);
                    break;
                default:
                    throw new UnsupportedOperationException("尚未支持该类型");
            }
        }
        return ret;

    }

    private static <T> T getResultInstance(Class<T> resultClass, Map<String, Object> sqlResultMap) throws Exception {

        if (resultClass.isInterface()) {
            return resultClass.cast(Proxy.newProxyInstance(
                resultClass.getClassLoader(), new Class<?>[]{resultClass}, new ReturnValueHandler(sqlResultMap)
            ));
        } else {
            T result = resultClass.newInstance();
            for (String key : sqlResultMap.keySet()) {
                PropertyDescriptor prop = new PropertyDescriptor(key, resultClass);
                prop.getWriteMethod().invoke(result, sqlResultMap.get(key));
            }
            return result;
        }
    }

    private static <T> Object buildReturnValue(List<T> results, SQLMethodInfo methodInfo, String methodName) {

        if (results.size() > 1 && (methodInfo.getWrapClass() == null || methodInfo.getWrapClass() == Optional.class)) {

            throw new RuntimeException(String.format("%s超过一个返回结果，但该方法的返回值：%s只能容纳一个结果",
                methodName, methodInfo.getWrapClass() == null ? methodInfo.getActualClass().getName() : methodInfo.getWrapClass().getName()));
        }

        if (methodInfo.getWrapClass() == null) {
            return results.isEmpty() ? null : results.get(0);
        }

        if (methodInfo.getWrapClass() == Optional.class) {
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }

        Collection<T> ret;

        if (ArrayList.class == methodInfo.getWrapClass()) {
            ret = new ArrayList<>();
        } else if (HashSet.class == methodInfo.getWrapClass()) {
            ret = new HashSet<>();
        } else {
            throw new RuntimeException("集合类型当前只支持List和Set");
        }
        ret.addAll(results);
        return ret;

    }
}
