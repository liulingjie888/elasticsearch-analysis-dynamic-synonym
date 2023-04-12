package com.bellszhu.elasticsearch.plugin.synonym;

/**
 * @author liulingjie
 * @date 2022/11/30 16:03
 */
public class JdbcConfig {

    public JdbcConfig() {
    }

    public JdbcConfig(String driver, String url, String username, String password, String synonymWordSql, String synonymLastModitimeSql, Integer interval) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.synonymWordSql = synonymWordSql;
        this.synonymLastModitimeSql = synonymLastModitimeSql;
        this.interval = interval;
        this.driver = driver;
    }

    private String driver;

    private String url;

    private String username;

    private String password;

    private String synonymWordSql;

    private String synonymLastModitimeSql;

    private Integer interval;

    public String getDriver() { return driver; }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSynonymWordSql() {
        return synonymWordSql;
    }

    public String getSynonymLastModitimeSql() {
        return synonymLastModitimeSql;
    }

    public Integer getInterval() {
        return interval;
    }
}
