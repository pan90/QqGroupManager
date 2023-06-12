package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IQqBlackListService;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.jetbrains.annotations.NotNull;

class QqBlackListCmd extends TheCommand.HasSub {

    static final String LABEL = "QQ黑名单";

    private final @NotNull QqGroupManager plugin;

    QqBlackListCmd(@NotNull QqGroupManager plugin) {
        super(LABEL);
        this.plugin = plugin;

        addSubCmd(new Add());
        addSubCmd(new Remove());
        addSubCmd(new Query());
    }

    class Add extends TheCommand {

        Add() {
            super("添加");
        }

        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {

            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            final long qq;

            try {
                qq = parseQqArg(args, sender);
            } catch (Exception ignored) {
                return;
            }

            final String argReason;
            if (args.length > 1) argReason = args[1];
            else argReason = null;


            // 查询昵称
            final MiraiNormalMember member = QqGroupManager.findMember(event.getGroup(), qq);
            final String nick = member != null ? member.getNick() : null;

            final IQqBlackListService service = plugin.getBlackListService();

            try {
                service.add(new IQqBlackListService.Info(
                        qq,
                        nick,
                        argReason,
                        System.currentTimeMillis()
                ));
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(e.toString());
            }

            sender.sendMessage("添加成功。");
        }
    }

    class Remove extends TheCommand {

        Remove() {
            super("移除");
        }


        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {

            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            final long qq;

            try {
                qq = parseQqArg(args, sender);
            } catch (Exception ignored) {
                return;
            }

            final IQqBlackListService service = plugin.getBlackListService();

            final IQqBlackListService.Info info;
            try { // 查询
                info = service.queryByQq(qq);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(e.toString());
                return;
            }

            if (info == null) {
                sender.sendMessage("%d不在黑名单之中！".formatted(qq));
                return;
            }

            try {
                service.remove(qq);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(e.toString());
                return;
            }

            sender.sendMessage("移除成功：\nQQ号码：%d\n昵称：%s\n原因：%s\n时间：%d".formatted(
                    info.qq(), info.nick(), info.reason(), info.time()
            ));
        }
    }


    class Query extends TheCommand {

        Query() {
            super("查询");
        }

        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {
            final Utility.AtMessageSender sender = new Utility.AtMessageSender(event.getGroup(), event.getSenderID());

            final long qq;

            try {
                qq = parseQqArg(args, sender);
            } catch (Exception ignored) {
                return;
            }

            final IQqBlackListService.Info info;

            try {
                info = plugin.getBlackListService().queryByQq(qq);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(e.toString());
                return;
            }

            if (info == null) {
                sender.sendMessage("该QQ[%d]没有添加黑名单！".formatted(qq));
                return;
            }

            sender.sendMessage("黑名单信息：\nQQ：%d\n昵称：%s\n原因：%s\n时间：%d".formatted(
                    info.qq(), info.nick(), info.reason(), info.time()
            ));
        }
    }

    long parseQqArg(@NotNull String[] args, @NotNull Utility.AtMessageSender sender)
            throws Exception {

        String argQq = null;

        if (args.length > 0) argQq = args[0];

        if (argQq == null) {
            sender.sendMessage("你必须指定一个QQ号码！");
            throw new Exception();
        }

        final Long qq = plugin.getUtility().parseQqId(argQq);

        if (qq == null) {
            sender.sendMessage("%s 不包含QQ号码！".formatted(argQq));
            throw new Exception();
        }
        return qq;
    }
}
