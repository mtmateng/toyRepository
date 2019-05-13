package com.toySpring.repository.utils.databaseDialects;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DialectBuilderFactory {

    private static Map<DataSource, DialectBuilder> dataSourceDialectBuilderMap = new HashMap<>();

    public static DialectBuilder getDialectBuilder(DataSource dataSource) {

        return dataSourceDialectBuilderMap.get(dataSource);
    }

    public static void registerDialect(DataSource dataSource, String url) {

        DialectBuilder dialectBuilder;
        url = url.replaceFirst("jdbc:", "").toLowerCase();
        if (url.startsWith("h2")) {
            dialectBuilder = new H2DialectBuilder();
        } else {
            throw new RuntimeException("尚不支持这种数据库");
        }

        dataSourceDialectBuilderMap.put(dataSource, dialectBuilder);
    }

}
