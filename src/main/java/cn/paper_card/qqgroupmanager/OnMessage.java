package cn.paper_card.qqgroupmanager;

import cn.paper_card.papercardauth.PaperCardAuth;
import cn.paper_card.papercardauth.api.IUuidQqBindService;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.group.member.MiraiMemberJoinEvent;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OnMessage implements Listener {

    private final @NotNull QqGroupManager plugin;

    public OnMessage(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on2(@NotNull MiraiMemberJoinEvent event) {
        final long groupID = event.getGroupID();

        if (groupID == this.plugin.getAuditQqGroupID()) {
            event.getGroup().sendMessage("@%s 欢迎入群，请查看【群公告】填写【审核问卷】~".formatted(event.getMemberNick()));
        }

        if (groupID == this.plugin.getQqGroupId()) {
            event.getGroup().sendMessage("@%s 欢迎新伙伴入裙~\n请查看群公告【新人必看】~".formatted(event.getMemberNick()));
        }
    }

    private boolean isOneDayPlayer(@NotNull OfflinePlayer player, long curTime) {
        final long firstPlayed = player.getFirstPlayed();
        final long lastSeen = player.getLastSeen();


        // 第一次进来玩的时间和最后一次退出的时间相差不到一天
        if (lastSeen - firstPlayed < 24 * 60 * 60 * 1000) {
            // 最后退出的时间与当前时间相差超过七天
            return curTime - lastSeen > 7 * 24 * 60 * 60 * 1000;
        }
        return false;
    }

    public void handleViewOneDayPlayer(@NotNull MiraiGroupMessageEvent event) {
        final long groupID = event.getGroupID();
        if (groupID != this.plugin.getQqGroupId()) return;
        if (event.getSenderPermission() < 1) return;


        final String message = event.getMessage();


        if (message == null) return;
        if (!message.equals("查看一日游玩家名单")) return;

        final MiraiGroup group = event.getGroup();


        final LinkedList<OfflinePlayer> players = new LinkedList<>();
        final long curTime = System.currentTimeMillis();
        for (final OfflinePlayer offlinePlayer : this.plugin.getServer().getOfflinePlayers()) {
            if (this.isOneDayPlayer(offlinePlayer, curTime)) {
                players.add(offlinePlayer);
            }
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("一共%d个可能的一日游玩家：\n".formatted(players.size()));
        int i = 1;
        builder.append("序号 | 名字\n");
        for (final OfflinePlayer player : players) {
            String name = player.getName();
            if (name == null) name = player.getUniqueId().toString();

            builder.append("%d | %s\n".formatted(i, name));

            ++i;
        }

        group.sendMessage(builder.toString());
    }

    @EventHandler
    public void on(@NotNull MiraiGroupMessageEvent event) {

        this.handleViewOneDayPlayer(event);

        final long groupID = event.getGroupID();
        if (groupID != this.plugin.getQqGroupId()) return;
        if (event.getSenderPermission() < 1) return;


        final String message = event.getMessage();


        if (message == null) return;
        if (!message.equals("查看未添加白名单QQ")) return;

        final MiraiGroup group = event.getGroup();

        final List<MiraiNormalMember> members = group.getMembers();

        final Plugin paperCardAuth = this.plugin.getServer().getPluginManager().getPlugin("PaperCardAuth");
        if (!(paperCardAuth instanceof PaperCardAuth auth)) return;

        final IUuidQqBindService uuidQqBindService = auth.getWhitelistManager().getUuidQqBindService();

        final ArrayList<MiraiNormalMember> notBindMembers = new ArrayList<>();

        for (final MiraiNormalMember member : members) {
            final UUID uuid;
            try {
                uuid = uuidQqBindService.queryByQq(member.getId());
            } catch (Exception e) {
                group.sendMessage("@%s 异常：%s".formatted(event.getSenderName(), e));
                return;
            }

            if (uuid == null) notBindMembers.add(member);
        }

        final StringBuilder builder = new StringBuilder();
        int i = 1;
        final LinkedList<Integer> integers = new LinkedList<>();
        builder.append("序号 | QQ | Remark | Nick | 头衔");
        for (final MiraiNormalMember notBindMember : notBindMembers) {
            final String specialTitle = notBindMember.getSpecialTitle();

            builder.append("%d | %d | %s | %s | %s\n".formatted(
                    i, notBindMember.getId(), notBindMember.getRemark(), notBindMember.getNick(), specialTitle
            ));

            if (specialTitle != null && !specialTitle.equals("")) {
                integers.add(i);
            }

            ++i;
        }

        builder.append("有自定义头衔的（序号）：");
        for (final Integer integer : integers) {
            builder.append(integer);
            builder.append(", ");
        }

        group.sendMessage(builder.toString());
    }
}
