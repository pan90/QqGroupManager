package cn.paper_card.qqgroupmanager.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 对于主群的管理
public interface IQqGroupManager {

    // 发送消息到主群中
    void sendMessageLater(@NotNull String message);

    void sendAtMessageLater(long qq, @NotNull String message);

    // 在大群中根据QQ号查找群成员

    void init();

    void destroy();


    interface JoinEvent {
        // 进群监听
        void addMemberJoinGroupListener();

        void removeMemberJoinGroupListener();
    }

    interface QuitEvent {
        // 退出监听
        void addMemberQuitGroupListener();

        void removeMemberQuitGroupListener();
    }
}
