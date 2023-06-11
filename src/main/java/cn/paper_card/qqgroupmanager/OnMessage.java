package cn.paper_card.qqgroupmanager;

import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

class OnMessage implements Listener {

    private final @NotNull QqGroupManager plugin;

    public OnMessage(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(@NotNull MiraiGroupMessageEvent event) {

        final long groupID = event.getGroupID();

        // 主群
        if (groupID == this.plugin.getQqGroupId()) {
            if (plugin.getAutoKick().handleMessage(event)) return;
        }

        // 审核群
        if (groupID == this.plugin.getAuditQqGroupID()) {
            if (this.plugin.getKickListAuditGroup().handleMessage(event)) return;
        }

        this.doNothing();
    }

    private void doNothing() {
    }
}
