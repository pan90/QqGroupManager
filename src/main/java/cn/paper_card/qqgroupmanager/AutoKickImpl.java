package cn.paper_card.qqgroupmanager;

import cn.paper_card.papercardauth.PaperCardAuth;
import cn.paper_card.papercardauth.api.IUuidQqBindService;
import cn.paper_card.qqgroupmanager.api.IAutoKick;
import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AutoKickImpl implements IAutoKick {

    private final @NotNull QqGroupManager plugin;

    public AutoKickImpl(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
    }

    private static boolean isValidGroup(@NotNull MiraiGroup group) {
        try {
            group.getName();
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }

    private static @Nullable MiraiNormalMember findMember(@NotNull MiraiGroup group, long qq) {
        try {
            final MiraiNormalMember member = group.getMember(qq);
            member.getId();
            return member;
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public @NotNull ArrayList<MiraiNormalMember> createNotBindList(@NotNull MiraiGroup group) throws Exception {
        final ArrayList<MiraiNormalMember> members = new ArrayList<>();

        final Plugin paperCardAuth = this.plugin.getServer().getPluginManager().getPlugin("PaperCardAuth");
        if (!(paperCardAuth instanceof PaperCardAuth auth)) throw new Exception("PaperCardAuth插件未安装！");

        final IUuidQqBindService uuidQqBindService = auth.getWhitelistManager().getUuidQqBindService();

        // 获取未绑定UUID的
        final long curTime = System.currentTimeMillis();
        for (final MiraiNormalMember member : group.getMembers()) {
            final UUID uuid = uuidQqBindService.queryByQq(member.getId());

            if (uuid != null) continue; // 已经绑定了的

            // 跳过有头衔的
            final String specialTitle = member.getSpecialTitle();
            if (specialTitle != null && !specialTitle.equals("")) continue;

            // 跳过刚来不到一周的
            final long joinTimestamp = member.getJoinTimestamp() /* 这玩意是秒为单位的 */ * 1000;

            if (curTime - joinTimestamp < 7 * 24 * 60 * 60 * 1000) continue;

            members.add(member);
        }

        // 按群等级排序，从低到高
        members.sort((o1, o2) -> {
            final int temperature = o1.getActive().getTemperature();
            final int temperature1 = o2.getActive().getTemperature();
            return temperature - temperature1;
        });

        return members;
    }

    private boolean isOneDayPlayer(@NotNull OfflinePlayer player, long curTime) {
        final long firstPlayed = player.getFirstPlayed();
        final long lastSeen = player.getLastSeen();

        // 第一次进来玩的时间和最后一次退出的时间相差不到一天
        if (lastSeen - firstPlayed < 24 * 60 * 60 * 1000) {
            // 最后退出的时间与当前时间相差超过七天
            if (curTime - lastSeen > 7 * 24 * 60 * 60 * 1000) {
                return true;
            }
        }
        return false; // 不是一日游玩家
    }

    @Override
    public @NotNull ArrayList<OneDayPlayerMember> createPlayOneDayList(@NotNull MiraiGroup group) throws Exception {

        final Plugin paperCardAuth = this.plugin.getServer().getPluginManager().getPlugin("PaperCardAuth");
        if (!(paperCardAuth instanceof PaperCardAuth auth)) throw new Exception("PaperCardAuth插件未安装！");

        final IUuidQqBindService uuidQqBindService = auth.getWhitelistManager().getUuidQqBindService();

        final ArrayList<OneDayPlayerMember> members = new ArrayList<>();
        final long curTime = System.currentTimeMillis();
        for (final OfflinePlayer offlinePlayer : this.plugin.getServer().getOfflinePlayers()) {

            if (!isOneDayPlayer(offlinePlayer, curTime)) continue; // 不是一日游玩家

            final UUID id = offlinePlayer.getUniqueId();
            final Long qq = uuidQqBindService.queryByUuid(id);
            if (qq == null) continue; // 跳过没有QQ的

            final MiraiNormalMember member = findMember(group, qq);
            if (member == null) continue; // 应该不在群里了

            // 拥有头衔
            final String specialTitle = member.getSpecialTitle();
            if (specialTitle != null && !specialTitle.equals("")) continue;

            if (member.getPermission() > 0) continue; // 群管理

            members.add(new OneDayPlayerMember(member, offlinePlayer));
        }

        // 排序
        members.sort((o1, o2) -> {
            final long lastSeen = o1.offlinePlayer().getLastSeen();
            final long lastSeen1 = o2.offlinePlayer().getLastSeen();
            return (int) (lastSeen - lastSeen1);
        });


        return members;
    }

    @Override
    public @NotNull ArrayList<NotEnoughTimeMember> createNotEnoughTimeList(@NotNull MiraiGroup group, long leastTimeWeak) throws Exception {

        final Plugin paperCardAuth = this.plugin.getServer().getPluginManager().getPlugin("PaperCardAuth");
        if (!(paperCardAuth instanceof PaperCardAuth auth)) throw new Exception("PaperCardAuth插件未安装！");

        final IUuidQqBindService uuidQqBindService = auth.getWhitelistManager().getUuidQqBindService();
        final IOnlineTimeService onlineTimeService = plugin.getOnlineTimeService();

        final ArrayList<NotEnoughTimeMember> members = new ArrayList<>();

        final long curTime = System.currentTimeMillis();
        for (final OfflinePlayer offlinePlayer : this.plugin.getServer().getOfflinePlayers()) {

            final Long qq = uuidQqBindService.queryByUuid(offlinePlayer.getUniqueId());
            if (qq == null) continue; // 没有绑定QQ

            final MiraiNormalMember member = findMember(group, qq);
            if (member == null) continue; // 可能已经退群了

            // 拥有自定义头衔
            final String specialTitle = member.getSpecialTitle();
            if (specialTitle != null && !specialTitle.equals("")) continue;

            // 群管理
            if (member.getPermission() > 0) continue;

            final long t = curTime - offlinePlayer.getFirstPlayed(); // 入服多久了
            final long weak = t / (7 * 24 * 60 * 60 * 1000); // 多少周了
            if (weak < 2) continue;  // 玩了不到两周

            final Long onlineTimeTotal = onlineTimeService.getStorage().query(offlinePlayer.getUniqueId());

            // 查不到在线时长，当0处理
            if (onlineTimeTotal == null) {
                members.add(new NotEnoughTimeMember(member, 0));
                continue;
            }

            final long onlineTimePerWeak = onlineTimeTotal / weak;

            if (onlineTimePerWeak < leastTimeWeak) { // 平均每周在线时长不足
                members.add(new NotEnoughTimeMember(member, onlineTimePerWeak));
            }
        }

        members.sort((o1, o2) -> (int) (o1.avgWeakOnlineTime() - o2.avgWeakOnlineTime()));

        return members;
    }


    @Override
    public boolean handleMessage(@NotNull MiraiGroupMessageEvent event) {
        return false;
    }

    @Override
    public @NotNull List<Info> createList(int num) throws Exception {
        final MiraiGroup group = this.plugin.findGroup();

        if (group == null) throw new Exception("无法访问QQ群！");


        for (final MiraiNormalMember member : group.getMembers()) {

        }

        return null;
    }

    @Override
    public void doKick(List<Info> info) throws Exception {

    }
}
