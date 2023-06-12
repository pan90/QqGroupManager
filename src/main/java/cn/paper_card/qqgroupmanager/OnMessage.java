package cn.paper_card.qqgroupmanager;

import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

class OnMessage implements Listener {

    private final @NotNull QqGroupManager plugin;

    private final @NotNull QqBlackListCmd qqBlackListCmd;
    private final @NotNull KickListAuditCmd kickListAuditCmd;
    private final @NotNull KickListCmd kickListCmd;

    public OnMessage(@NotNull QqGroupManager plugin) {
        this.plugin = plugin;
        this.qqBlackListCmd = new QqBlackListCmd(plugin);
        this.kickListAuditCmd = new KickListAuditCmd(plugin);
        this.kickListCmd = new KickListCmd(plugin);
    }

    private static boolean checkExeCmd(@NotNull TheCommand cmd, @NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {
        if (cmd.getLabel().equals(args[0])) {
            final String[] args2 = new String[args.length - 1];
            System.arraycopy(args, 1, args2, 0, args2.length);
            cmd.execute(args2, event);
            return true;
        }
        return false;
    }

    @EventHandler
    public void on(@NotNull MiraiGroupMessageEvent event) {

        final long groupID = event.getGroupID();

        final String message = event.getMessage();
        if (message == null) return;

        // 主群
        if (groupID == this.plugin.getQqGroupId()) {

            if (event.getSenderPermission() < 1) return;

            final String[] args = message.split(" ");

            if (args.length == 0) return;

            if (checkExeCmd(this.kickListCmd, args, event)) return;
            if (checkExeCmd(this.qqBlackListCmd, args, event)) return;

            return;
        }

        // 审核群
        if (groupID == this.plugin.getAuditQqGroupID()) {

            if (event.getSenderPermission() < 1) return;

            final String[] args = message.split(" ");

            if (args.length == 0) return;

            if (checkExeCmd(this.kickListAuditCmd, args, event)) return;

            return;
        }

        this.doNothing();
    }

    private void doNothing() {
    }
}
