package cn.paper_card.qqgroupmanager;

import me.dreamvoid.miraimc.bukkit.event.message.passive.MiraiGroupMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

abstract class TheCommand {
    private final @NotNull String label;

    TheCommand(@NotNull String label) {
        this.label = label;
    }

    @NotNull String getLabel() {
        return this.label;
    }

    abstract void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event);


    static class HasSub extends TheCommand {

        protected final @NotNull HashMap<String, TheCommand> subCommands;

        HasSub(@NotNull String label) {
            super(label);
            this.subCommands = new HashMap<>();
        }

        void addSubCmd(@NotNull TheCommand cmd) {
            this.subCommands.put(cmd.getLabel(), cmd);
        }

        void onNotFound(@Nullable String subCmd, @NotNull MiraiGroupMessageEvent event) {
        }

        @Override
        void execute(@NotNull String[] args, @NotNull MiraiGroupMessageEvent event) {
            if (args.length > 0) {
                final String label = args[0];
                final TheCommand tc = this.subCommands.get(label);
                if (tc != null) {
                    final String[] args2 = new String[args.length - 1];
                    System.arraycopy(args, 1, args2, 0, args2.length);
                    tc.execute(args2, event);
                } else {
                    this.onNotFound(label, event);
                }
            } else {
                this.onNotFound(null, event);
            }
        }
    }
}
