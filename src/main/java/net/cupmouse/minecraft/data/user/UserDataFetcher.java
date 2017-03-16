package net.cupmouse.minecraft.data.user;

import net.cupmouse.minecraft.CMcPlugin;
import net.cupmouse.minecraft.Utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.Future;

public class UserDataFetcher {

    private CMcPlugin plugin;
    private final int userId;

    public UserDataFetcher(CMcPlugin plugin, int userId) {
        this.plugin = plugin;
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    /**
     * パラメータに指定された実績を獲得していなければ与えます。
     * @param achievement 与える実績
     * @return 以前に獲得していない実績の場合は{@code true}、そうでない場合は{@code false}を返す{@link Future}
     */
    public Future<Boolean> earnAchievementIfNot(UserAchievements achievement) {
        LocalDateTime datetime = LocalDateTime.now();

        Future<Boolean> future = this.plugin.getDbm().queueQueryTask(() -> {
            Connection connection = null;

            try {
                connection = this.plugin.getDbm().getConnection();

                PreparedStatement prepStmt = connection.prepareStatement(
                        "SELECT COUNT(*) FROM user_achievement WHERE user_id = ?");
                prepStmt.setInt(1, userId);
                ResultSet resultSet = prepStmt.executeQuery();

                if (resultSet.next()) {
                    int count = resultSet.getInt(1);

                    if (count == 1) {
                        // 実績を持っているので終了
                        return false;
                    } else {
                        // 実績を持っていないと考える
                        // 実績を獲得させる

                        prepStmt.close();

                        PreparedStatement prepStmt2 = connection.prepareStatement(
                                "INSERT INTO user_achievement (user_id, achievement_type, datetime) VALUES " +
                                        "(?, (SELECT achievement_type FROM achievements WHERE name = ?), ?)");
                        prepStmt2.setInt(1, userId);
                        prepStmt2.setString(2, achievement.name());
                        prepStmt2.setString(3,
                                Utilities.LOCALDATETIME_MYSQL_FORMAT_NANO.format(datetime));

                        if (prepStmt2.executeUpdate() == 1) {
                            connection.commit();
                            return true;
                        } else {
                            throw new IllegalStateException("レコードを挿入できませんでした");
                        }
                    }
                } else {
                    throw new IllegalStateException("結果が帰ってきませんでした");
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

                // Futureが使われるかも知れないので
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

        return future;
    }
}
