package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.*;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public final class QqGroupManager extends JavaPlugin implements IQqGroupManager {

    private final @NotNull IOnlineTimeService onlineTimeService;

    private final @NotNull IKickList autoKick;
    private final @NotNull IKickListAuditGroup kickListAuditGroup;

    private final @NotNull KickListCmd.SessionManager sessionManager;

    private final @NotNull IQqBlackListService blackListService;

    private final @NotNull Utility utility;

    public QqGroupManager() {
        this.onlineTimeService = new OnlineTimeService(this);
        this.autoKick = new KickListImpl(this);
        this.kickListAuditGroup = new KickListAuditGroupImpl(this);
        this.sessionManager = new KickListCmd.SessionManager();
        this.utility = new Utility();
        this.blackListService = new QqBlackListServiceImpl(this);
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

    @NotNull KickListCmd.SessionManager getSessionManager() {
        return this.sessionManager;
    }

    @NotNull Utility getUtility() {
        return this.utility;
    }

    public @NotNull IOnlineTimeService getOnlineTimeService() {
        return this.onlineTimeService;
    }

    public @NotNull IKickList getAutoKick() {
        return this.autoKick;
    }

    public @NotNull IKickListAuditGroup getKickListAuditGroup() {
        return this.kickListAuditGroup;
    }

    public @NotNull IQqBlackListService getBlackListService() {
        return this.blackListService;
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

    public @Nullable MiraiGroup findAuditGroup() {
        for (final Long onlineBot : MiraiBot.getOnlineBots()) {
            try {
                final MiraiBot bot = MiraiBot.getBot(onlineBot); // not null
                if (bot.isExist() && bot.isOnline()) {
                    final MiraiGroup group = bot.getGroup(this.getAuditQqGroupID());
                    if (isValidGroup(group)) {
                        return group;
                    }
                }
            } catch (NoSuchElementException ignored) {
            }
        }
        return null;
    }

    public static @Nullable MiraiNormalMember findMember(@NotNull MiraiGroup group, long qq) {
        try {
            final MiraiNormalMember member = group.getMember(qq);
            member.getId();
            return member;
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public void onEnable() {

        final PluginManager manager = this.getServer().getPluginManager();

        manager.registerEvents(new OnMessage(this), this);
        manager.registerEvents(new OnJoinGroup(this), this);

        this.autoKick.init();
        this.onlineTimeService.init();
        this.blackListService.init();
    }

    @Override
    public void onDisable() {
        this.blackListService.destroy();
        this.onlineTimeService.destroy();
        this.autoKick.destroy();

    }

    @Override
    public void sendMessageLater(@NotNull String message) {

    }

    @Override
    public void sendAtMessageLater(long qq, @NotNull String message) {

    }


    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }
}
