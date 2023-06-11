package cn.paper_card.qqgroupmanager;

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

        if (groupID == this.plugin.getAuditQqGroupID()) {
            group.sendMessageMirai("[mirai:at:%d] 欢迎入群，请查看【群公告】填写【审核问卷】~\n有疑问可以查看相关的群公告。"
                    .formatted(event.getNewMemberID()));
        }

        if (groupID == this.plugin.getQqGroupId()) {
            group.sendMessageMirai("[mirai:at:%d] 欢迎新伙伴入裙~\n请查看群公告【新人必看】~".formatted(event.getNewMemberID()));


            final MiraiGroup auditGroup = plugin.findAuditGroup();
            if (auditGroup != null) {
                final MiraiNormalMember member = QqGroupManager.findMember(auditGroup, event.getNewMemberID());
                if (member != null) {
                    auditGroup.sendMessageMirai("[mirai:at:%d] 你已经通过审核进入主群，现在可退出审核群。".formatted(event.getNewMemberID()));
                }
            }
        }
    }
}
