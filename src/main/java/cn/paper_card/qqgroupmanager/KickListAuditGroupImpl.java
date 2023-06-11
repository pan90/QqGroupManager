package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IKickListAuditGroup;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

class KickListAuditGroupImpl implements IKickListAuditGroup {

    KickListAuditGroupImpl(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
    }

    private static class SessionImpl implements Session {

        private List<MemberInfo> kickList = null;

        @Override
        public void setKickList(@Nullable List<MemberInfo> list) {
            this.kickList = list;
        }

        @Override
        public @Nullable List<MemberInfo> getKickList() {
            return this.kickList;
        }
    }

    private static class SessionManagerImpl implements SessionManager {
        private final @NotNull HashMap<Long, Session> map = new HashMap<>();

        @Override
        public @NotNull Session getSession(long qq) {
            synchronized (this) {
                Session s = this.map.get(qq);
                if (s != null) return s;

                s = new SessionImpl();
                this.map.put(qq, s);
                return s;
            }
        }
    }


    private final @NotNull KickListAuditGroupImpl.SessionManagerImpl sessionManager = new SessionManagerImpl();

    private final @NotNull QqGroupManager plugin;

    @Override
    public @NotNull SessionManager getSessionManager() {
        return this.sessionManager;
    }

    @Override
    public @NotNull List<MemberInfo> createListSortByJoinTime(int num) throws Exception {

        // 遍历审核群
        final MiraiGroup auditGroup = this.plugin.findAuditGroup();
        if (auditGroup == null) throw new Exception("无法访问审核群！");

        final List<MiraiNormalMember> list = new LinkedList<>(auditGroup.getMembers());

        // 按入群时间进行排序
        list.sort(Comparator.comparingInt(MiraiNormalMember::getJoinTimestamp));

        final List<MemberInfo> list2 = new LinkedList<>();

        if (num == 0) return list2;


        for (final MiraiNormalMember member : list) {

            // 跳过管理
            if (member.getPermission() > 0) continue;

            // 有自定义头衔
            final String specialTitle = member.getSpecialTitle();
            if (specialTitle != null && !specialTitle.equals("")) continue;

            list2.add(new MemberInfo(
                    member.getId(),
                    member.getNick(),
                    member.getJoinTimestamp(),
                    -1
            ));
        }

        if (num < -1) return list2;

        final LinkedList<MemberInfo> list3 = new LinkedList<>();

        int c = 0;

        for (final MemberInfo mi : list2) {
            if (c < num) {
                list3.add(mi);
                ++c;
            } else break;
        }

        return list3;
    }

    @Override
    public @NotNull List<MemberInfo> createListHasJoinGroup(int num) throws Exception {


        final MiraiGroup group = this.plugin.findGroup();
        final MiraiGroup auditGroup = this.plugin.findAuditGroup();

        if (group == null) throw new Exception("无法访问主群！");
        if (auditGroup == null) throw new Exception("无法访问审核群！");


        final List<MemberInfo> list = new LinkedList<>();

        if (num == 0) return list;

        for (final MiraiNormalMember member : auditGroup.getMembers()) {

            if (member.getPermission() > 0) continue; // 管理员

            // 跳过有头衔的
            final String specialTitle = member.getSpecialTitle();
            if (specialTitle != null && !specialTitle.equals("")) continue;


            final MiraiNormalMember member1 = QqGroupManager.findMember(group, member.getId());
            if (member1 == null) continue; // 没有进入


            list.add(new MemberInfo(
                    member.getId(),
                    member.getNick(),
                    member.getJoinTimestamp(),
                    member1.getJoinTimestamp()
            ));
        }


        // 按进去主群的时间排序
        list.sort(Comparator.comparingInt(MemberInfo::joinMainTime));

        if (num < 0) return list;

        final LinkedList<MemberInfo> list2 = new LinkedList<>();
        int c = 0;
        for (final MemberInfo memberInfo : list) {
            if (c < num) {
                list2.add(memberInfo);
                ++c;
            } else break;
        }

        return list2;
    }

    @Override
    public void starkKick(@NotNull List<MemberInfo> list, @NotNull OnKickFinished onKickFinished) throws Exception {

        final MiraiGroup auditGroup = this.plugin.findAuditGroup();

        if (auditGroup == null) throw new Exception("无法访问审核群！");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            final LinkedList<MemberInfo> kicked = new LinkedList<>();
            final LinkedList<MemberInfo> notKick = new LinkedList<>();

            for (final MemberInfo member : list) {
                final MiraiNormalMember member1 = QqGroupManager.findMember(auditGroup, member.qq());
                if (member1 != null) {
                    try {
                        member1.doKick("");
                        kicked.add(member);
                        Thread.sleep(4000L);
                    } catch (Exception e) {
                        notKick.add(member);
                        e.printStackTrace();
                    }
                } else {
                    notKick.add(member);
                }
            }

            onKickFinished.on(kicked, notKick);
        });
    }


    @Override
    public boolean handleMessage(@NotNull MiraiGroupMessageEvent event) {

        if (event.getSenderPermission() < 1) return false; // 不是管理员

        final String message = event.getMessage();
        if (message == null) return false; // 空消息

        final String[] args = message.split(" ");
        if (args.length < 1) return false; // 内容

        final KickListAuditCmd cmd = new KickListAuditCmd(plugin, event);

        if (!args[0].equals(cmd.getLabel())) return false;

        // 缩减参数
        final String[] args2 = new String[args.length - 1];
        System.arraycopy(args, 1, args2, 0, args2.length);

        // 执行命令
        cmd.execute(args2);

        return true;
    }
}
