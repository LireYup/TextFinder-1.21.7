package lire.textfinder;

import lire.textfinder.event.ClientEventHandler;
import lire.textfinder.event.CommandRegistrationHandler;
import lire.textfinder.config.ModConfig;
import net.fabricmc.api.ClientModInitializer; // 确保正确导入
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 确保正确实现客户端初始化接口
public class TextFinder implements ClientModInitializer {
    public static final String MOD_ID = "textfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 配置实例
    public static ModConfig config;

    @Override
    public void onInitializeClient() {
        // 加载配置
        config = ModConfig.load();
        LOGGER.info("配置加载完成");

        // 注册事件处理器
        LOGGER.info("初始化告示牌搜索模组...");
        ClientEventHandler.registerEvents();

        // 注册指令
        CommandRegistrationHandler.register();

        // 注册客户端关闭时的配置保存事件
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            config.save();
            LOGGER.info("保存配置并关闭告示牌搜索模组");
        });

        LOGGER.info("告示牌搜索模组初始化完成!");
    }
}