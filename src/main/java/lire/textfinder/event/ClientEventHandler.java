package lire.textfinder.event;

import lire.textfinder.TextFinder;
import lire.textfinder.search.SignSearchManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 客户端事件处理器，管理搜索过程和数据清除
 */
public class ClientEventHandler {
    public static void registerEvents() {
        // 世界加载时清除搜索结果
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (MinecraftClient.getInstance().isOnThread()) {
                SignSearchManager.getInstance().clearFoundSigns();
            }
        });

        // 世界卸载时清除搜索结果
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (MinecraftClient.getInstance().isOnThread()) {
                SignSearchManager.getInstance().clearFoundSigns();
            }
        });

        // 注册客户端Tick事件，处理搜索逻辑
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                SignSearchManager searchManager = SignSearchManager.getInstance();
                if (searchManager.isSearching()) {
                    searchManager.tickSearch();
                    handleDebugOutput(searchManager, client);
                }
            }
        });
    }

    /**
     * 处理调试模式下的进度输出
     */
    private static void handleDebugOutput(SignSearchManager searchManager, MinecraftClient client) {
        // 根据输出复杂度判断是否显示调试信息（4对应Debug模式）
        int outputcomplexity = Integer.parseInt(String.valueOf(TextFinder.config.getoutputcomplexity()));
        if (outputcomplexity == 4 && client.player != null) {
            int debuginterval = TextFinder.config.getdebugpgt();
            int totalchecked = searchManager.getTotalSignsChecked();

            if (totalchecked % debuginterval == 0 && totalchecked > 0) {
                int found = searchManager.getFoundSigns().size();
                client.player.sendMessage(
                        Text.literal("已找到" + found + "/" + totalchecked + "个告示牌"),
                        false
                );
            }
        }
    }
}