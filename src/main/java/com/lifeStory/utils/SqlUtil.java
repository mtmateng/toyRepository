package com.lifeStory.utils;

import com.lifeStory.helper.EntityInfo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUtil {

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

    static void executeSql(String sql, DataSource dataSource) {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("执行%s失败", sql), e);
        }
    }
}
