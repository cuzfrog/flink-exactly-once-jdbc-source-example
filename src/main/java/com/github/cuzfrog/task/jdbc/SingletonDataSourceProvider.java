package com.github.cuzfrog.task.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.hibernate.HikariConfigurationUtil;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.Serializable;

@Service("dataSourceProvider")
final class SingletonDataSourceProvider implements java.util.function.Supplier<DataSource>, Serializable {
    private final DataSourceProps props;
    private transient DataSource dataSource;

    SingletonDataSourceProvider(DataSourceProps props) {
        this.props = props;
    }

    @Override
    public synchronized DataSource get() {
        if (this.dataSource == null) {
            HikariConfig hikariConfig = new HikariConfig(props.getHikari());
            hikariConfig.setJdbcUrl(props.getUrl());
            hikariConfig.setUsername(props.getUsername());
            hikariConfig.setPassword(props.getPassword());
            hikariConfig.setDriverClassName(props.getDriverClassName());
            this.dataSource = new HikariDataSource(hikariConfig);
        }
        return dataSource;
    }
}
