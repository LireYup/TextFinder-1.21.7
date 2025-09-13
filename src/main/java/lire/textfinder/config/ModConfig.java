package lire.textfinder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lire.textfinder.TextFinder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "textfinder.json");

    // 配置项定义
    private int maxsearchamountpertick = 100;
    private boolean withbaritone = false;
    private boolean withclientcommands = false;
    private int outputcomplexity = 2; // 1=simple, 2=normal, 3=complex, 4=debug
    private int debugpgt = 4;
    private int cglowtime = 60;
    private String cglowcolor = "white";
    private boolean firstlaunch = false; // 首次启动标识
    private int searchrange = 12; // 搜索范围

    public static ModConfig load() {
        ModConfig config = new ModConfig();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);

                // 首次启动或配置异常时重置为默认值
                if (config.firstlaunch) {
                    config = new ModConfig();
                    config.firstlaunch = false;
                    config.save();
                }
            } catch (IOException e) {
                TextFinder.LOGGER.error("加载配置文件失败", e);
                config = new ModConfig();
                config.save();
            }
        } else {
            config.save();
        }

        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            TextFinder.LOGGER.error("保存配置文件失败", e);
        }
    }

    // 最大每tick搜索数量
    public int getmaxsearchamountpertick() {
        return Math.max(1, Math.min(255, maxsearchamountpertick));
    }

    public void setmaxsearchamountpertick(int value) {
        this.maxsearchamountpertick = Math.max(1, Math.min(255, value));
    }

    // Baritone集成开关
    public boolean iswithbaritone() {
        return withbaritone;
    }

    public void setwithbaritone(boolean withbaritone) {
        this.withbaritone = withbaritone;
    }

    // 客户端指令集成开关
    public boolean iswithclientcommands() {
        return withclientcommands;
    }

    public void setwithclientcommands(boolean withclientcommands) {
        this.withclientcommands = withclientcommands;
    }

    // 输出复杂度
    public String getoutputcomplexity() {
        return Math.max(1, Math.min(4, outputcomplexity));
    }

    public void setoutputcomplexity(int outputcomplexity) {
        this.outputcomplexity = Math.max(1, Math.min(4, outputcomplexity));
    }

    // 调试信息间隔
    public int getdebugpgt() {
        return Math.max(1, Math.min(255, debugpgt));
    }

    public void setdebugpgt(int debugpgt) {
        this.debugpgt = Math.max(1, Math.min(255, debugpgt));
    }

    // 发光时间
    public int getcglowtime() {
        return cglowtime;
    }

    public void setcglowtime(int cglowtime) {
        this.cglowtime = cglowtime;
    }

    // 发光颜色
    public String getcglowcolor() {
        return cglowcolor;
    }

    public void setcglowcolor(String cglowcolor) {
        this.cglowcolor = cglowcolor;
    }

    // 首次启动标识
    public boolean isfirstlaunch() {
        return firstlaunch;
    }

    public void setfirstlaunch(boolean firstlaunch) {
        this.firstlaunch = firstlaunch;
    }

    // 搜索范围
    public int getsearchrange() {
        return Math.max(1, Math.min(32, searchrange));
    }

    public void setsearchrange(int searchrange) {
        this.searchrange = Math.max(1, Math.min(32, searchrange));
    }
}