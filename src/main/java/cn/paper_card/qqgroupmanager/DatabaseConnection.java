package cn.paper_card.qqgroupmanager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


class DatabaseConnection {

    private final @NotNull Connection connection; // 数据库连接

    DatabaseConnection(@NotNull QqGroupManager plugin) throws Exception {

        // 数据库文件
        final File file = new File(plugin.getDataFolder(), "%s.db".formatted(plugin.getName()));
        final File parentFile = file.getParentFile();
        if (!parentFile.isDirectory()) {
            if (!parentFile.mkdir()) {
                throw new Exception("创建父目录[%s]失败！".formatted(parentFile.getAbsolutePath()));
            }
        }

        // 驱动类
        Class.forName("org.sqlite.JDBC");

        // 数据库连接
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    @NotNull Connection getConnection() {
        return this.connection;
    }

    void close() throws SQLException {
        this.connection.close();
    }
}
