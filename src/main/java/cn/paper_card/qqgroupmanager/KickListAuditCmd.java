package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IKickListAuditGroup;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class KickListAuditCmd extends TheCommand.HasSub {

    private final @NotNull QqGroupManager plugin;

    KickListAuditCmd(@NotNull QqGroupManager plugin) {
        super("踢出名单");
        this.plugin = plugin;

        addSubCmd(new Create());
        addSubCmd(new View());
        addSubCmd(new Remove());
        addSubCmd(new Confirm());

    }


    private void noNotFound0(@NotNull TheCommand.HasSub cmd, @NotNull MiraiGroupMessageEvent event) {
        final StringBuilder builder = new StringBuilder("当前可用的子命令：\n");

        for (final String s : cmd.subCommands.keySet()) {
            builder.append(s);
            builder.append('\n');
        }

        final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());
        sender.sendMessage(builder.toString());
    }

    @Override
    void onNotFound(String subCmd, @NotNull MiraiGroupMessageEvent event) {
        noNotFound0(this, event);
    }

    class Create extends TheCommand.HasSub {

        Create() {
            super("生成");

            this.addSubCmd(new ByHasJoinMain());
            this.addSubCmd(new ByJoinTime());
        }

        @Override
        void onNotFound(String subCmd, @NotNull MiraiGroupMessageEvent event) {
            noNotFound0(this, event);
        }

        int parseNumArg(@NotNull String[] args, @NotNull Utility.AtMessageSender sender) throws Exception {
            String argNum = null;
            if (args.length > 0) argNum = args[0];

            if (argNum == null) return -1;

            final int num;

            try {
                num = Integer.parseInt(argNum);
            } catch (NumberFormatException e) {
                sender.sendMessage("%s 不是一个数字！".formatted(argNum));
                throw new Exception(e);
            }
            return num;
        }

        private interface GetList {
            List<IKickListAuditGroup.MemberInfo> get(int num) throws Exception;
        }

        private void onCreate(@NotNull String[] args, @NotNull GetList get, @NotNull MiraiGroupMessageEvent event) {

            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            final int num;

            try {
                num = parseNumArg(args, sender);
            } catch (Exception ignored) {
                return;
            }

            final List<IKickListAuditGroup.MemberInfo> list;

            try {
                list = get.get(num);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(e.toString());
                return;
            }

            // 保存到会话中
            final IKickListAuditGroup kick = plugin.getKickListAuditGroup();
            final IKickListAuditGroup.Session session = kick.getSessionManager().getSession(event.getSenderID());
            session.setKickList(list);

            sender.sendMessage("生成成功，人数：%d".formatted(list.size()));

        }

        class ByJoinTime extends TheCommand {

            ByJoinTime() {
                super("按入群时间");
            }

            @Override
            void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {
                onCreate(args, num -> plugin.getKickListAuditGroup().createListSortByJoinTime(num), event);
            }
        }

        class ByHasJoinMain extends TheCommand {

            ByHasJoinMain() {
                super("已进入主群");
            }

            @Override
            void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {
                onCreate(args, num -> plugin.getKickListAuditGroup().createListHasJoinGroup(num), event);
            }
        }
    }

    class View extends TheCommand {

        View() {
            super("查看");
        }

        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {

            final IKickListAuditGroup kick = plugin.getKickListAuditGroup();
            final IKickListAuditGroup.Session session = kick.getSessionManager().getSession(event.getSenderID());

            final List<IKickListAuditGroup.MemberInfo> list = session.getKickList();

            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            if (list == null) {
                sender.sendMessage("没有生成名单！");
                return;
            }

            StringBuilder builder = new StringBuilder();
            builder.append("序号 | QQ | 昵称 | 入群天数 | 进入主群天数\n");
            int i = 1;

            final MiraiGroup group = event.getGroup();

            for (final IKickListAuditGroup.MemberInfo member : list) {

                final double days = (double) (System.currentTimeMillis() / 1000L - member.joinTime()) /
                        (60 * 60 * 24);

                final String joinMainDays;
                if (member.joinMainTime() > 0) {
                    final double days2 = (double) (System.currentTimeMillis() / 1000L - member.joinMainTime()) /
                            (60 * 60 * 24);

                    joinMainDays = "%.2f".formatted(days2);
                } else if (member.joinMainTime() == 0) {
                    joinMainDays = "未进入";
                } else {
                    joinMainDays = "不确定";
                }

                builder.append("%d | %d | %s | %.2f | %s \n".formatted(
                        i, member.qq(), member.nick(), days, joinMainDays
                ));

                if (i % 16 == 0) {
                    group.sendMessage(builder.toString());
                    builder = new StringBuilder();
                }
                ++i;
            }

            final String msg = builder.toString();
            if (!msg.isEmpty()) {
                group.sendMessage(msg);
            }
        }
    }

    class Confirm extends TheCommand {

        Confirm() {
            super("确认踢出");
        }

        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {

            final IKickListAuditGroup kick = plugin.getKickListAuditGroup();
            final IKickListAuditGroup.Session session = kick.getSessionManager().getSession(event.getSenderID());

            final List<IKickListAuditGroup.MemberInfo> kickList = session.getKickList();

            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            if (kickList == null) {
                sender.sendMessage("没有生成名单！");
                return;
            }

            try {

                kick.starkKick(kickList, (kicked, notKick) ->
                        sender.sendMessage("踢出成功：%d，踢出失败：%d".formatted(kicked.size(), notKick.size())));

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(e.toString());
                return;
            }

            sender.sendMessage("已经启动踢人任务，请等待任务完成。");
        }
    }

    class Remove extends TheCommand {

        Remove() {
            super("移除");
        }

        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {
            // 踢出名单 移除 <QQ>
            String argQq = null;

            if (args.length > 0) argQq = args[0];

            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            if (argQq == null) {
                sender.sendMessage("你必须指定一个QQ号码！");
                return;
            }


            final Long qq = plugin.getUtility().parseQqId(argQq);

            if (qq == null) {
                sender.sendMessage("%s 中不包含QQ号码！".formatted(argQq));
                return;
            }

            final IKickListAuditGroup kick = plugin.getKickListAuditGroup();

            final IKickListAuditGroup.Session session = kick.getSessionManager().getSession(event.getSenderID());
            final List<IKickListAuditGroup.MemberInfo> list = session.getKickList();

            if (list == null) {
                sender.sendMessage("名单没有生成！");
                return;
            }


            final int oldSize = list.size();

            list.removeIf(member -> member.qq() == qq);

            final int newSize = list.size();

            sender.sendMessage("名单人数变化：%d => %d".formatted(oldSize, newSize));
        }
    }
}
