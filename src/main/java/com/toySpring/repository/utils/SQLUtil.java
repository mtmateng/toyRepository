package com.toySpring.repository.utils;

import com.toySpring.repository.helper.EntityInfo;
import com.toySpring.repository.helper.ReturnValueHandler;
import com.toySpring.repository.helper.SelectSQLMethodInfo;
import com.toySpring.repository.utils.databaseDialects.DialectBuilderFactory;

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


    static String generateCreateTableSql(DataSource dataSource, EntityInfo entityInfo) {

        return DialectBuilderFactory.getDialectBuilder(dataSource).buildCreateTableSql(entityInfo);

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
            Class fieldType = entityInfo.getFiledName2Field().get(fieldName).getType();
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

    public static String buildSelectSQLTemplate(DataSource dataSource, EntityInfo entityInfo, List<String> selectFieldNames, List<String> queryFieldNames) {

        return DialectBuilderFactory.getDialectBuilder(dataSource).buildSelectSQLTemplate(entityInfo, selectFieldNames, queryFieldNames);

    }

    public static <T> void executeUpdateSQL(DataSource dataSource, EntityInfo entityInfo, T entity) {

        String sql = buildUpdateSQLWithArgs(dataSource, entityInfo, entity);

        executeSQL(sql, dataSource);

    }

    private static <T> String buildUpdateSQLWithArgs(DataSource dataSource, EntityInfo entityInfo, T entity) {

        return DialectBuilderFactory.getDialectBuilder(dataSource).buildUpdateSQL(entityInfo, entity);
    }


    public static <T> void executeInsertSQL(DataSource dataSource, EntityInfo entityInfo, T entity) {

        String sql = buildInsertSQLWithArgs(dataSource, entityInfo, entity);
        executeSQL(sql, dataSource);

    }

    private static <T> String buildInsertSQLWithArgs(DataSource dataSource, EntityInfo entityInfo, T entity) {

        return DialectBuilderFactory.getDialectBuilder(dataSource).buildInsertSQL(entityInfo, entity);
    }

}
