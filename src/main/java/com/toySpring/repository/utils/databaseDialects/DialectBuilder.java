package com.toySpring.repository.utils.databaseDialects;

import com.toySpring.repository.helper.EntityInfo;

import java.util.List;

public interface DialectBuilder {

    String buildCreateTableSql(EntityInfo entityInfo);

    String buildSelectSQLTemplate(EntityInfo entityInfo, List<String> selectFieldNames, List<String> queryFieldNames);

    <T> String buildUpdateSQL(EntityInfo entityInfo, T entity);

    <T> String buildInsertSQL(EntityInfo entityInfo, T entity);

}
