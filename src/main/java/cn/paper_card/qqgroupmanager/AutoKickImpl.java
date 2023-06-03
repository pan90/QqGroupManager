package cn.paper_card.qqgroupmanager;

import cn.paper_card.papercardauth.PaperCardAuth;
import cn.paper_card.papercardauth.api.IUuidQqBindService;
import cn.paper_card.qqgroupmanager.api.IAutoKick;
import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

public class AutoKickImpl implements IAutoKick {

    private final @NotNull QqGroupManager plugin;

    public AutoKickImpl(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
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

            if (member.getPermission() > 0) continue; // 跳过管理员等

            // 跳过有头衔的
            final String specialTitle = member.getSpecialTitle();
            if (specialTitle != null && !specialTitle.equals("")) continue;

            // 跳过刚来不到一周的
            final long joinTimestamp = member.getJoinTimestamp() /* 这玩意是秒为单位的 */ * 1000L;
            if (curTime - joinTimestamp < 7L * 24L * 60L * 60L * 1000L) continue;


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
        if (lastSeen - firstPlayed < 24L * 60L * 60L * 1000L) {
            // 最后退出的时间与当前时间相差超过七天
            return curTime - lastSeen > 7L * 24L * 60L * 60L * 1000L;
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
            final long lastSeen = o1.player().getLastSeen();
            final long lastSeen1 = o2.player().getLastSeen();
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
            final long weak = t / (7L * 24L * 60L * 60L * 1000L); // 入服多少周了
            if (weak < 2) continue;  // 玩了不到两周

            // 总计在线时长，以毫秒为单位
            final Long onlineTimeTotal = onlineTimeService.getStorage().query(offlinePlayer.getUniqueId());

            // 查不到在线时长，当0处理
            if (onlineTimeTotal == null) {
                members.add(new NotEnoughTimeMember(member, offlinePlayer, 0));
                continue;
            }

            final double onlineTimePerWeak = (double) onlineTimeTotal / (double) weak;

            if (onlineTimePerWeak < leastTimeWeak) { // 平均每周在线时长不足
                members.add(new NotEnoughTimeMember(member, offlinePlayer, onlineTimePerWeak));
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

        final LinkedList<Info> info = new LinkedList<>();
        int c = 0;

        // 第一批次
        if (c < num) {
            final ArrayList<MiraiNormalMember> notBindList = this.createNotBindList(group);
            final long curTime = System.currentTimeMillis();
            for (final MiraiNormalMember member : notBindList) {

                final long join = member.getJoinTimestamp() * 1000L;
                long days = curTime - join;
                days /= (24L * 60L * 60L * 1000L);

                info.add(new Info(member, null, KickType.NoWhiteList, "入群天数：%d".formatted(days)));

                ++c;
                if (c >= num) break;
            }
        }

        // 第二批次
        if (c < num) {
            final ArrayList<OneDayPlayerMember> playOneDayList = this.createPlayOneDayList(group);
            final long curTime = System.currentTimeMillis();
            for (final OneDayPlayerMember oneDayPlayerMember : playOneDayList) {

                final long lastSeen = oneDayPlayerMember.player().getLastSeen();
                long days = curTime - lastSeen;
                days /= (24L * 60L * 60L * 1000L);

                info.add(new Info(oneDayPlayerMember.member(), oneDayPlayerMember.player(), KickType.PlayOneDay, "%d天未上线".formatted(days)));

                ++c;
                if (c >= num) break;
            }
        }

        // 第三批次
        if (c < num) {
            final ArrayList<NotEnoughTimeMember> notEnoughTimeList = this.createNotEnoughTimeList(group, 2L * 60L * 60L * 1000L);
            for (final NotEnoughTimeMember member : notEnoughTimeList) {

                info.add(new Info(member.member(), member.player(), KickType.LongTimeNoPlay,
                        "平均每周时长：%.2f".formatted(member.avgWeakOnlineTime())));

                ++c;
                if (c >= num) break;
            }
        }

        return info;
    }

    @Override
    public void doKick(List<Info> info) throws Exception {

    }

    @Override
    public void onEnable() {
        final PluginCommand command = this.plugin.getCommand("get-member-info");

        assert command != null;
        command.setExecutor((commandSender, command1, s, strings) -> {
            if (strings.length != 1) return false;

            final long qq;
            try {
                qq = Long.parseLong(strings[0]);
            } catch (NumberFormatException e) {
                commandSender.sendMessage(Component.text("请提供合法的QQ号码！"));
                return true;
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                final MiraiGroup group = plugin.findGroup();

                if (group == null) {
                    commandSender.sendMessage(Component.text("无法访问QQ群！"));
                    return;
                }

                final MiraiNormalMember member = findMember(group, qq);

                if (member == null) {
                    commandSender.sendMessage(Component.text("无法获取该QQ相关信息，可能已经退群"));
                    return;
                }

                final String nick = member.getNick();
                final String remark = member.getRemark();
                final String specialTitle = member.getSpecialTitle();
                final int joinTimestamp = member.getJoinTimestamp();
                final int lastSpeakTimestamp = member.getLastSpeakTimestamp();
                final int temperature = member.getActive().getTemperature();

                final int permission = member.getPermission();

                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");

                commandSender.sendMessage(Component.text()
                        .append(Component.text("Nick: %s\n".formatted(nick)))
                        .append(Component.text("Remark: %s\n".formatted(remark)))
                        .append(Component.text("SpecialTitle: %s\n".formatted(specialTitle)))
                        .append(Component.text("Permission: %d\n".formatted(permission)))
                        .append(Component.text("Temperature: %d\n".formatted(temperature)))
                        .append(Component.text("JoinTime: %s\n".formatted(simpleDateFormat.format(new Date(joinTimestamp * 1000L)))))
                        .append(Component.text("lastSpeak: %s\n".formatted(simpleDateFormat.format(new Date(lastSpeakTimestamp * 1000L)))))
                        .append(Component.text("CurrentTime: %s".formatted(simpleDateFormat.format(new Date()))))
                        .build()
                );
            });

            return true;
        });
    }

    @Override
    public void onDisable() {

    }
}
