package cn.paper_card.qqgroupmanager.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// QQ黑名单
public interface IQqBlackListService {
    record Info(
            long qq, // QQ号码
            String nick, // QQ昵称
            String reason, // 添加原因
            long time // 添加时间
    ) {
    }

    // 添加一个黑名单，时间如果为负数，那么函数将自动填充时间
    // 如果该QQ已经添加，应该会导致异常
    void add(@NotNull Info info) throws Exception;

    // 移除一个黑名单，根据QQ号码，如果没有添加到黑名单中，不会导致异常
    void remove(long qq) throws Exception;

    // 根据QQ号码查询黑名单信息，如果没有则返回null
    @Nullable Info queryByQq(long qq) throws Exception;

    void init();

    void destroy();
}
