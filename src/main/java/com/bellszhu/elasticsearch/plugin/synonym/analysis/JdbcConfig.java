package com.bellszhu.elasticsearch.plugin.synonym.analysis;

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

    /**
     * 驱动名
     */
    private String driver;

    /**
     * 数据库url
     */
    private String url;

    /**
     * 数据库账号
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 查询近义词汇的sql，注意是以words字段展示
     */
    private String synonymWordSql;

    /**
     * 获取近义词最近更新时间的sql
     */
    private String synonymLastModitimeSql;

    /**
     * 间隔，暂时无用
     */
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
