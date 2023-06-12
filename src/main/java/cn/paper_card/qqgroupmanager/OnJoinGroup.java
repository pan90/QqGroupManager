package cn.paper_card.qqgroupmanager;

import cn.paper_card.qqgroupmanager.api.IQqBlackListService;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.api.bot.group.MiraiNormalMember;
import me.dreamvoid.miraimc.bukkit.event.group.member.MiraiMemberJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

class OnJoinGroup implements Listener {

    private final @NotNull QqGroupManager plugin;

    OnJoinGroup(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(@NotNull MiraiMemberJoinEvent event) {
        final long groupID = event.getGroupID();

        final MiraiGroup group = event.getGroup();
        final Utility.AtMessageSender sender = new Utility.AtMessageSender(group, event.getNewMemberID());

        // 进入审核群
        if (groupID == this.plugin.getAuditQqGroupID()) {


            // 查询是否黑名单
            final IQqBlackListService.Info info;

            try {
                info = plugin.getBlackListService().queryByQq(event.getNewMemberID());
            } catch (Exception e) {
                e.printStackTrace();
                group.sendMessage(e.toString());
                return;
            }

            if (info != null) { // 黑名单
                sender.sendMessage("黑名单警告！原因：%s".formatted(info.reason()));
                return;
            }

            sender.sendMessage("欢迎入群，请查看【群公告】填写【审核问卷】~\n有疑问可以查看相关的群公告。");

            return;
        }

        if (groupID == this.plugin.getQqGroupId()) { // 进入主群

            // 查询黑名单
            final IQqBlackListService.Info info;

            try {
                info = plugin.getBlackListService().queryByQq(event.getNewMemberID());
            } catch (Exception e) {
                e.printStackTrace();
                group.sendMessage(e.toString());
                return;
            }


            // 是黑名单
            if (info != null) {
                sender.sendMessage("黑名单警告！原因：%s".formatted(info.reason()));
                return;
            }

            sender.sendMessage("欢迎新伙伴入裙~\n请查看群公告【新人必看】\n祝您游戏愉快~");

            final MiraiGroup auditGroup = plugin.findAuditGroup();
            if (auditGroup != null) {
                final MiraiNormalMember member = QqGroupManager.findMember(auditGroup, event.getNewMemberID());
                if (member != null) { // 还在审核群中，提醒他退出
                    Utility.sendAtMessage(auditGroup, member.getId(), "恭喜你已经通过审核进入主群，现在可以退出审核群啦~");
                }
            }
        }
    }
}
