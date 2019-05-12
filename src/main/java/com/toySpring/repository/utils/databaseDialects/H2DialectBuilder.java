package com.toySpring.repository.utils.databaseDialects;

import com.toySpring.repository.helper.EntityInfo;

import java.beans.PropertyDescriptor;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class H2DialectBuilder implements DialectBuilder {

    H2DialectBuilder() {
        //使其只能被Factory创建
    }

    private static final String CREATE_TABLE_TEMPLATE = "CREATE TABLE IF NOT EXISTS `%s` (%s %s) %s %s;";
    /**
     * 这个值记录了在SQL语句中不需要加引号的那些值的类型。比如int，double之类的。
     */
    private static final Set<Class> NO_QUOTE_CLASS = new HashSet<Class>() {
        {
            add(Integer.class);
            add(int.class);
            add(double.class);
            add(Double.class);
        }
    };

    @Override
    public String buildCreateTableSql(EntityInfo entityInfo) {
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

    @Override
    public String buildSelectSQLTemplate(EntityInfo entityInfo, List<String> selectFieldNames, List<String> queryFieldNames) {

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

    @Override
    public <T> String buildUpdateSQL(EntityInfo entityInfo, T entity) {

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

    @Override
    public <T> String buildInsertSQL(EntityInfo entityInfo, T entity) {

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

}
