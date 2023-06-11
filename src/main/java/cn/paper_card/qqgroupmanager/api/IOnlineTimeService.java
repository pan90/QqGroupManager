package cn.paper_card.qqgroupmanager.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IOnlineTimeService {

    void init();

    void destroy();

    @NotNull Storage getStorage();

    interface Storage {

        record Record(UUID uuid, long time) {
        }

        // 查询一个玩家的在线时间（单位毫秒）
        @Nullable Long query(@NotNull UUID uuid) throws Exception;

        @NotNull List<Record> queryAll() throws Exception;

        // 增加一个玩家的在线时间
        void addTime(@NotNull UUID uuid, long time) throws Exception;
    }
}
