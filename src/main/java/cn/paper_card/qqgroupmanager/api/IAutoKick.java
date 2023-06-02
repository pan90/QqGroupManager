package cn.paper_card.qqgroupmanager.api;


import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface IAutoKick {

    enum KickType {
        NoWhiteList,
        PlayOneDay,
        LongTimeNoPlay
    }

    record OneDayPlayerMember(@NotNull MiraiNormalMember member, @NotNull OfflinePlayer offlinePlayer) {
    }

    record NotEnoughTimeMember(@NotNull MiraiNormalMember member, long avgWeakOnlineTime) {
    }

    record Info(long qq, String nick, String remark, UUID uuid, KickType type) {
    }

    boolean handleMessage(@NotNull MiraiGroupMessageEvent event);

    // 生成一个未绑定游戏账号的QQ列表，按群等级由低到高排序
    @NotNull ArrayList<MiraiNormalMember> createNotBindList(@NotNull MiraiGroup group) throws Exception;

    // 生成一个一日游玩家QQ列表，排序：最近在线时间从远到近
    @NotNull ArrayList<OneDayPlayerMember> createPlayOneDayList(@NotNull MiraiGroup group) throws Exception;

    // 生成平均每周在线时间少于指定值的QQ列表
    @NotNull ArrayList<NotEnoughTimeMember> createNotEnoughTimeList(@NotNull MiraiGroup group, long leastTimeWeak) throws Exception;

    @NotNull List<Info> createList(int num) throws Exception;

    void doKick(List<Info> info) throws Exception;
}
