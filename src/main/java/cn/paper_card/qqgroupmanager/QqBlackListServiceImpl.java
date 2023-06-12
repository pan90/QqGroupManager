package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IQqBlackListService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

class QqBlackListServiceImpl implements IQqBlackListService {

    private DatabaseConnection connection = null;
    private Table table = null;

    private final @NotNull QqGroupManager plugin;

    QqBlackListServiceImpl(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
    }

    private @NotNull DatabaseConnection getConnection() throws Exception {
        if (this.connection == null) {
            this.connection = new DatabaseConnection(this.plugin);
        }
        return this.connection;
    }

    private @NotNull Table getTable() throws Exception {
        if (this.table == null) {
            this.table = new Table(this.getConnection().getConnection());
        }
        return this.table;
    }

    @Override
    public void add(@NotNull Info info) throws Exception {
        synchronized (this) {
            final Table t = this.getTable();
            final int i = t.insert(info);
            if (i != 1) throw new Exception("数据库影响行数：%d（应该是1）！".formatted(i));
        }
    }

    @Override
    public void remove(long qq) throws Exception {
        synchronized (this) {
            final Table t = this.getTable();
            final int i = t.delete(qq);
            if (i > 1) throw new Exception("根据一个QQ[%d]删除了%d条数据！".formatted(qq, i));
        }
    }

    @Override
    public @Nullable Info queryByQq(long qq) throws Exception {
        synchronized (this) {
            final Table t = this.getTable();

            final LinkedList<Info> list = t.query(qq);
            final int size = list.size();
            if (size == 0) return null;
            if (size == 1) return list.get(0);
            throw new Exception("根据一个QQ[%d]查到了%d条数据".formatted(qq, size));
        }
    }

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

        if (this.table != null) {
            try {
                this.table.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭数据库连接时异常：" + e);
                e.printStackTrace();
            }
            this.table = null;
        }

        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭数据库连接时异常：" + e);
                e.printStackTrace();
            }
            this.connection = null;
        }

    }

    private static class Table {

        static final String NAME = "qq_black_list";

        private final PreparedStatement statementInsert;
        private final PreparedStatement statementDelete;

        private final PreparedStatement statementQuery;

        Table(@NotNull Connection connection) throws SQLException {
            this.createTable(connection);

            try {
                this.statementInsert = connection.prepareStatement
                        ("insert into " + NAME + " (qq,nick,reason,time) values (?,?,?,?)");

                this.statementDelete = connection.prepareStatement
                        ("delete from " + NAME + " where qq=?");

                this.statementQuery = connection.prepareStatement
                        ("select qq,nick,reason,time from " + NAME + " where qq=?");


            } catch (SQLException e) {
                try {
                    this.close();
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }

        private void close() throws SQLException {
            SQLException exception = null;

            if (this.statementInsert != null) {
                try {
                    this.statementInsert.close();
                } catch (SQLException e) {
                    exception = e;
                }
            }

            if (this.statementDelete != null) {
                try {
                    this.statementDelete.close();
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

            if (exception != null) throw exception;
        }


        private void createTable(@NotNull Connection connection) throws SQLException {

            final String sql = "create table if not exists " + NAME +
                    "(qq integer not null primary key, nick varchar(64), reason varchar(256), time integer)";

            DatabaseConnection.createTable(connection, sql);
        }

        int insert(@NotNull Info info) throws SQLException {
            final PreparedStatement ps = this.statementInsert;

            ps.setLong(1, info.qq());
            ps.setString(2, info.nick());
            ps.setString(3, info.reason());
            ps.setLong(4, info.time());

            return ps.executeUpdate();
        }

        int delete(long qq) throws SQLException {
            final PreparedStatement ps = this.statementDelete;
            ps.setLong(1, qq);
            return ps.executeUpdate();
        }

        LinkedList<Info> query(long qq) throws SQLException {
            final PreparedStatement ps = this.statementQuery;

            ps.setLong(1, qq);

            final ResultSet resultSet = ps.executeQuery();

            final LinkedList<Info> list = new LinkedList<>();

            try {
                while (resultSet.next()) {
                    final long qq1 = resultSet.getLong("qq");
                    final String nick = resultSet.getString("nick");
                    final String reason = resultSet.getString("reason");
                    final long time = resultSet.getLong("time");

                    list.add(new Info(
                            qq1, nick, reason, time
                    ));
                }
            } catch (SQLException e) {
                try {
                    resultSet.close();
                } catch (SQLException ignored) {
                }

                throw e;
            }


            resultSet.close();

            return list;
        }
    }
}
