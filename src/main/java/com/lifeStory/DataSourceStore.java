package com.lifeStory;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import javax.sql.DataSource;

public final class DataSourceStore {

    @Getter
    private static final DataSource datasource;

    static {
        HikariDataSource innerDataSource = new HikariDataSource();
        innerDataSource.setJdbcUrl("jdbc:h2:mem:test");
        innerDataSource.setDriverClassName("org.h2.Driver");
        innerDataSource.setUsername("testdb");
        innerDataSource.setPassword("testdb");
        datasource = innerDataSource;
    }


}
