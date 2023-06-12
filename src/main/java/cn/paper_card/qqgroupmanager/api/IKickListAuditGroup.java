package cn.paper_card.qqgroupmanager.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IKickListAuditGroup {

    record MemberInfo(
            long qq,
            String nick,
            int joinTime,
            int joinMainTime
    ) {
    }

    interface Session {

        void setKickList(@Nullable List<MemberInfo> list);

        @Nullable List<MemberInfo> getKickList();
    }

    interface OnKickFinished {
        void on(@NotNull List<MemberInfo> kicked, @NotNull List<MemberInfo> notKick);
    }


    interface SessionManager {
        @NotNull Session getSession(long qq);
    }

    @NotNull SessionManager getSessionManager();

    // 生成一个踢出名单，按照入群时间进行排序
    // 名单中不包括管理和有自定义头衔的
    @NotNull List<MemberInfo> createListSortByJoinTime(int num) throws Exception;

    // 生成一个踢出名单，按照进入主群的时间进行排序
    // 名单中不包括管理和有自定义头衔的
    @NotNull List<MemberInfo> createListHasJoinGroup(int num) throws Exception;

    void starkKick(@NotNull List<MemberInfo> list, @NotNull OnKickFinished onKickFinished) throws Exception;

}
