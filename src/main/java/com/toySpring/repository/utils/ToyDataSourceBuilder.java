package com.toySpring.repository.utils;

import com.toySpring.repository.helper.DataSourceConfigHolder;
import com.toySpring.repository.utils.databaseDialects.DialectBuilderFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.util.Map;

public class ToyDataSourceBuilder {

    // Spring对于配偶文件是先解析为一个全局文件，之后使用之，我们这里就粗暴地每次解析文件好了
    public static DataSource buildDataSource(String prefix) {

        DataSourceConfigHolder dataSourceConfigHolder = getDataSourceConfigHolder(prefix);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dataSourceConfigHolder.getUrl());
        dataSource.setDriverClassName(dataSourceConfigHolder.getDriver());
        dataSource.setUsername(dataSourceConfigHolder.getUsername());
        dataSource.setPassword(dataSourceConfigHolder.getPassword());

        DialectBuilderFactory.registerDialect(dataSource, dataSourceConfigHolder.getUrl());

        return dataSource;

    }

    private static DataSourceConfigHolder getDataSourceConfigHolder(String prefix) {

        DataSourceConfigHolder dataSourceConfigHolder = new DataSourceConfigHolder();
        String[] path = prefix.split("\\.");
        Map<String, Map<String, Map<String, String>>> configHolder = new Yaml().load(
            ToyDataSourceBuilder.class.getClassLoader().getResourceAsStream("application.yaml"));
        try {
            dataSourceConfigHolder.setUrl(configHolder.get(path[0]).get(path[1]).get("url"));
            dataSourceConfigHolder.setDriver(configHolder.get(path[0]).get(path[1]).get("driver"));
            dataSourceConfigHolder.setUsername(configHolder.get(path[0]).get(path[1]).get("username"));
            dataSourceConfigHolder.setPassword(configHolder.get(path[0]).get(path[1]).get("password"));
            return dataSourceConfigHolder;
        } catch (Exception e) {
            throw new RuntimeException(String.format("解析前缀为%s的配置文件失败", prefix), e);
        }

    }
}
