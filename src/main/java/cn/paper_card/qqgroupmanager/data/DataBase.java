package cn.paper_card.qqgroupmanager.data;

import cn.paper_card.papercardauth.PaperCardAuth;
import cn.paper_card.papercardauth.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// todo: 复制来的，还没修改
public class DataBase {

    private final Connection connection; // 数据库连接


    private final PaperCardAuth plugin;

    private BanRiXianMojangUuidBindTable banRiXianMojangUuidBindTable = null;

    private PlayerLastLoginTable playerLastLoginTable = null;

    private UuidQqBindTable uuidQqBindTable = null;

    private ChineseNameTable chineseNameTable = null;

    private LittleSkinUuidBindTable littleSkinUuidBindTable = null;

    public DataBase(@NotNull PaperCardAuth plugin) throws SQLException, ClassNotFoundException {
        this.plugin = plugin;
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

    public @Nullable LittleSkinUuidBindTable getLittleSkinUuidBindTable() throws SQLException {
        if (this.littleSkinUuidBindTable == null) {
            this.littleSkinUuidBindTable = new LittleSkinUuidBindTable(this.connection);
        }
        return this.littleSkinUuidBindTable;
    }

    private void closeLittleSkinBindTable() {
        if (this.littleSkinUuidBindTable != null) {
            this.littleSkinUuidBindTable.close();
            this.littleSkinUuidBindTable = null;
        }
    }

    @Nullable
    public BanRiXianMojangUuidBindTable getBanRiXianMojangUuidBindTable() {
        if (this.banRiXianMojangUuidBindTable == null) {
            try {
                this.banRiXianMojangUuidBindTable = new BanRiXianMojangUuidBindTable(this.getConnection());
            } catch (SQLException e) {
                plugin.getLogger().severe("创建数据表" + BanRiXianMojangUuidBindTable.TABLE_NAME + "时发生异常：" + e.getLocalizedMessage());
            }
        }
        return this.banRiXianMojangUuidBindTable;
    }


    private void closeBanRiXianMojangUuidBindTable() {
        if (this.banRiXianMojangUuidBindTable != null) {
            this.banRiXianMojangUuidBindTable.close();
            this.banRiXianMojangUuidBindTable = null;
        }
    }

    @Nullable
    public PlayerLastLoginTable getPlayerLastLoginTable() {
        if (this.playerLastLoginTable == null) {
            try {
                this.playerLastLoginTable = new PlayerLastLoginTable(this.getConnection());
            } catch (SQLException e) {
                plugin.getLogger().severe("创建数据表" + PlayerLastLoginTable.TABLE_NAME + "时发生异常：" + e);
            }
        }
        return this.playerLastLoginTable;
    }

    private void closePlayerLastLoginTable() {
        if (this.playerLastLoginTable != null) {
            this.playerLastLoginTable.closeTable();
            this.playerLastLoginTable = null;
        }
    }

    public UuidQqBindTable getUuidQqBindTable() {
        if (this.uuidQqBindTable == null) {
            try {
                this.uuidQqBindTable = new UuidQqBindTable(this.getConnection());
            } catch (SQLException e) {
                plugin.getLogger().severe("创建数据表" + UuidQqBindTable.TABLE_NAME + "时发生异常：" + e);
            }
        }
        return this.uuidQqBindTable;
    }

    private void closeUuidQqBindTable() {
        if (this.uuidQqBindTable != null) {
            this.uuidQqBindTable.close();
            this.uuidQqBindTable = null;
        }
    }

    @Nullable
    public ChineseNameTable getChineseNameTable() {
        if (this.chineseNameTable == null) {
            try {
                this.chineseNameTable = new ChineseNameTable(this.connection);
            } catch (SQLException e) {
                this.plugin.getLogger().severe("创建数据表" + ChineseNameTable.TABLE_NAME + "时发生异常：" + e);
            }
        }
        return this.chineseNameTable;
    }

    private void closeChineseNameTable() {
        if (this.chineseNameTable != null) {
            this.chineseNameTable.close();
            this.chineseNameTable = null;
        }
    }

    public Connection getConnection() {
        return this.connection;
    }


    public void close() {

        this.closeBanRiXianMojangUuidBindTable();
        this.closePlayerLastLoginTable();
        this.closeUuidQqBindTable();
        this.closeChineseNameTable();
        this.closeLittleSkinBindTable();

        try {
            this.connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("关闭数据库连接时异常：" + e);
        }
    }
}
