package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IKickList;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

class KickListCmd extends TheCommand.HasSub {


    static class SessionManager {
        static class Session {
            private List<IKickList.Info> list = null;

            void setList(@Nullable List<IKickList.Info> list) {
                this.list = list;
            }

            @Nullable List<IKickList.Info> getList() {
                return this.list;
            }

        }

        private final HashMap<Long, Session> map;

        SessionManager() {
            this.map = new HashMap<>();
        }

        @NotNull Session getSession(long qq) {
            synchronized (this) {
                Session session = this.map.get(qq);
                if (session != null) return session;

                session = new Session();
                this.map.put(qq, session);
                return session;
            }
        }
    }


    private class Create extends TheCommand.HasSub {

        private int parseNumArg(String[] args) throws Exception {
            String argNum = null;
            if (args.length > 0) {
                argNum = args[0];
            }

            final int num;

            if (argNum != null) {
                try {
                    num = Integer.parseInt(argNum);
                } catch (NumberFormatException e) {
                    sendAtMsg("%s 不是一个数字！".formatted(argNum));
                    throw new Exception(e);
                }
            } else num = -1;

            return num;
        }

        private class NotBind extends TheCommand {

            NotBind() {
                super("未绑定");
            }

            @Override
            void execute(@NotNull String[] args) {

                final int num;

                try {
                    num = parseNumArg(args);
                } catch (Exception ignored) {
                    return;
                }


                final List<MiraiNormalMember> notBindList;

                try {
                    notBindList = plugin.getAutoKick().createNotBindList(event.getGroup(), num);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendAtMsg("异常：" + e);
                    return;
                }

                final List<IKickList.Info> is = plugin.getAutoKick().transformNotBindList(notBindList);

                final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());

                session.setList(is);

                sendAtMsg("生成成功，人数：%d".formatted(is.size()));
            }
        }

        private class OneDayPlay extends TheCommand {

            OneDayPlay() {
                super("一日游");
            }

            @Override
            void execute(@NotNull String[] args) {
                final int num;

                try {
                    num = parseNumArg(args);
                } catch (Exception e) {
                    return;
                }

                final List<IKickList.OneDayPlayerMember> list;
                final IKickList kick = plugin.getAutoKick();

                try {
                    list = kick.createPlayOneDayList(event.getGroup(), num);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendAtMsg("异常：" + e);
                    return;
                }

                final List<IKickList.Info> is = kick.transformOneDayPlayList(list);

                final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());

                session.setList(is);

                sendAtMsg("生成成功，人数：%d".formatted(is.size()));
            }
        }

        private class NotEnoughTime extends TheCommand {

            NotEnoughTime() {
                super("在线时长不足");
            }

            @Override
            void execute(@NotNull String[] args) {
                final int num;

                try {
                    num = parseNumArg(args);
                } catch (Exception e) {
                    return;
                }

                final List<IKickList.NotEnoughTimeMember> list;
                final IKickList kick = plugin.getAutoKick();

                try {
                    list = kick.createNotEnoughTimeList(event.getGroup(), num, 2L * 60L * 60L * 1000L);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendAtMsg("异常：" + e);
                    return;
                }

                final List<IKickList.Info> is = kick.transformNotEnoughTimeList(list);

                final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());

                session.setList(is);

                sendAtMsg("生成成功，人数：%d".formatted(is.size()));
            }
        }

        private class All extends TheCommand {

            All() {
                super("所有");
            }

            @Override
            void execute(@NotNull String[] args) {
                final int num;

                try {
                    num = parseNumArg(args);
                } catch (Exception ignored) {
                    return;
                }

                final IKickList kick = plugin.getAutoKick();

                final List<IKickList.Info> list;


                try {
                    list = kick.createList(num);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendAtMsg("异常：" + e);
                    return;
                }

                final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());
                session.setList(list);

                sendAtMsg("生成成功，人数：%d".formatted(list.size()));
            }
        }

        Create() {
            super("生成");
            addSubCmd(new NotBind());
            addSubCmd(new OneDayPlay());
            addSubCmd(new NotEnoughTime());
            addSubCmd(new All());
        }

        @Override
        void onNotFound(String subCmd) {
            final StringBuilder builder = new StringBuilder("所有可用的子命令：\n");
            for (String s : this.subCommands.keySet()) {
                builder.append(s);
                builder.append('\n');
            }
            sendAtMsg(builder.toString());
        }
    }

    private class View extends TheCommand {

        View() {
            super("查看");
        }

        private static void sendList(@NotNull List<IKickList.Info> list, @NotNull MiraiGroup group) {

            StringBuilder builder = new StringBuilder();
            builder.append("序号 | QQ | 等级 | 游戏名 | 昵称 | 原因 | 备注 \n");
            int i = 1;
            for (final IKickList.Info info : list) {

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
        }

        @Override
        void execute(@NotNull String[] args) {
            final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());

            final List<IKickList.Info> list = session.getList();

            if (list == null) {
                sendAtMsg("请先生成一个名单！");
                return;
            }

            sendList(list, event.getGroup());
        }
    }

    private class Remove extends TheCommand {

        Remove() {
            super("移除");
        }

        @Override
        void execute(@NotNull String[] args) {
            final String argQq;

            if (args.length > 0) argQq = args[0];
            else argQq = null;

            if (argQq == null) {
                sendAtMsg("必须指定一个QQ参数！");
                return;
            }


            final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());
            final List<IKickList.Info> list = session.getList();

            if (list == null) {
                sendAtMsg("请先生成名单！");
                return;
            }

            final Long qq = plugin.getUtility().parseQqId(argQq);
            if (qq == null) {
                sendAtMsg("%s 不是一个QQ号码！".formatted(argQq));
                return;
            }

            final int oldSize = list.size();

            list.removeIf(info -> info.member().getId() == qq);

            final int newSize = list.size();

            sendAtMsg("名单人数变化：%d => %d".formatted(oldSize, newSize));
        }
    }

    private class Confirm extends TheCommand {

        Confirm() {
            super("确认踢出");
        }

        @Override
        void execute(@NotNull String[] args) {
            final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());
            final List<IKickList.Info> list = session.getList();
            session.setList(null);

            if (list == null) {
                sendAtMsg("请先生成名单！");
                return;
            }

            try {
                plugin.getAutoKick().startKick(list, (kicked, notKick) ->
                        sendAtMsg("踢出成功：%d，踢出失败：%d".formatted(kicked.size(), notKick.size())));

            } catch (Exception e) {
                e.printStackTrace();
                sendAtMsg("异常：" + e);
                return;
            }

            sendAtMsg("已经启动踢人任务，任务完成会通知。");
        }
    }

    private class Notify extends TheCommand {

        Notify() {
            super("通知");
        }

        @Override
        void execute(@NotNull String[] args) {
            final SessionManager.Session session = plugin.getSessionManager().getSession(event.getSenderID());

            final List<IKickList.Info> list = session.getList();

            if (list == null) {
                sendAtMsg("请先生成名单！");
                return;
            }

            final StringBuilder builder = new StringBuilder();
            for (final IKickList.Info info : list) {
                builder.append("[mirai:at:%d] ".formatted(info.member().getId()));
            }

            final String string = builder.toString();

            if (string.isEmpty()) {
                sendAtMsg("没有人要通知！");
                return;
            }

            event.getGroup().sendMessageMirai(string);
        }
    }

    private final @NotNull QqGroupManager plugin;

    private final @NotNull MiraiGroupMessageEvent event;


    private void sendAtMsg(@NotNull String msg) {
        final MiraiGroup group = event.getGroup();
        group.sendMessageMirai("[mirai:at:%d] %s".formatted(event.getSenderID(), msg));
    }

    KickListCmd(@NotNull QqGroupManager plugin, @NotNull MiraiGroupMessageEvent event) {
        super("踢出名单");
        this.plugin = plugin;
        this.event = event;
        addSubCmd(new Create());
        addSubCmd(new View());
        addSubCmd(new Remove());
        addSubCmd(new Confirm());
        addSubCmd(new Notify());
    }

    @Override
    void onNotFound(String subCmd) {
        final StringBuilder builder = new StringBuilder("所有可用的子命令：\n");
        for (String s : this.subCommands.keySet()) {
            builder.append(s);
            builder.append('\n');
        }
        sendAtMsg(builder.toString());
    }
}
