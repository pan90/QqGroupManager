package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IAutoKick;
import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiMemberActive;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.group.member.MiraiMemberJoinEvent;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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

    public boolean handleViewOneDayPlayer(@NotNull MiraiGroupMessageEvent event) {

        if (event.getSenderPermission() < 1) return false;

        final String message = event.getMessage();
        if (message == null) return false;
        if (!message.equals("查看一日游玩家名单")) return false;

        final MiraiGroup group = event.getGroup();

        final ArrayList<IAutoKick.OneDayPlayerMember> playOneDayList;
        try {
            playOneDayList = this.plugin.getAutoKick().createPlayOneDayList(group);
        } catch (Exception e) {
            group.sendMessage("@%s 异常：%s".formatted(event.getSenderName(), e));
            e.printStackTrace();
            return true;
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("\n");
        int i = 1;
        final long curTime = System.currentTimeMillis();
        builder.append("序号 | QQ | 昵称 | 游戏名 | 几天前上线\n");
        for (final IAutoKick.OneDayPlayerMember oneDayPlayerMember : playOneDayList) {

            final long t = curTime - oneDayPlayerMember.offlinePlayer().getLastSeen();
            final long days = t / (24 * 60 * 60 * 1000);

            builder.append("%d | %d | %s | %s | %d\n".formatted(
                    i,
                    oneDayPlayerMember.member().getId(),
                    oneDayPlayerMember.member().getNick(),
                    oneDayPlayerMember.offlinePlayer().getName(),
                    days
            ));
            ++i;
        }

        group.sendMessage(builder.toString());

        return true;
    }

    private boolean handleViewNotBind(@NotNull MiraiGroupMessageEvent event) {

        if (event.getSenderPermission() < 1) return false;

        final String message = event.getMessage();

        if (message == null) return false;
        if (!message.equals("查看未添加白名单QQ")) return false;

        final MiraiGroup group = event.getGroup();

        final ArrayList<MiraiNormalMember> notBindMembers;
        try {
            notBindMembers = plugin.getAutoKick().createNotBindList(group);
        } catch (Exception e) {
            group.sendMessage("@%s 异常：%s".formatted(event.getSenderName(), e));
            e.printStackTrace();
            return true;
        }

        final StringBuilder builder = new StringBuilder();
        int i = 1;
        builder.append("序号 | QQ | 昵称 | 等级\n");
        for (final MiraiNormalMember notBindMember : notBindMembers) {

            final MiraiMemberActive active = notBindMember.getActive();

            // 经过测试，这个是群等级
            final int temperature = active.getTemperature();

            builder.append("%d | %d | %s | %d\n".formatted(
                    i, notBindMember.getId(), notBindMember.getNick(), temperature
            ));
            ++i;
        }

        group.sendMessage(builder.toString());

        return true;
    }

    private boolean handleViewOnlineTop(@NotNull MiraiGroupMessageEvent event) {
        if (event.getSenderPermission() < 1) return false;

        final String message = event.getMessage();
        if (message == null) return false;
        if (!message.equals("查看在线时长排行榜")) return false;

        final MiraiGroup group = event.getGroup();

        final List<IOnlineTimeService.Storage.Record> records;

        try {
            records = plugin.getOnlineTimeService().getStorage().queryAll();
        } catch (Exception e) {
            group.sendMessage("@%s 异常：%s".formatted(event.getSenderName(), e));
            e.printStackTrace();
            return true;
        }

        records.sort((o1, o2) -> (int) (o2.time() - o1.time()));

        final StringBuilder builder = new StringBuilder();
        builder.append("序号 | 游戏名 | 在线时长（时）");

        int i = 1;
        for (final IOnlineTimeService.Storage.Record record : records) {
            final String name = plugin.getServer().getOfflinePlayer(record.uuid()).getName();
            final long hours = record.time() / (60 * 60 * 1000);

            builder.append("%d | %s | %d \n".formatted(i, name, hours));

            ++i;
        }

        group.sendMessage(builder.toString());

        return true;
    }

    @EventHandler
    public void on(@NotNull MiraiGroupMessageEvent event) {

        final long groupID = event.getGroupID();
        if (groupID != this.plugin.getQqGroupId()) return;

        if (this.handleViewOneDayPlayer(event)) return;
        if (this.handleViewNotBind(event)) return;
        this.handleViewOnlineTop(event);
    }
}
