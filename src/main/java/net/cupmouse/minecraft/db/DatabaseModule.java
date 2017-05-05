package net.cupmouse.minecraft.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.cupmouse.minecraft.CMcPlugin;
import net.cupmouse.minecraft.PluginModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;

public class DatabaseModule implements PluginModule {

    private CMcPlugin plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    public DatabaseModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    public <T> Future<T> queueQueryTask(Callable<T> callable) {
        return executor.submit(callable);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void onInitializationProxy() {
        // 設定ファイルからデータベースの設定を読む
        CommentedConfigurationNode configDatabaseNode = plugin.getCommonConfigNode().getNode("database");
        String url = configDatabaseNode.getNode("url").getString();
        String user = configDatabaseNode.getNode("user").getString();
        String password = configDatabaseNode.getNode("password").getString();
        CommentedConfigurationNode configOptionNode = configDatabaseNode.getNode("option");
        int prepStmtCacheSize = configOptionNode.getNode("prep_stmt_cache_size").getInt();
        int prepStmtCacheSqlLimit = configOptionNode.getNode("prep_stmt_cache_sql_limit").getInt();
        int maxPoolSize = configOptionNode.getNode("max_pool_size").getInt();
        int threads = configOptionNode.getNode("threads").getInt();
        int connectionTimeout = configOptionNode.getNode("connection_timeout").getInt();
        int idleTimeout = configDatabaseNode.getNode("idle_timeout").getInt();
        int minimumIdle = configDatabaseNode.getNode("minimum_idle").getInt();

        // HikariCPをセットアップ
        HikariConfig hc = new HikariConfig();
        // これを設定するかもしくはJDBCURLを設定するらしいがよくわからない
//        hc.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
        // プールのサイズ
        hc.setMaximumPoolSize(maxPoolSize);
        // 接続時のタイムアウト
        hc.setConnectionTimeout(connectionTimeout);
        // アイドルのタイムアウト
        hc.setIdleTimeout(idleTimeout);
        // アイドル状態のコネクションの数
        hc.setMinimumIdle(minimumIdle);
        // 設定を適用
        hc.setJdbcUrl(url);
        hc.setUsername(user);
        hc.setPassword(password);

        // TODO 何故かエラー（対応していない？）
        // Prepared statementをキャッシュする TODO キャッシュ?どういうことか。
//        hc.addDataSourceProperty("cachePrepStmts", true);
        // コネクション毎にキャッシュされるPrepared Statement
//        hc.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
        // キャッシュするPrepared statementの文字数上限
//        hc.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
        // オートコミットしない
        hc.setAutoCommit(false);

        // データソースを作る
        this.dataSource = new HikariDataSource(hc);

        plugin.getLogger().info("データベースプールを初期化しました！");

        this.executor = Executors.newFixedThreadPool(threads);
        plugin.getLogger().info("クエリサービスを開始しました！");
    }

    @Override
    public void onStoppedServerProxy() {
        plugin.getLogger().info("クエリサービスを停止します。");
        this.executor.shutdown();

        // エラーが起きても5回試行
        int tries = 0;

        while (true) {
            boolean terminated = false;

            try {
                terminated = executor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tries++;

            if (terminated) {
                break;
            } else {
                // 5回目の試行ならエラー表示
                if (tries == 5) {
                    plugin.getLogger().info("クエリサービスを停止できなかったようです。");
                }
            }
        }

        // TODO Minecraft Tickは終了時に実行されるのか？されなければ、ゲーム内とデータベースで矛盾が発生する可能性がある

        plugin.getLogger().info("データソースをクローズします。");
        // データソースをクローズ
        dataSource.close();
    }

}
