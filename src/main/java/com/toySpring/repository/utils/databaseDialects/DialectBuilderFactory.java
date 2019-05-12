package com.toySpring.repository.utils.databaseDialects;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DialectBuilderFactory {

    private static Map<DataSource, DialectBuilder> dataSourceDialectBuilderMap = new HashMap<>();

    public static DialectBuilder getDialectBuilder(DataSource dataSource) {

        if (dataSourceDialectBuilderMap.get(dataSource) == null) {
            dataSourceDialectBuilderMap.put(dataSource, getNewDialectBuilder(dataSource));
        }

        return dataSourceDialectBuilderMap.get(dataSource);
    }

    private static DialectBuilder getNewDialectBuilder(DataSource dataSource) {

        //todo
        return new H2DialectBuilder();

    }

}
