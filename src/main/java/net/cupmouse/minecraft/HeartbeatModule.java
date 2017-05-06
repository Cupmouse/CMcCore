package net.cupmouse.minecraft;

import net.cupmouse.minecraft.db.DatabaseModule;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HeartbeatModule implements PluginModule {

    private Task heartbeatTask;
    private Task heartbeatIdTask;
    private Integer sessionId;

    @Override
    public void onStoppedServerProxy() {

        heartbeatTask.cancel();

        // この関数を抜けると、ハートビートが更新される前にJVMが終了されてしまうので、この処理が終わるまで待機する。
        // 仮に、致命的なエラーが発生してこの関数で止まってしまっても、ゲームのデータは安全に保存されている。
        // TODO ただしプラグインは違うよ
        Future<Object> future = CMcCore.getDbm().queueQueryTask(() -> {
            Connection connection = null;
            try {
                connection = CMcCore.getDbm().getConnection();
                PreparedStatement prepStmt = connection.prepareStatement(
                        "UPDATE server_session SET last_datetime=?, status='stopped' WHERE session_id=?");
                prepStmt.setString(1,
                        Utilities.LOCALDATETIME_MYSQL_FORMAT_NONANO.format(LocalDateTime.now()));
                prepStmt.setInt(2, sessionId);

                if (prepStmt.executeUpdate() == 1) {
                    connection.commit();

                    return null;
                } else {
                    throw new IllegalStateException("ハートビートの最終更新に失敗");
                }
            } catch (SQLException e) {
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }

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

        try {
            // 終了まで待つ
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAboutToStartServerProxy() {

        DatabaseModule dbm = CMcCore.getDbm();

        // Heartbeat準備(Heartbeatレコードを追加し、そのHeartbeatIdを取得する)
        Future<Integer> future = dbm.queueQueryTask(() -> {

            Connection connection = null;

            try {
                connection = dbm.getConnection();

                // はじめの時間だけ指定して、レコードを追加
                PreparedStatement prepStmt = connection.prepareStatement(
                        "INSERT INTO server_session (start_datetime) VALUES (?)");
                prepStmt.setString(1,
                        Utilities.LOCALDATETIME_MYSQL_FORMAT_NONANO.format(LocalDateTime.now()));

                // 帰ってきた行数が1でないとうまく更新できていない
                if (prepStmt.executeUpdate() == 1) {
                    prepStmt.close();

                    Statement statement = connection.createStatement();

                    ResultSet resultSet = statement.executeQuery("SELECT LAST_INSERT_ID() FROM server_session");

                    if (resultSet.next()) {
                        int sessionId = resultSet.getInt(1);

                        connection.commit();

                        return sessionId;
                    } else {
                        throw new IllegalStateException("LAST_INSERT_ID()の結果が帰ってきません。");
                    }
                } else {
                    throw new IllegalStateException("新しいハートビートレコードを追加できませんでした！");
                }
            } catch (Exception e) {
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
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

        Game game = Sponge.getGame();

        // これから作られるタスクは、ID取得タスクが、IDを取得したことを確認したあとにスケジュールされ、実行される。
        Task.Builder heartbeatTb = game.getScheduler().createTaskBuilder();
        heartbeatTb.name("ハートビートタスク");
        heartbeatTb.intervalTicks(100);
        heartbeatTb.execute(() -> {
            dbm.queueQueryTask(() -> {
                Connection connection = null;
                try {
                    connection = dbm.getConnection();

                    // 状態＝稼働中にセットし、最終更新日時を更新する。
                    PreparedStatement prepStmt = connection.prepareStatement(
                            "UPDATE server_session SET last_datetime=?, status='running' WHERE session_id=?");
                    prepStmt.setString(1,
                            Utilities.LOCALDATETIME_MYSQL_FORMAT_NONANO.format(LocalDateTime.now()));
                    prepStmt.setInt(2, sessionId);

                    if (prepStmt.executeUpdate() == 1) {
                        connection.commit();

//                        logger.debug("HEARTBEAT BEEEEP!");
                        // 処理が終わっただけ。返すものは何もなし
                        return null;
                    } else {
                        throw new IllegalStateException("ハートビートに失敗しました。");
                    }
                } catch (SQLException | IllegalStateException e) {
                    e.printStackTrace();

                    if (connection != null) {
                        try {
                            connection.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }

                    return null;
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
        });

        // IDが取得されたか確認するタスク。（IDの取得はクエリスレッドで行うから。）
        // 上のタスクよりこちらのほうが先に実行される。
        Task.Builder sessionIdGetter = game.getScheduler().createTaskBuilder();
        sessionIdGetter.name("ハートビートIDの取得タスク");
        sessionIdGetter.intervalTicks(1);
        sessionIdGetter.execute(() -> {
            if (future.isDone() && game.getState() == GameState.SERVER_STARTED) {
                try {
                    this.sessionId = future.get();
                    CMcCore.getLogger().debug("sessionID: " + sessionId);

                    heartbeatTask = heartbeatTb.submit(CMcCore.getPlugin());

                    // このタスクは再生終了後に自動削除されます ビー！
                    heartbeatIdTask.cancel();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                CMcCore.getLogger().debug("sessionID取得待機");
            }
        });

        // ハートビートID取得タスクを実行！
        heartbeatIdTask = sessionIdGetter.submit(CMcCore.getPlugin());
    }
}
