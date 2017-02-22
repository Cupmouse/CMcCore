package net.cupmouse.minecraft.db;

import com.zaxxer.hikari.HikariConfig;
import net.cupmouse.minecraft.CMcPlugin;
import net.cupmouse.minecraft.PluginModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseBase implements PluginModule {

    private ExecutorService executor;
    private CMcPlugin plugin;

    public DatabaseBase(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    public static void queueSQLAndRunSync(String sql, Runnable runnable) {
        
    }

    @Override
    public void enable() {
        CommentedConfigurationNode configDatabaseNode = plugin.getCommonConfigNode().getNode("Database");
        int threadNumber = configDatabaseNode.getInt();
        String uri = configDatabaseNode.getNode("uri").getString();
        String user = configDatabaseNode.getNode("user").getString();
        String password = configDatabaseNode.getNode("password").getString();

        HikariConfig config = new HikariConfig();

        this.executor = Executors.newFixedThreadPool(threadNumber);

        int i = 0;

        while (i < threadNumber) {
            try {
                Connection connection = DriverManager.getConnection(uri, user, password);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void disable() {

        plugin.getLogger().info("クエリエグゼキューターを終了します。");

        // エグゼキューターを終了する
        this.executor.shutdown();

        try {
            int tryCount = 0;
            while (this.executor.awaitTermination(1000, TimeUnit.SECONDS)) {
                tryCount++;
                if (tryCount >= 10) {
                    plugin.getLogger().warn("クエリエグゼキューターがうまく終了しなかったようです！");
                    break;
                } else {
                    plugin.getLogger().info("クエリエグゼキューターを終了中..." + tryCount);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();

        }

        if (!this.executor.isTerminated()) {
            plugin.getLogger().debug("クエリエグゼキューターにキューが残っています！！！");

        }

        this.executor = null;
    }

    private class RequestingThread extends Thread {

        @Override
        public void run() {

        }
    }
}
