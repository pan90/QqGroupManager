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
        builder.append("可能会被踢出群的一日游玩家名单：\n");
        int i = 1;
        final long curTime = System.currentTimeMillis();
        builder.append("序号 | QQ | 昵称 | 游戏名 | 几天前上线 \n");
        for (final IAutoKick.OneDayPlayerMember oneDayPlayerMember : playOneDayList) {

            final long t = curTime - oneDayPlayerMember.player().getLastSeen();
            final long days = t / (24L * 60L * 60L * 1000L);

            builder.append("%d | %d | %s | %s | %d \n".formatted(
                    i,
                    oneDayPlayerMember.member().getId(),
                    oneDayPlayerMember.member().getNick(),
                    oneDayPlayerMember.player().getName(),
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

        StringBuilder builder = new StringBuilder();
        int i = 1;
        builder.append("可能会因为没有绑定游戏账号而被提出群的名单：\n");
        builder.append("序号 | QQ | 昵称 | 等级 \n");
        for (final MiraiNormalMember notBindMember : notBindMembers) {

            final MiraiMemberActive active = notBindMember.getActive();

            // 经过测试，这个是群等级
            final int temperature = active.getTemperature();

            builder.append("%d | %d | %s | %d \n".formatted(
                    i, notBindMember.getId(), notBindMember.getNick(), temperature
            ));

            if (i % 16 == 0) {
                group.sendMessage(builder.toString());
                builder = new StringBuilder();
            }

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

        StringBuilder builder = new StringBuilder();
        builder.append("序号 | 游戏名 | 在线时长（时） \n");

        int i = 1;
        for (final IOnlineTimeService.Storage.Record record : records) {
            final String name = plugin.getServer().getOfflinePlayer(record.uuid()).getName();
            final double hours = (double) record.time() / (60L * 60L * 1000L);

            builder.append("%d | %s | %.2f \n".formatted(i, name, hours));

            if (i % 16 == 0) {
                group.sendMessage(builder.toString());
                builder = new StringBuilder();
            }

            ++i;
        }

        group.sendMessage(builder.toString());

        return true;
    }

    private boolean handleViewNotEnoughTimeMember(@NotNull MiraiGroupMessageEvent event) {

        if (event.getSenderPermission() < 1) return false;

        final String message = event.getMessage();
        if (message == null) return false;
        if (!message.equals("查看平均每周在线时长不足二小时名单")) return false;

        final MiraiGroup group = event.getGroup();

        final ArrayList<IAutoKick.NotEnoughTimeMember> notEnoughTimeList;
        try {
            notEnoughTimeList = plugin.getAutoKick().createNotEnoughTimeList(group, 2L * 60L * 60L * 1000L);
        } catch (Exception e) {
            group.sendMessage("@%s 异常：%s".formatted(event.getSenderName(), e));
            e.printStackTrace();
            return true;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("可能会因为平均每周在线时长不足二小时而被踢掉的名单：\n");
        builder.append("序号 | QQ | 游戏名 | 平均每周在线时长（小时） \n");
        int i = 1;
        for (final IAutoKick.NotEnoughTimeMember member : notEnoughTimeList) {
            builder.append("%d | %d | %s | %.2f \n".formatted(
                    i, member.member().getId(), member.player().getName(), member.avgWeakOnlineTime()
            ));

            if (i % 16 == 0) {
                group.sendMessage(builder.toString());
                builder = new StringBuilder();
            }

            ++i;
        }

        group.sendMessage(builder.toString());

        return true;
    }

    private boolean handleViewKickList(@NotNull MiraiGroupMessageEvent event) {

        if (event.getSenderPermission() < 1) return false;

        final String message = event.getMessage();
        if (message == null) return false;
        final String prefix = "自动踢出名单生成人数";
        if (!message.startsWith(prefix)) return false;

        final MiraiGroup group = event.getGroup();

        final String substring = message.substring(prefix.length());
        final int num;
        try {
            num = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            group.sendMessage("@%s 请提供一个数字！".formatted(event.getSenderName()));
            return true;
        }

        if (num <= 0) {
            group.sendMessage("@%s 参数错误：%d".formatted(event.getSenderName(), num));
            return true;
        }

        final List<IAutoKick.Info> list;

        try {
            list = plugin.getAutoKick().createList(num);
        } catch (Exception e) {
            group.sendMessage("@%s 异常：%s".formatted(event.getSenderName(), e));
            e.printStackTrace();
            return true;
        }


        StringBuilder builder = new StringBuilder();
        builder.append("序号 | QQ | 等级 | 游戏名 | 昵称 | 原因 | 备注 \n");
        int i = 1;
        for (final IAutoKick.Info info : list) {

            final OfflinePlayer player = info.offlinePlayer();
            final String name = player != null ? player.getName() : "null";

            String reason = switch (info.kickType()) {
                case NoWhiteList -> "未添加白名单";
                case PlayOneDay -> "一日游玩家";
                case LongTimeNoPlay -> "平均在线时长不足";
            };

            builder.append("%d | %d | %d | %s | %s | %s | %s \n".formatted(
                    i,
                    info.member().getId(),
                    info.member().getActive().getTemperature(),
                    name,
                    info.member().getNick(),
                    reason,
                    info.extra()
            ));

            if (i % 16 == 0) {
                group.sendMessage(builder.toString());
                builder = new StringBuilder();
            }
            ++i;
        }

        group.sendMessage(builder.toString());
        return true;
    }

    @EventHandler
    public void on(@NotNull MiraiGroupMessageEvent event) {

        final long groupID = event.getGroupID();
        if (groupID != this.plugin.getQqGroupId()) return;

        if (this.handleViewNotEnoughTimeMember(event)) return;
        if (this.handleViewOnlineTop(event)) return;
        if (this.handleViewOneDayPlayer(event)) return;
        if (this.handleViewNotBind(event)) return;
        if (this.handleViewKickList(event)) return;
        this.doNothing();
    }

    private void doNothing() {
    }
}
