package cn.paper_card.qqgroupmanager;

import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Utility {

    private final @NotNull Pattern compile = Pattern.compile("([1-9][0-9]{4,14})");


    @Nullable Long parseQqId(@NotNull String string) {
        final Matcher matcher = this.compile.matcher(string);
        if (matcher.find()) {
            final String qq_str = matcher.group(1);
            try {
                return Long.parseLong(qq_str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    static void sendAtMessage(@NotNull MiraiGroup group, long qq, @NotNull String message) {
        group.sendMessageMirai("[mirai:at:%d] %s".formatted(qq, message));
    }

    static class AtMessageSender {
        private final @NotNull MiraiGroup group;
        private final long qq;


        AtMessageSender(@NotNull MiraiGroup group, long qq) {
            this.group = group;
            this.qq = qq;
        }

        void sendMessage(@NotNull String message) {
            Utility.sendAtMessage(this.group, this.qq, message);
        }
    }
}
