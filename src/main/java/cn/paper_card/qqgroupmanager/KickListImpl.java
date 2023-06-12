package cn.paper_card.qqgroupmanager;

import cn.paper_card.papercardauth.PaperCardAuth;
import cn.paper_card.papercardauth.api.IUuidQqBindService;
import cn.paper_card.qqgroupmanager.api.IKickList;
import cn.paper_card.qqgroupmanager.api.IOnlineTimeService;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

class KickListImpl implements IKickList {

    private final @NotNull QqGroupManager plugin;

    public KickListImpl(@NotNull QqGroupManager plugin) {
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

    private @NotNull IUuidQqBindService getBindService() throws Exception {

        final Plugin paperCardAuth = this.plugin.getServer().getPluginManager().getPlugin("PaperCardAuth");
        if (!(paperCardAuth instanceof PaperCardAuth auth)) throw new Exception("PaperCardAuth插件未安装！");

        return auth.getWhitelistManager().getUuidQqBindService();
    }

    @Override
    public @NotNull List<MiraiNormalMember> createNotBindList(@NotNull MiraiGroup group, int num) throws Exception {

        final ArrayList<MiraiNormalMember> members = new ArrayList<>();

        if (num == 0) return members;

        final IUuidQqBindService uuidQqBindService = this.getBindService();

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

        if (num < 0) return members;

        int c = 0;
        final LinkedList<MiraiNormalMember> members1 = new LinkedList<>();
        for (final MiraiNormalMember member : members) {
            if (c < num) {
                members1.add(member);
                ++c;
            } else break;
        }

        return members1;
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
    public @NotNull List<OneDayPlayerMember> createPlayOneDayList(@NotNull MiraiGroup group, int num) throws Exception {

        final IUuidQqBindService bindService = this.getBindService();

        final ArrayList<OneDayPlayerMember> members = new ArrayList<>();

        if (num == 0) return members;

        final long curTime = System.currentTimeMillis();

        for (final OfflinePlayer offlinePlayer : this.plugin.getServer().getOfflinePlayers()) {

            if (!isOneDayPlayer(offlinePlayer, curTime)) continue; // 不是一日游玩家

            final UUID id = offlinePlayer.getUniqueId();
            final Long qq = bindService.queryByUuid(id);
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

        if (num < 0) return members;

        final LinkedList<OneDayPlayerMember> members1 = new LinkedList<>();
        int c = 0;
        for (final OneDayPlayerMember member : members) {
            if (c < num) {
                members1.add(member);
                ++c;
            } else break;
        }
        return members1;
    }

    @Override
    public @NotNull List<NotEnoughTimeMember> createNotEnoughTimeList(@NotNull MiraiGroup group, int num, long leastTimeWeak) throws Exception {

        final IUuidQqBindService uuidQqBindService = this.getBindService();
        final IOnlineTimeService onlineTimeService = plugin.getOnlineTimeService();

        final ArrayList<NotEnoughTimeMember> members = new ArrayList<>();

        if (num == 0) return members;

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
            final long weeks = t / (7L * 24L * 60L * 60L * 1000L); // 入服多少周了
            if (weeks < 2) continue;  // 玩了不到两周

            // 总计在线时长，以毫秒为单位
            Long onlineTimeTotal = onlineTimeService.getStorage().query(offlinePlayer.getUniqueId());

            // 查不到在线时长，当0处理
            if (onlineTimeTotal == null) {
                onlineTimeTotal = 0L;
            }

            final double onlineTimePerWeak = (double) onlineTimeTotal / (double) weeks;

            if (onlineTimePerWeak < leastTimeWeak) { // 平均每周在线时长不足

                final NotEnoughTimeMember m = new NotEnoughTimeMember
                        (member, offlinePlayer, onlineTimePerWeak, weeks, onlineTimeTotal);

                members.add(m);
            }
        }

        members.sort((o1, o2) -> (int) (o1.avgWeekOnlineTime() - o2.avgWeekOnlineTime()));

        if (num < 0) return members;

        int c = 0;
        final LinkedList<NotEnoughTimeMember> members1 = new LinkedList<>();

        for (final NotEnoughTimeMember member : members) {
            if (c < num) {
                members1.add(member);
                ++c;
            } else break;
        }

        return members1;
    }

    @Override
    public @NotNull List<Info> createList(int num) throws Exception {
        final MiraiGroup group = this.plugin.findGroup();

        if (group == null) throw new Exception("无法访问QQ群！");

        final LinkedList<Info> info = new LinkedList<>();
        int c = 0;

        // 第一批次
        if (c < num) {
            final List<MiraiNormalMember> notBindList = this.createNotBindList(group, -1);
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
            final List<OneDayPlayerMember> playOneDayList = this.createPlayOneDayList(group, -1);
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
            final List<NotEnoughTimeMember> notEnoughTimeList = this.createNotEnoughTimeList(group, -1, 2L * 60L * 60L * 1000L);
            for (final NotEnoughTimeMember member : notEnoughTimeList) {

                info.add(new Info(member.member(), member.player(), KickType.LongTimeNoPlay,
                        "平均每周时长：%.2f".formatted(member.avgWeekOnlineTime())));

                ++c;
                if (c >= num) break;
            }
        }

        return info;
    }

    @Override
    public @NotNull List<Info> transformNotBindList(@NotNull List<MiraiNormalMember> list) {
        final LinkedList<Info> is = new LinkedList<>();

        final long l = System.currentTimeMillis();

        for (final MiraiNormalMember member : list) {

            final int joinTimestamp = member.getJoinTimestamp();
            final long delta = l - joinTimestamp * 1000L;
            final double days = (double) delta / (24L * 60L * 60L * 1000L);

            is.add(new Info(
                    member,
                    null,
                    KickType.NoWhiteList,
                    "入群天数：%.2f".formatted(days)
            ));
        }

        return is;
    }

    @Override
    public @NotNull List<Info> transformOneDayPlayList(@NotNull List<OneDayPlayerMember> list) {
        final LinkedList<Info> is = new LinkedList<>();

        final long l = System.currentTimeMillis();

        for (final OneDayPlayerMember m : list) {

            final long lastSeen = m.player().getLastSeen();
            final long delta = l - lastSeen;
            final double days = (double) delta / (24L * 60L * 60L * 1000L);

            is.add(new Info(
                    m.member(),
                    m.player(),
                    KickType.PlayOneDay,
                    "%.2f天前上线".formatted(days)
            ));
        }

        return is;

    }

    @Override
    public @NotNull List<Info> transformNotEnoughTimeList(@NotNull List<NotEnoughTimeMember> list) {
        final LinkedList<Info> is = new LinkedList<>();

        for (final NotEnoughTimeMember m : list) {

            final double v = m.avgWeekOnlineTime() / (60L * 60L * 1000L);

            is.add(new Info(
                    m.member(),
                    m.player(),
                    KickType.LongTimeNoPlay,
                    "周数：%d，平均每周%.2f小时".formatted(m.weeks(), v)
            ));
        }
        return is;
    }

    @Override
    public void startKick(@NotNull List<Info> info, @NotNull OnKickFinish onKickFinish) throws Exception {

        final MiraiGroup group = plugin.findGroup();

        if (group == null) throw new Exception("无法访问QQ群");


        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            final LinkedList<Info> kicked = new LinkedList<>();
            final LinkedList<Info> notKicked = new LinkedList<>();

            for (final Info info1 : info) {
                MiraiNormalMember member = info1.member();

                // 重新查找，避免重复踢出
                member = findMember(group, member.getId());

                if (member == null) continue;

                String reason = "未知原因";
                switch (info1.kickType()) {
                    case PlayOneDay -> reason = "一日游玩家，" + info1.extra();
                    case NoWhiteList -> reason = "未添加白名单，" + info1.extra();
                    case LongTimeNoPlay -> reason = "在线时长不足，" + info1.extra();
                }

                try {
                    member.doKick(reason);
                    kicked.add(info1);
                } catch (Exception e) {
                    e.printStackTrace();
                    notKicked.add(info1);
                }

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            onKickFinish.on(kicked, notKicked);
        });
    }

    @Override
    public void init() {
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
    public void destroy() {

    }
}
