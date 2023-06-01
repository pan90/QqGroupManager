package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import cn.paper_card.qqgroupmanager.data.DataBase;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.NoSuchElementException;

public final class QqGroupManager extends JavaPlugin {

    private final @NotNull IOnlineTimeService onlineTimeService;

    private DataBase dataBase = null;

    public QqGroupManager() {
        this.onlineTimeService = new OnlineTimeService(this);
    }

    public @NotNull DataBase getDataBase() throws SQLException, ClassNotFoundException {
        if (this.dataBase == null) {
            this.dataBase = new DataBase(this);
        }
        return this.dataBase;
    }

    public long getQqGroupId() {
        return 706926037L;
    }


    public long getAuditQqGroupID() {
        return 747760104L;
    }

    private static boolean isValidGroup(@NotNull MiraiGroup group) {
        try {
            group.getName();
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public @NotNull IOnlineTimeService getOnlineTimeService() {
        return this.onlineTimeService;
    }

    public @Nullable MiraiGroup findGroup() {
        for (final Long onlineBot : MiraiBot.getOnlineBots()) {
            try {
                final MiraiBot bot = MiraiBot.getBot(onlineBot); // not null
                if (bot.isExist() && bot.isOnline()) {
                    final MiraiGroup group = bot.getGroup(this.getQqGroupId());
                    if (isValidGroup(group)) {
                        return group;
                    }
                }
            } catch (NoSuchElementException ignored) {
            }
        }
        return null;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new OnMessage(this), this);

        try {
            this.getDataBase();
        } catch (SQLException | ClassNotFoundException e) {
            this.getLogger().severe("连接数据库时异常：" + e);
        }

        this.onlineTimeService.onEnable();

    }

    @Override
    public void onDisable() {
        this.onlineTimeService.onDisable();

        if (this.dataBase != null) {
            try {
                this.dataBase.close();
            } catch (SQLException e) {
                this.getLogger().severe("关闭数据库连接时异常：" + e);
                e.printStackTrace();
            }
            this.dataBase = null;
        }
    }
}
