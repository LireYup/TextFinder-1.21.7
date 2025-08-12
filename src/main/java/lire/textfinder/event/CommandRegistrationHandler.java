package lire.textfinder.event;

import lire.textfinder.TextFinder;
import lire.textfinder.command.TextFinderCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * 注册模组指令的事件处理器
 */
public class CommandRegistrationHandler {

    /**
     * 注册所有指令
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TextFinder.LOGGER.info("注册告示牌搜索指令...");
            TextFinderCommands.register(dispatcher, registryAccess, environment);
        });
    }
}
