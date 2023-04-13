package com.bellszhu.elasticsearch.plugin.synonym.analysis;

import com.bellszhu.elasticsearch.plugin.DynamicSynonymPlugin;
import com.bellszhu.elasticsearch.plugin.synonym.JdbcConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.elasticsearch.core.PathUtils;
import org.elasticsearch.env.Environment;

import java.io.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author liulingjie
 * @date 2023/4/12 19:43
 */
public class DynamicSynonymFromDbFile implements SynonymFile {

    /**
     * 配置文件名
      */
    private final static String DB_PROPERTIES = "jdbc.properties";

    private static Logger logger = LogManager.getLogger("dynamic-synonym");

    private String format;

    private boolean expand;

    private boolean lenient;

    private Analyzer analyzer;

    private Environment env;

    /**
     * 动态配置类型
     */
    private String location;

    /**
     * 作用类型
     */
    private String group;

    private long lastModified;

    private Path conf_dir;

    private JdbcConfig jdbcConfig;

    DynamicSynonymFromDbFile(Environment env, Analyzer analyzer,
                        boolean expand,boolean lenient, String format, String location, String group) {
        this.analyzer = analyzer;
        this.expand = expand;
        this.lenient = lenient;
        this.format = format;
        this.env = env;
        this.location = location;
        this.group = group;
        // 读取配置文件
        setJdbcConfig();
        // 加载驱动
        try {
            Class.forName(jdbcConfig.getDriver());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // 判断是否需要加载
        isNeedReloadSynonymMap();
    }

    /**
     * 读取配置文件
     */
    private void setJdbcConfig() {
        // 读取当前 jar 包存放的路径
        Path filePath = PathUtils.get(new File(DynamicSynonymPlugin.class.getProtectionDomain().getCodeSource()
                .getLocation().getPath())
                .getParent(), "config")
                .toAbsolutePath();
        this.conf_dir = filePath.resolve(DB_PROPERTIES);
        File file = conf_dir.toFile();
        Properties properties = null;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(file));
        } catch (Exception e) {
            logger.error("load jdbc.properties failed");
            logger.error(e.getMessage());
        }
        jdbcConfig = new JdbcConfig(
                properties.getProperty("jdbc.driver"),
                properties.getProperty("jdbc.url"),
                properties.getProperty("jdbc.username"),
                properties.getProperty("jdbc.password"),
                properties.getProperty("synonym.word.sql"),
                properties.getProperty("synonym.lastModitime.sql"),
                Integer.valueOf(properties.getProperty("interval"))
        );
    }

    /**
     * 加载同义词词典至SynonymMap中
     * @return SynonymMap
     */
    @Override
    public SynonymMap reloadSynonymMap() {
        try {
            logger.info("start reload local synonym from {}.", location);
            Reader rulesReader = getReader();
            SynonymMap.Builder parser = RemoteSynonymFile.getSynonymParser(rulesReader, format, expand, lenient, analyzer);
            return parser.build();
        } catch (Exception e) {
            logger.error("reload local synonym {} error!", e, location);
            throw new IllegalArgumentException(
                    "could not reload local synonyms file to build synonyms", e);
        }
    }

    /**
     * 判断是否需要进行重新加载
     * @return true or false
     */
    @Override
    public boolean isNeedReloadSynonymMap() {
        try {
            Long lastModify = getLastModify();
            if (lastModified < lastModify) {
                lastModified = lastModify;
                return true;
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return false;
    }

    /**
     * 获取同义词库最后一次修改的时间
     * 用于判断同义词是否需要进行重新加载
     *
     * @return getLastModify
     */
    public Long getLastModify() {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        Long last_modify_long = null;
        try {
            connection = DriverManager.getConnection(
                    jdbcConfig.getUrl(),
                    jdbcConfig.getUsername(),
                    jdbcConfig.getPassword()
            );
            statement = connection.createStatement();
            resultSet = statement.executeQuery(jdbcConfig.getSynonymLastModitimeSql());
            while (resultSet.next()) {
                Timestamp last_modify_dt = resultSet.getTimestamp("maxModitime");
                last_modify_long = last_modify_dt.getTime();
            }
        } catch (SQLException e) {
            logger.error("获取同义词库最后一次修改的时间",e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return last_modify_long;
    }

    /**
     * 查询数据库中的同义词
     * @return DBData
     */
    public ArrayList<String> getDBData() {
        ArrayList<String> arrayList = new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection(
                    jdbcConfig.getUrl(),
                    jdbcConfig.getUsername(),
                    jdbcConfig.getPassword()
            );
            statement = connection.createStatement();
            String sql = jdbcConfig.getSynonymWordSql();
            if (group != null && !"".equals(group.trim())) {
                sql = String.format("%s AND group = '%s'", group);
            }
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String theWord = resultSet.getString("words");
                arrayList.add(theWord);
            }
        } catch (SQLException e) {
            logger.error("查询数据库中的同义词异常",e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    /**
     * 同义词库的加载
     * @return Reader
     */
    @Override
    public Reader getReader() {
        StringBuffer sb = new StringBuffer();
        try {
            ArrayList<String> dbData = getDBData();
            for (int i = 0; i < dbData.size(); i++) {
                sb.append(dbData.get(i))
                        .append(System.getProperty("line.separator"));
            }
            logger.info("load the synonym from db");
        } catch (Exception e) {
            logger.error("reload synonym from db failed");
        }
        return new StringReader(sb.toString());
    }
}

