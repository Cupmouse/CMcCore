package net.cupmouse.minecraft.data.user;

import net.cupmouse.minecraft.CMcPlugin;
import net.cupmouse.minecraft.PluginModule;
import net.cupmouse.minecraft.Utilities;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UserDataModule implements PluginModule {

    // オンラインのプレイヤーのフェッチャーをキャッシュする
    private final Map<UUID, UserDataFetcher> onlineCached = new WeakHashMap<>();
    private final CMcPlugin plugin;

    public UserDataModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    @Listener(order = Order.PRE)
    public void onPlayerAuth(ClientConnectionEvent.Auth event) {
        // このメソッドは認証スレッドから実行されることに注意
        // プレイヤーが認証を通ろうとしたらキャッシュする

        // UUIDの取得をクエリスレッドで実行しないように！
        UUID uniqueId = event.getProfile().getUniqueId();

        Future<Integer> future = plugin.getDbm().queueQueryTask(() -> {
            // クエリスレッドで実行される

            Connection connection = null;
            ResultSet resultSet;

            try {
                connection = plugin.getDbm().getConnection();
                PreparedStatement prepStmt =
                        connection.prepareStatement("SELECT user_id FROM users WHERE uuid = ?");
                prepStmt.setBytes(1, Utilities.convertUUIDtoBytes(uniqueId));
                resultSet = prepStmt.executeQuery();

                if (resultSet.next()) {
                    // ユーザーが存在したのでそのIDを返す
                    return resultSet.getInt(1);
                } else {
                    // ユーザーが存在しないので作成する
                    // まずは前のPrepstmtをクローズ
                    prepStmt.close();

                    prepStmt = connection.prepareStatement("INSERT INTO users (uuid) VALUES (?)");
                    prepStmt.setBytes(1, Utilities.convertUUIDtoBytes(uniqueId));

                    // 新しくテーブルにユーザーを追加するが、変更された行が1行でないと正常に完了していない。
                    if (prepStmt.executeUpdate() == 1) {
                        // 追加したユーザーのユーザーIDを取得する。
                        // AUTO_INCREMENTなのでデータベースから取ってこないと分からない。
                        resultSet.close();
                        Statement statement = connection.createStatement();

                        resultSet = statement.executeQuery("SELECT LAST_INSERT_ID() FROM users");
                        if (resultSet.next()) {
                            int userId = resultSet.getInt(1);

                            // データベースが変更されているので、結果をコミットする
                            connection.commit();

                            return userId;
                        } else {
                            throw new IllegalStateException("LAST_INSERT_ID()の結果が帰ってきません。");
                        }
                    } else {
                        throw new IllegalStateException("データ挿入で変更された行数が1ではありません。");
                    }
                }
            } catch (Exception e) {
                // ぽけもんげっと

                if (connection != null) {
                    // ロールバックする

                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        e.printStackTrace();
                    }
                }

                // もとのスレッドで確認できるように例外を返す。
                throw e;
            } finally {
                if (connection != null) {
                    // 閉じる。すべてのPreparedStatementやそのResultSetは同時にクローズされる。
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // 結果より、UserDataFetcherを作ってキャッシュする。

        try {
            UserDataFetcher userDataFetcher = new UserDataFetcher(future.get());

            synchronized (onlineCached) {
                onlineCached.put(uniqueId, userDataFetcher);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
    @Listener(order = Order.DEFAULT)
    public void onPlayerAuthAfterCaching(ClientConnectionEvent.Auth event) {
        // このメソッドは認証スレッドから実行されることに注意
        // オンラインキャッシュにないなら問題が発生しているのでキック

        boolean contain;
        UUID uniqueId = event.getProfile().getUniqueId();

        synchronized (onlineCached) {
            contain = onlineCached.containsKey(uniqueId);
        }

        if (!contain) {
            event.setMessage(Text.of(TextColors.RED, "✗ユーザー情報読み取り時に問題が発生しました。"));
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.POST)
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        UserDataFetcher removed;
        UUID uniqueId = event.getTargetEntity().getUniqueId();

        synchronized (onlineCached) {
            removed = onlineCached.remove(uniqueId);
        }

        if (removed == null) {
            this.plugin.getLogger().info("ユーザーがキャッシュされていません。: " + uniqueId);
        }
    }

    public UserDataFetcher getFetcherOnline(Player player) {
        UserDataFetcher userDataFetcher;

        synchronized (onlineCached) {
            userDataFetcher = onlineCached.get(player.getUniqueId());
        }

        if (userDataFetcher == null) {
            player.kick(Text.of(TextColors.RED,
                    "✗申し訳ございません。ユーザーデータを扱う際に問題が発生しました。管理人にお問い合わせください。"));
            throw new NullPointerException("ログインしているのにキャッシュされていない: " + player.getUniqueId());
        }

        return userDataFetcher;
    }

    @Override
    public void onInitializationProxy() {
        // このインスタンスをリスナとして登録する
        Sponge.getGame().getEventManager().registerListeners(plugin, this);
        this.plugin.getLogger().info("ユーザー機能を有効化しました！");
    }
}
