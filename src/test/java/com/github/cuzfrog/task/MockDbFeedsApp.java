package com.github.cuzfrog.task;

import com.github.cuzfrog.task.jdbc.DataSourceProps;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
final class MockDbFeedsApp implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MockDbFeedsApp.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final DataSource dataSource;

    MockDbFeedsApp(DataSourceProps props) {
        HikariConfig config = new HikariConfig(props.getHikari());
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setDriverClassName(props.getDriverClassName());
        config.setReadOnly(false);
        this.dataSource = new HikariDataSource(config);
    }

    @SuppressWarnings("SqlResolve")
    @Override
    public void run(ApplicationArguments args) {
        log.info("Start feeding DB.");
        executorService.scheduleAtFixedRate(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            try(Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO MY_SOURCE_EVENT (TIMESTAMP) VALUES (?)"))   {
                statement.setLong(1, System.currentTimeMillis() - random.nextLong(-1000L, 1000L)); // make some skew
                statement.execute();
                connection.commit();
            } catch (Exception e) {
                log.error("Error inserting data,", e);
                throw new RuntimeException(e);
            }
        }, 3000, 100, TimeUnit.MILLISECONDS);
    }
}
