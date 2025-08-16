package lire.textfinder.event;

import lire.textfinder.TextFinder;
import lire.textfinder.command.TextFinderCommands;
// 替换为客户端指令注册回调
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * 注册模组指令的事件处理器
 */
public class CommandRegistrationHandler {

    /**
     * 注册所有指令（客户端指令）
     */
    public static void register() {
        // 使用客户端指令注册回调，确保指令在客户端生效
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TextFinder.LOGGER.info("注册告示牌搜索客户端指令...");
            TextFinderCommands.register(dispatcher);
        });
    }
}