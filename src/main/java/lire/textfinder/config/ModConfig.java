package lire.textfinder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lire.textfinder.TextFinder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

// 修正类名引用问题，确保被其他类使用
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "textfinder.json");

    // 变量名改为全小写
    private int maxsearchamountpertick = 100;
    private boolean withbaritone = false;
    private boolean withclientcommands = false;
    private String outputcomplexity = "Normal";
    private int debugpgt = 4;
    private int cglowtime = 60;
    private String cglowcolor = "white";

    public static ModConfig load() {
        ModConfig config = new ModConfig();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                // 替换printStackTrace为日志输出
                TextFinder.LOGGER.error("加载配置文件失败", e);
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
            // 替换printStackTrace为日志输出
            TextFinder.LOGGER.error("保存配置文件失败", e);
        }
    }

    // Getters and Setters改为全小写方法名
    public int getmaxsearchamountpertick() {
        return Math.max(1, Math.min(255, maxsearchamountpertick));
    }

    public void setmaxsearchamountpertick(int value) {
        this.maxsearchamountpertick = Math.max(1, Math.min(255, value));
    }

    public boolean iswithbaritone() {
        return withbaritone;
    }

    public void setwithbaritone(boolean withbaritone) {
        this.withbaritone = withbaritone;
    }

    public boolean iswithclientcommands() {
        return withclientcommands;
    }

    public void setwithclientcommands(boolean withclientcommands) {
        this.withclientcommands = withclientcommands;
    }

    public String getoutputcomplexity() {
        if (!outputcomplexity.equals("Simple") && !outputcomplexity.equals("Normal") && !outputcomplexity.equals("Debug")) {
            return "Normal";
        }
        return outputcomplexity;
    }

    public void setoutputcomplexity(String outputcomplexity) {
        this.outputcomplexity = outputcomplexity;
    }

    public int getdebugpgt() {
        return Math.max(1, Math.min(255, debugpgt));
    }

    public void setdebugpgt(int debugpgt) {
        this.debugpgt = Math.max(1, Math.min(255, debugpgt));
    }

    public int getcglowtime() {
        return cglowtime;
    }

    public void setcglowtime(int cglowtime) {
        this.cglowtime = cglowtime;
    }

    public String getcglowcolor() {
        return cglowcolor;
    }

    public void setcglowcolor(String cglowcolor) {
        this.cglowcolor = cglowcolor;
    }
}