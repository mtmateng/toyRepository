package com.toySpring.repository.utils;

import com.toySpring.repository.helper.EntityInfo;
import com.toySpring.repository.helper.ReturnValueHandler;
import com.toySpring.repository.helper.SelectSQLMethodInfo;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

public class SQLUtil {

    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE IF NOT EXISTS `%s` (%s %s) %s %s;";
    private static final Set<Class> NO_QUOTE_CLASS = new HashSet<Class>() {
        {
            add(Integer.class);
            add(int.class);
            add(double.class);
            add(Double.class);
        }
    };

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

    public static <T> Object executeSelectSQL(DataSource dataSource, SelectSQLMethodInfo methodInfo, Object[] args, Class<T> resultClass, EntityInfo entityInfo) {

        String sql = buildSelectSQLWithArgs(methodInfo.getSQLTemplate(), args);
        List<T> results = actualDoExecuteSelectSQL(dataSource, sql, resultClass, entityInfo, methodInfo.getSelectFieldsNames());
        return buildReturnValue(results, methodInfo);

    }

    private static String buildSelectSQLWithArgs(String sql, Object[] args) {

        return String.format(sql, args);

    }

    private static <T> List<T> actualDoExecuteSelectSQL(DataSource dataSource, String sql, Class<T> resultClass, EntityInfo entityInfo, List<String> selectFieldsNames) {

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

    private static <T> Object buildReturnValue(List<T> results, SelectSQLMethodInfo methodInfo) {

        if (results.size() > 1 && (methodInfo.getWrapClass() == null || methodInfo.getWrapClass() == Optional.class)) {

            throw new RuntimeException(String.format("超过一个返回结果，但该方法的返回值：%s只能容纳一个结果",
                methodInfo.getWrapClass() == null ? methodInfo.getActualClass().getName() : methodInfo.getWrapClass().getName()));
        }

        if (methodInfo.getWrapClass() == null) {
            return results.isEmpty() ? null : results.get(0);
        }

        if (methodInfo.getWrapClass() == Optional.class) {
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }

        Collection<T> ret;

        if (Collection.class == methodInfo.getWrapClass() || List.class == methodInfo.getWrapClass()) {
            ret = new ArrayList<>();
        } else if (Set.class == methodInfo.getWrapClass()) {
            ret = new HashSet<>();
        } else {
            throw new RuntimeException("集合类型当前只支持List和Set");
        }
        ret.addAll(results);
        return ret;

    }

    public static String actualBuildSelectSQLTemplate(EntityInfo entityInfo, List<String> selectFieldNames, List<String> queryFieldNames) {
        StringBuilder sqlSb = new StringBuilder().append("SELECT ");
        for (String selectFieldName : selectFieldNames) {
            sqlSb.append(entityInfo.getFiledName2DBName().get(selectFieldName)).append(", ");
        }
        sqlSb.delete(sqlSb.length() - 2, sqlSb.length()).append(" FROM ").append(entityInfo.getEntityDBName());
        sqlSb.append(" WHERE ");

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

    public static <T> void executeUpdateSQL(DataSource dataSource, EntityInfo entityInfo, T entity) {

        String sql = buildUpdateSQLWithArgs(entityInfo, entity);

        actualExecuteSQL(dataSource, sql);

    }

    private static void actualExecuteSQL(DataSource dataSource, String sql) {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);

        } catch (SQLException e) {
            throw new RuntimeException(String.format("执行SQL语句：%s失败", sql), e);
        }

    }

    private static <T> String buildUpdateSQLWithArgs(EntityInfo entityInfo, T entity) {

        try {
            StringBuilder sb = new StringBuilder("UPDATE ").append(entityInfo.getEntityDBName()).append(" SET ");
            for (String fieldName : entityInfo.getFiledName2DBName().keySet()) {
                PropertyDescriptor prop = new PropertyDescriptor(fieldName, entityInfo.getEntityClass());
                sb.append(entityInfo.getFiledName2DBName().get(fieldName))
                    .append(" = ")
                    .append(NO_QUOTE_CLASS.contains(entityInfo.getFieldName2Type().get(fieldName)) ? "" : "'")
                    .append(prop.getReadMethod().invoke(entity))
                    .append(NO_QUOTE_CLASS.contains(entityInfo.getFieldName2Type().get(fieldName)) ? "" : "'")
                    .append(" , ");
            }
            PropertyDescriptor prop = new PropertyDescriptor(entityInfo.getIdFieldName(), entityInfo.getEntityClass());
            sb.delete(sb.length() - 3, sb.length()).append(" WHERE ").append(entityInfo.getIdDBName()).append(" = ")
                .append(NO_QUOTE_CLASS.contains(entityInfo.getFieldName2Type().get(entityInfo.getIdFieldName())) ? prop.getReadMethod().invoke(entity) : "'" + prop.getReadMethod().invoke(entity) + "'");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("为%s构建Update语句时失败", entity));
        }

    }


    public static <T> void executeInsertSQL(DataSource dataSource, EntityInfo entityInfo, T entity) {

        String sql = buildInsertSQLWithArgs(entityInfo, entity);
        actualExecuteSQL(dataSource, sql);

    }

    private static <T> String buildInsertSQLWithArgs(EntityInfo entityInfo, T entity) {

        try {
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(entityInfo.getEntityDBName()).append(" (");
            Set<String> fieldNames = entityInfo.getFiledName2DBName().keySet();
            for (String fieldName : fieldNames) {
                sb.append(entityInfo.getFiledName2DBName().get(fieldName)).append(" , ");
            }
            sb.delete(sb.length() - 2, sb.length()).append(") VALUES (");
            for (String fieldName : entityInfo.getFiledName2DBName().keySet()) {
                PropertyDescriptor prop = new PropertyDescriptor(fieldName, entityInfo.getEntityClass());
                sb.append(NO_QUOTE_CLASS.contains(entityInfo.getFieldName2Type().get(fieldName)) ? "" : "'")
                    .append(prop.getReadMethod().invoke(entity))
                    .append(NO_QUOTE_CLASS.contains(entityInfo.getFieldName2Type().get(fieldName)) ? "" : "'")
                    .append(" , ");

            }
            sb.delete(sb.length() - 3, sb.length()).append(")");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("为%s构建Insert语句时失败", entity));
        }

    }

}
