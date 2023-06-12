package cn.paper_card.qqgroupmanager.api;


import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IKickList {

    // 踢出类型
    enum KickType {
        NoWhiteList, // QQ没有绑定UUID，也就是没有白名单
        PlayOneDay, // 一日游玩家
        LongTimeNoPlay // 每周平均在线时长不足
    }

    // 一日游的玩家
    record OneDayPlayerMember(@NotNull MiraiNormalMember member, @NotNull OfflinePlayer player) {
    }

    // 平均在线时长不足的玩家
    record NotEnoughTimeMember(
            @NotNull MiraiNormalMember member,
            @NotNull OfflinePlayer player,
            double avgWeekOnlineTime, // 平均每周在线时长，单位毫秒
            long weeks, // 游玩周数
            long onlineTime // 在线时长，单位毫秒
    ) {
    }

    // 踢出信息
    record Info(
            @NotNull MiraiNormalMember member,
            @Nullable OfflinePlayer offlinePlayer,
            @NotNull KickType kickType,
            @Nullable String extra) {
    }

    interface OnKickFinish {
        void on(@NotNull List<Info> kicked, @NotNull List<Info> notKick);
    }

    // 生成一个未绑定游戏账号的QQ列表，按群等级由低到高排序
    @NotNull List<MiraiNormalMember> createNotBindList(@NotNull MiraiGroup group, int num) throws Exception;

    // 生成一个一日游玩家QQ列表，排序：最近在线时间从远到近
    @NotNull List<OneDayPlayerMember> createPlayOneDayList(@NotNull MiraiGroup group, int num) throws Exception;

    // 生成平均每周在线时间少于指定值的QQ列表
    @NotNull List<NotEnoughTimeMember> createNotEnoughTimeList(@NotNull MiraiGroup group, int num, long leastTimeWeak) throws Exception;

    @NotNull List<Info> createList(int num) throws Exception;

    @NotNull List<Info> transformNotBindList(@NotNull List<MiraiNormalMember> list);

    @NotNull List<Info> transformOneDayPlayList(@NotNull List<OneDayPlayerMember> list);

    @NotNull List<Info> transformNotEnoughTimeList(@NotNull List<NotEnoughTimeMember> list);

    void startKick(@NotNull List<Info> info, @NotNull OnKickFinish onKickFinish) throws Exception;

    void init();

    void destroy();
}
