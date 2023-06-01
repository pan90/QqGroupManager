package cn.paper_card.qqgroupmanager.data;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// 记录玩家在线时长的数据表
public class PlayerOnlineTimeTable {

    private final static String NAME = "player_online_time";

    public PlayerOnlineTimeTable(@NotNull Connection connection) {

    }

    private void createTable(@NotNull Connection connection) throws SQLException {
        final Statement statement = connection.createStatement();

        try {
            statement.executeUpdate("create table if not exists " + NAME +
                    "(uuid char(40) primary key, time integer)");
        } catch (SQLException e) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        statement.close();
    }
}
