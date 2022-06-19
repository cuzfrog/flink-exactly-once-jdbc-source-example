package com.github.cuzfrog.task.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Properties;

@Component
@ConfigurationProperties(prefix = "spring.datasource")
public final class DataSourceProps implements Serializable {
    private String url;
    private String username;
    private String password;
    private String driverClassName;

    private Properties hikari = new Properties();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public Properties getHikari() {
        return hikari;
    }

    public void setHikari(Properties hikari) {
        this.hikari = hikari;
    }
}
