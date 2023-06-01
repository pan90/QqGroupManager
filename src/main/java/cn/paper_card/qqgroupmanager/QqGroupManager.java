package cn.paper_card.qqgroupmanager;

import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

public final class QqGroupManager extends JavaPlugin {

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

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
