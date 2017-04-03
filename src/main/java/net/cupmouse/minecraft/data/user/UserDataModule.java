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

import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UserDataModule implements PluginModule {

    // オンラインのプレイヤーをキャッシュする
    private final Map<UUID, OnlineUser> onlineCached = new WeakHashMap<>();
    private final CMcPlugin plugin;

    public UserDataModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    public OnlineUser getOnlineUser(Player player) {
        OnlineUser onlineUser;

        synchronized (onlineCached) {
            onlineUser = onlineCached.get(player.getUniqueId());
        }

        if (onlineUser == null) {
            player.kick(Text.of(TextColors.RED,
                    "✗申し訳ございません。ユーザーデータを扱う際に問題が発生しました。" +
                            "管理人にお問い合わせください。"));
            throw new NullPointerException("ログインしているのにキャッシュされていない: " + player.getUniqueId());
        }

        return onlineUser;
    }

    @Listener(order = Order.PRE)
    public void onAuth(ClientConnectionEvent.Auth event) {
        // このメソッドは認証スレッドから実行されることに注意
        // プレイヤーが認証を通ろうとしたらキャッシュする

        // UUIDの取得をクエリスレッドで実行しないように！
        UUID uniqueId = event.getProfile().getUniqueId();
        // 時間はこのスレッドで取る
        LocalDateTime loginDatetime = LocalDateTime.now();

        Future<Long> future = plugin.getDbm().queueQueryTask(() -> {
            // クエリスレッドで実行される

            Connection connection = null;

            try {
                connection = plugin.getDbm().getConnection();

                // 第一段階と第二段階がある。
                // どちらかに失敗するとユーザー情報取り込み失敗としてコミットせず、ロールバックし、キャッシュしない。
                // （故に、後にログイン拒否される。）

                // 第一段階：ユーザーIDを取得する
                int userId;
                // アドレスを記録するので取っておく
                InetAddress address = event.getConnection().getAddress().getAddress();
                // 名前も記録するので取っておく
                Optional<String> optional = event.getProfile().getName();

                PreparedStatement prepStmt =
                        connection.prepareStatement(
                                "SELECT user_id, name, address AS address FROM users " +
                                        "WHERE uuid = ?");
                prepStmt.setBytes(1, Utilities.convertUUIDtoBytes(uniqueId));
                ResultSet resultSet = prepStmt.executeQuery();

                if (resultSet.next()) {
                    // ユーザーが存在したのでそのIDを返す
                    userId = resultSet.getInt(1);
                    String recordedName = resultSet.getString(2);
                    byte[] recordedAddressBytes = resultSet.getBytes(3);

                    prepStmt.close();

                    // TODO 認証時に常に名前が帰ってこない人は、名前が更新されないけど？

                    // 名前変更確認
                    if (optional.isPresent()) {
                        String playerName = optional.get();

                        // equalsを反対にするとぬるぽ
                        if (!playerName.equals(recordedName)) {
                            plugin.getLogger().debug("rec:"+ recordedName +" name:"+ playerName);

                            // 名前の更新をする
                            PreparedStatement prepStmt2 = connection.prepareStatement(
                                    "UPDATE users SET name = ? WHERE user_id = ?");
                            prepStmt2.setString(1, playerName);
                            prepStmt2.setInt(2, userId);
                            if (prepStmt2.executeUpdate() != 1) {
                                throw new IllegalStateException("名前の更新ができませんでした");
                            }
                            prepStmt2.close();
                        }
                    }

                    InetAddress recordedAddress = null;

                    // TODO データベースのADDRESSが不正な長さのバイナリだと、InetAddress.getByAddressでつまずく
                    // java.net.UnknownHostException: addr is of illegal length
                    // 接続元変更確認
                    if (recordedAddressBytes == null
                            || !address.equals(recordedAddress = InetAddress.getByAddress(recordedAddressBytes))) {
                        plugin.getLogger().debug("rec:"+ recordedAddress +" addr:"+ address);

                        PreparedStatement prepStmt2 = connection.prepareStatement(
                                "UPDATE users SET address = ? WHERE user_id = ?");
                        prepStmt2.setBytes(1, address.getAddress());
                        prepStmt2.setInt(2, userId);

                        if (prepStmt2.executeUpdate() != 1) {
                            throw new IllegalStateException("アドレスの更新ができませんでした");
                        }

                        prepStmt2.close();
                    }
                } else {
                    // ユーザーが存在しないので作成する
                    // まずは前のPrepstmtをクローズ
                    prepStmt.close();

                    PreparedStatement prepStmt2 = connection.prepareStatement(
                            "INSERT INTO users (uuid, name, address) VALUES (?, ?, ?)");
                    prepStmt2.setBytes(1, Utilities.convertUUIDtoBytes(uniqueId));
                    prepStmt2.setString(2, optional.orElse(null));
                    prepStmt2.setBytes(3, address.getAddress());

                    // 新しくテーブルにユーザーを追加するが、変更された行が1行でないと正常に完了していない。
                    if (prepStmt2.executeUpdate() == 1) {
                        // 追加したユーザーのユーザーIDを取得する。
                        // AUTO_INCREMENTなのでデータベースから取ってこないと分からない。
                        prepStmt2.close();
                        Statement statement = connection.createStatement();

                        ResultSet resultSet2 = statement.executeQuery("SELECT LAST_INSERT_ID() FROM users");

                        if (resultSet2.next()) {
                            userId = resultSet2.getInt(1);

                            statement.close();
                        } else {
                            throw new IllegalStateException("LAST_INSERT_ID()の結果が帰ってきません。");
                        }
                    } else {
                        throw new IllegalStateException("データ挿入で変更された行数が1ではありません。");
                    }
                }

                // 第二段階：セッションレコードを作る
                PreparedStatement prepStmt2 = connection.prepareStatement(
                        "INSERT INTO user_session (user_id, login_datetime) VALUES (?, ?)");

                prepStmt2.setInt(1, userId);
                prepStmt2.setString(2,
                        Utilities.LOCALDATETIME_MYSQL_FORMAT_NANO.format(loginDatetime));

                int sessionId;

                if (prepStmt2.executeUpdate() == 1) {
                    prepStmt2.close();

                    // レコードが作れたのでその番号を確認
                    Statement statement = connection.createStatement();
                    ResultSet resultSet2 = statement.executeQuery("SELECT LAST_INSERT_ID() FROM user_session");
                    if (resultSet2.next()) {
                        sessionId = resultSet2.getInt(1);
                    } else {
                        throw new IllegalStateException("LAST_INSERT_ID()の結果が帰ってきません");
                    }
                } else {
                    throw new IllegalStateException("コネクション履歴の追加に失敗");
                }

                // データベースが変更されているので、結果をコミットする
                connection.commit();

                return ((long) userId) << 32 | sessionId;
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
            Long aLong = future.get();
            int userId = (int) (aLong >>> 32);
            int sessionId = aLong.intValue();

            UserDataFetcher userDataFetcher = new UserDataFetcher(plugin, userId);
            OnlineUser onlineUser = new OnlineUser(sessionId, userDataFetcher);

            synchronized (onlineCached) {
                onlineCached.put(uniqueId, onlineUser);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Listener(order = Order.DEFAULT)
    public void onAuthAfterCaching(ClientConnectionEvent.Auth event) {
        // オンラインキャッシュにないなら問題が発生しているのでキック
        // このメソッドは認証スレッドから実行されることに注意

        boolean contain;
        UUID uniqueId = event.getProfile().getUniqueId();

        synchronized (onlineCached) {
            contain = onlineCached.containsKey(uniqueId);
        }

        if (!contain) {
            event.setMessage(Text.of(TextColors.RED,
                    "✗申し訳ございません。ユーザーデータを扱う際に問題が発生しました。" +
                            "管理人にお問い合わせください。"));
            event.setCancelled(true);
        }
    }

    // TODO 本当はLOGIN時のときのほうが親切でよい
    @Listener
    public void onJoin(ClientConnectionEvent.Join event) {
        OnlineUser onlineUser;

        synchronized (onlineCached) {
            onlineUser = onlineCached.get(event.getTargetEntity().getUniqueId());
        }

        // 初めてログイン実績を解除
        Future<Boolean> future = onlineUser.getDataFetcher().earnAchievementIfNot(UserAchievements.FIRST_LOGIN);

        // あとでチェック
        this.plugin.getGame().getScheduler().createTaskBuilder().intervalTicks(1).execute(task -> {
            plugin.getLogger().debug("CHK ACHIVEMENT");

            if (future.isDone()) {
                plugin.getLogger().debug("DONE");
                try {
                    Boolean aBoolean = future.get();

                    if (aBoolean) {
                        // 初めてログインした！

                        event.getTargetEntity().sendMessage(Text.of("CMC実績「はじめまして」を獲得！"));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                task.cancel();
            }
        }).submit(plugin);
    }


    @Listener(order = Order.POST)
    public void onDisconnect(ClientConnectionEvent.Disconnect event) {
        OnlineUser removed;
        UUID uniqueId = event.getTargetEntity().getUniqueId();

        synchronized (onlineCached) {
            removed = onlineCached.remove(uniqueId);
        }

        if (removed == null) {
            this.plugin.getLogger().info("ユーザーがキャッシュされていません。: " + uniqueId);
        } else {
            // キャッシュにあるなら、切断時刻の記録は試行する。

            LocalDateTime disconnectDatetime = LocalDateTime.now();

            // セッションを終了する。
            this.plugin.getDbm().queueQueryTask(() -> {
                Connection connection = null;
                try {
                    connection = this.plugin.getDbm().getConnection();

                    // 切断時刻の記録
                    PreparedStatement prepStmt = connection.prepareStatement(
                            "UPDATE user_session SET disconnect_datetime = ? WHERE session_id = ?");
                    prepStmt.setString(1,
                            Utilities.LOCALDATETIME_MYSQL_FORMAT_NANO.format(disconnectDatetime));
                    prepStmt.setInt(2, removed.getSessionId());

                    if (prepStmt.executeUpdate() == 1) {
                        // これ以降は失敗してもプレイ時間が少なく見積もられるだけなので、ここでコミットしてしまう。
                        connection.commit();

                        prepStmt.close();

                        // プレイした時間の記録
                        PreparedStatement prepStmt2 = connection.prepareStatement(
                                "UPDATE users SET playing_sec = " +
                                        "playing_sec + (SELECT disconnect_datetime - login_datetime " +
                                        "FROM user_session WHERE session_id = ?) WHERE user_id = ?");
                        prepStmt2.setInt(1, removed.getSessionId());
                        prepStmt2.setInt(2, removed.getDataFetcher().getUserId());

                        if (prepStmt2.executeUpdate() == 1) {
                            connection.commit();

                            return null;
                        } else {
                            throw new IllegalStateException("合計プレイ時間の更新ができませんでした");
                        }
                    } else {
                        throw new IllegalStateException("切断時刻を記録できませんでした");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();

                    if (connection != null) {
                        try {
                            connection.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }

                    // 必要ないけど一応
                    throw e;
                } finally {
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onInitializationProxy() {
        // このインスタンスをリスナとして登録する
        Sponge.getGame().getEventManager().registerListeners(plugin, this);
        this.plugin.getLogger().info("ユーザー機能を有効化しました！");
    }
}
