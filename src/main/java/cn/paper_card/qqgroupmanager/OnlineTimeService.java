package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import cn.paper_card.qqgroupmanager.data.DataBase;
import cn.paper_card.qqgroupmanager.data.PlayerOnlineTimeTable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineTimeService implements IOnlineTimeService {

    private final @NotNull QqGroupManager plugin;

    private final @NotNull Storage storage;

    public OnlineTimeService(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
        this.storage = new StorageImpl();
    }


    @Override
    public void onEnable() {
        this.plugin.getServer().getPluginManager().registerEvents(new Listener() {

            private final ConcurrentHashMap<UUID, Long> joinTimes = new ConcurrentHashMap<>();

            @EventHandler
            public void on(@NotNull PlayerJoinEvent event) {
                // 记录开始上线时间
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                        () -> this.joinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis()));
            }

            @EventHandler
            public void on2(@NotNull PlayerQuitEvent event) {
                // 将在线时间写入数据库
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

                    final UUID id = event.getPlayer().getUniqueId();
                    final String name = event.getPlayer().getName();
                    final Long joinTime = this.joinTimes.get(id);
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
    public void onDisable() {

    }

    @Override
    public @NotNull Storage getStorage() {
        return this.storage;
    }

    private class StorageImpl implements Storage {


        private PlayerOnlineTimeTable table = null;

        private @NotNull PlayerOnlineTimeTable getTable() throws SQLException, ClassNotFoundException {
            if (this.table == null) {
                final DataBase dataBase = OnlineTimeService.this.plugin.getDataBase();
                this.table = dataBase.getPlayerOnlineTimeTable();
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
}
