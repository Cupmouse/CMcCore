package net.cupmouse.minecraft.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.cupmouse.minecraft.CMcPlugin;
import net.cupmouse.minecraft.PluginModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseModule implements PluginModule {

    private CMcPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    public static void queueSQLAndRunSync(String sql, Runnable runnable) {

    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void enable() {
        // 設定ファイルからデータベースの設定を読む
        CommentedConfigurationNode configDatabaseNode = plugin.getCommonConfigNode().getNode("database");
        String url = configDatabaseNode.getNode("url").getString();
        String user = configDatabaseNode.getNode("user").getString();
        String password = configDatabaseNode.getNode("password").getString();
        CommentedConfigurationNode configOptionNode = plugin.getCommonConfigNode().getNode("option");
        int prepStmtCacheSize = configOptionNode.getNode("prep_stmt_cache_size").getInt();
        int prepStmtCacheSqlLimit = configOptionNode.getNode("prep_stmt_cache_sql_limit").getInt();
        int maxPoolSize = configOptionNode.getNode("max_pool_size").getInt();

        // HikariCPをセットアップ
        HikariConfig hc = new HikariConfig();
        hc.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
        // 設定を適用
        hc.setJdbcUrl(url);
        hc.setUsername(user);
        hc.setPassword(password);

        // Prepared statementをキャッシュする TODO キャッシュ?どういうことか。
        hc.addDataSourceProperty("cachePrepStmts", true);
        // コネクション毎にキャッシュされるPrepared Statement
        hc.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
        // キャッシュするPrepared statementの文字数上限
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
        // プールのサイズ
        hc.setMaximumPoolSize(maxPoolSize);
        // オートコミットしない
        hc.setAutoCommit(false);

        // データソースを作る
        this.dataSource = new HikariDataSource(hc);

        plugin.getLogger().info("データベースプールを初期化しました！");
    }

    @Override
    public void disable() {
        // データソースをクローズ
        dataSource.close();
    }

}
