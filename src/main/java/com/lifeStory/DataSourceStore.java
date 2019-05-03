package com.lifeStory;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class DataSourceStore {

    private static final Map<String, DataSource> datasourceMap = new HashMap<>();

    static {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:test");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("testdb");
        dataSource.setPassword("testdb");
        datasourceMap.put("student", dataSource);
    }

    static {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:test2");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("testdb");
        dataSource.setPassword("testdb");
        datasourceMap.put("case", dataSource);
    }

    public static DataSource getDataSource(String name) {

        return Optional.ofNullable(datasourceMap.get(name)).orElseThrow(() -> new RuntimeException("名为：" + name + "的dataSource不存在"));

    }


}
