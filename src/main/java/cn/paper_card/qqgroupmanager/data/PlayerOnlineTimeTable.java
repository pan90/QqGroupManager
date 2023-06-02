package cn.paper_card.qqgroupmanager.data;

import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.LinkedList;

// 记录玩家在线时长的数据表
public class PlayerOnlineTimeTable {

    public record Record(String uuid, long time) {
    }

    private final static String NAME = "player_online_time";

    private final PreparedStatement statementInsert;

    private final PreparedStatement statementQuery;

    private final PreparedStatement statementAdd;

    private final PreparedStatement statementQueryAll;

    public PlayerOnlineTimeTable(@NotNull Connection connection) throws SQLException {
        this.createTable(connection);

        try {
            this.statementInsert = connection.prepareStatement
                    ("insert into " + NAME + " (uuid,time) values (?,?)");

            this.statementQuery = connection.prepareStatement
                    ("select time from " + NAME + " where uuid=?");

            this.statementAdd = connection.prepareStatement
                    ("update " + NAME + " set time=time+? where uuid=?");

            this.statementQueryAll = connection.prepareStatement
                    ("select uuid,time from " + NAME);


        } catch (SQLException e) {
            try {
                this.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

    }

    public void close() throws SQLException {

        SQLException exception = null;

        if (this.statementInsert != null) {
            try {
                this.statementInsert.close();
            } catch (SQLException e) {
                exception = e;
            }
        }

        if (this.statementQuery != null) {
            try {
                this.statementQuery.close();
            } catch (SQLException e) {
                exception = e;
            }
        }

        if (this.statementAdd != null) {
            try {
                this.statementAdd.close();
            } catch (SQLException e) {
                exception = e;
            }
        }


        if (this.statementQueryAll != null) {
            try {
                this.statementQueryAll.close();
            } catch (SQLException e) {
                exception = e;
            }
        }

        if (exception != null) throw exception;

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

    public @NotNull LinkedList<Long> query(@NotNull String uuid) throws SQLException {
        this.statementQuery.setString(1, uuid);

        final ResultSet resultSet = this.statementQuery.executeQuery();

        final LinkedList<Long> times = new LinkedList<>();
        try {
            while (resultSet.next()) {
                final long time = resultSet.getLong(1);
                times.add(time);
            }
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return times;
    }

    public @NotNull LinkedList<Record> queryAll() throws SQLException {

        final ResultSet resultSet = this.statementQueryAll.executeQuery();

        final LinkedList<Record> times = new LinkedList<>();
        try {
            while (resultSet.next()) {
                final String uuid = resultSet.getString(1);
                final long time = resultSet.getLong(2);
                times.add(new Record(uuid, time));
            }
        } catch (SQLException e) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }

        resultSet.close();

        return times;
    }

    public int insert(@NotNull String uuid, long time) throws SQLException {
        this.statementInsert.setString(1, uuid);
        this.statementInsert.setLong(2, time);

        return this.statementInsert.executeUpdate();
    }

    public int add(@NotNull String uuid, long time) throws SQLException {
        this.statementAdd.setLong(1, time);
        this.statementAdd.setString(2, uuid);

        return this.statementAdd.executeUpdate();
    }
}
