package cn.paper_card.qqgroupmanager.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IQqGroupManager {

    // 发送消息到主群中
    void sendMessageToGroupLater(@NotNull String message);

    // 发送消息到审核群中
    void sendMessageToAuditGroupLater(@NotNull String message);

    // 在大群中根据QQ号查找群成员
    @Nullable IGroupMemberInfo findGroupMember(long qq);

    @Nullable String queryMemberRemark(long qq);

    // 消息监听
    interface MessageEvent {
        void addGroupMessageListener(@NotNull IGroupMessageListener listener);

        void removeGroupMessageListener(@NotNull IGroupMessageListener listener);

        void addAuditGroupMessageListener(@NotNull IGroupMessageListener listener);

        void removeAuditGroupMessageListener(@NotNull IGroupMessageListener listener);
    }

    interface JoinEvent {
        // 进群监听
        void addMemberJoinGroupListener();

        void removeMemberJoinGroupListener();

        void addMemberJoinAuditGroupListener();

        void removeMemberJoinAuditGroupListener();
    }

    interface QuitEvent {
        // 退出监听
        void addMemberQuitGroupListener();

        void removeMemberQuitGroupListener();

        void addMemberQuitAuditGroupListener();

        void removeMemberQuitAuditGroupListener();
    }
}
