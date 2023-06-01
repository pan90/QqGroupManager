package cn.paper_card.qqgroupmanager.data;

import cn.paper_card.qqgroupmanager.QqGroupManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DataBase {

    private final Connection connection; // 数据库连接

    private PlayerOnlineTimeTable playerOnlineTimeTable = null;


    public DataBase(@NotNull QqGroupManager plugin) throws SQLException, ClassNotFoundException {

        // 数据库文件
        final File file = new File(plugin.getDataFolder(), "%s.db".formatted(plugin.getName()));
        final File parentFile = file.getParentFile();
        if (!parentFile.isDirectory() && parentFile.mkdir()) {

        }

        // 驱动类
        Class.forName("org.sqlite.JDBC");

        // 数据库连接
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    public void close() throws SQLException {

        SQLException exception = null;
        if (this.playerOnlineTimeTable != null) {
            try {
                this.playerOnlineTimeTable.close();
            } catch (SQLException e) {
                exception = e;
            }
            this.playerOnlineTimeTable = null;
        }

        if (exception != null) throw exception;
    }

    public @NotNull PlayerOnlineTimeTable getPlayerOnlineTimeTable() throws SQLException {
        if (this.playerOnlineTimeTable == null) {
            this.playerOnlineTimeTable = new PlayerOnlineTimeTable(this.connection);
        }
        return this.playerOnlineTimeTable;
    }
}
