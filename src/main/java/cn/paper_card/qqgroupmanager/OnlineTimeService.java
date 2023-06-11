package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

class OnlineTimeService implements IOnlineTimeService {

    private final @NotNull QqGroupManager plugin;

    private DatabaseConnection dataBase = null;

    private final @NotNull Storage storage;

    private final @NotNull HashMap<UUID, Long> beginTimes = new HashMap<>();
    private final @NotNull Object beginTimesLock = new Object();

    public OnlineTimeService(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
        this.storage = new StorageImpl();
    }

    private @NotNull DatabaseConnection getDataBase() throws Exception {
        if (this.dataBase == null) {
            this.dataBase = new DatabaseConnection(this.plugin);
        }
        return this.dataBase;
    }

    private void closeDatabase() {
        if (this.dataBase != null) {
            try {
                this.dataBase.close();
            } catch (SQLException e) {
                this.plugin.getLogger().severe("关闭数据库连接时异常：" + e);
                e.printStackTrace();
            }
            this.dataBase = null;
        }
    }

    @Override
    public void init() {
        this.plugin.getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void on(@NotNull PlayerJoinEvent event) {
                // 记录开始上线时间
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                        () -> {
                            synchronized (beginTimesLock) {
                                beginTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                            }
                        });

            }

            @EventHandler
            public void on2(@NotNull PlayerQuitEvent event) {
                // 将在线时间写入数据库
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

                    final UUID id = event.getPlayer().getUniqueId();
                    final String name = event.getPlayer().getName();
                    final Long joinTime;
                    synchronized (beginTimesLock) {
                        joinTime = beginTimes.remove(id);
                    }

                    if (joinTime == null) return;

                    final long onlineTime = System.currentTimeMillis() - joinTime;
                    try {
                        plugin.getOnlineTimeService().getStorage().addTime(id, onlineTime);
                        plugin.getLogger().info("在线时间增加 {name: %s, time: %d}".formatted(name, onlineTime));
                    } catch (Exception e) {
                        plugin.getLogger().severe(e.toString());
                        e.printStackTrace();
                    }
                });
            }
        }, this.plugin);
    }

    @Override
    public void destroy() {

        final long curTime = System.currentTimeMillis();
        synchronized (beginTimesLock) {
            for (final UUID uuid : beginTimes.keySet()) {
                final Long beginTime = beginTimes.get(uuid);
                if (beginTime == null) continue;

                final long t = curTime - beginTime;

                try {
                    this.storage.addTime(uuid, t);
                } catch (Exception e) {
                    this.plugin.getLogger().severe("异常：" + e);
                    e.printStackTrace();
                }
            }
            beginTimes.clear();
        }

        this.closeDatabase();
    }

    @Override
    public @NotNull Storage getStorage() {
        return this.storage;
    }

    private class StorageImpl implements Storage {


        private PlayerOnlineTimeTable table = null;

        private @NotNull PlayerOnlineTimeTable getTable() throws Exception {
            if (this.table == null) {
                final DatabaseConnection dataBase = getDataBase();
                this.table = new PlayerOnlineTimeTable(dataBase.getConnection());
            }
            return this.table;
        }

        @Override
        public @Nullable Long query(@NotNull UUID uuid) throws Exception {
            synchronized (this) {
                final PlayerOnlineTimeTable table1 = this.getTable();
                final LinkedList<Long> query = table1.query(uuid.toString());
                final int size = query.size();
                if (size == 1) {
                    return query.get(0);
                } else if (size > 0) {
                    throw new Exception("根据一个UUID查到%d条数据！（应该是0或1）".formatted(size));
                } else {
                    return null;
                }
            }
        }

        @Override
        public @NotNull List<Record> queryAll() throws Exception {
            synchronized (this) {
                final PlayerOnlineTimeTable table1 = this.getTable();
                final LinkedList<PlayerOnlineTimeTable.Record> records = table1.queryAll();

                final LinkedList<Record> records1 = new LinkedList<>();
                for (PlayerOnlineTimeTable.Record record : records) {
                    records1.add(new Record(UUID.fromString(record.uuid()), record.time()));
                }

                return records1;
            }
        }

        @Override
        public void addTime(@NotNull UUID uuid, long time) throws Exception {
            synchronized (this) {
                final PlayerOnlineTimeTable table1 = this.getTable();
                final LinkedList<Long> query = table1.query(uuid.toString());
                final int size = query.size();
                if (size == 0) {
                    final int i = table1.insert(uuid.toString(), time);
                    if (i != 1) throw new Exception("数据库影响行数：%d（应该是1）".formatted(i));
                } else if (size == 1) {
                    final int i = table1.add(uuid.toString(), time);
                    if (i != 1) throw new Exception("数据库影响行数：%d（应该是1）".formatted(i));
                } else {
                    throw new Exception("根据一个UUID查到%d条数据！（应该是0或1）".formatted(size));
                }
            }
        }
    }


    // 记录玩家在线时长的数据表
    private static class PlayerOnlineTimeTable {

        record Record(String uuid, long time) {
        }

        private final static String NAME = "player_online_time";

        private final PreparedStatement statementInsert;

        private final PreparedStatement statementQuery;

        private final PreparedStatement statementAdd;

        private final PreparedStatement statementQueryAll;

        PlayerOnlineTimeTable(@NotNull Connection connection) throws SQLException {
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

        void close() throws SQLException {

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

        void createTable(@NotNull Connection connection) throws SQLException {
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

        @NotNull LinkedList<Long> query(@NotNull String uuid) throws SQLException {
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

        @NotNull LinkedList<Record> queryAll() throws SQLException {

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

        int insert(@NotNull String uuid, long time) throws SQLException {
            this.statementInsert.setString(1, uuid);
            this.statementInsert.setLong(2, time);

            return this.statementInsert.executeUpdate();
        }

        int add(@NotNull String uuid, long time) throws SQLException {
            this.statementAdd.setLong(1, time);
            this.statementAdd.setString(2, uuid);

            return this.statementAdd.executeUpdate();
        }
    }
}
