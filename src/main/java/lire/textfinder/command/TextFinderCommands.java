package lire.textfinder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lire.textfinder.search.SignSearchManager;
import lire.textfinder.data.SignData;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
// 导入客户端指令相关类
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class TextFinderCommands {

    // 适配客户端指令注册参数（移除服务器环境参数）
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess) {
        // 主指令节点（使用客户端指令源）
        dispatcher.register(literal("textfinder")
                .then(literal("search")
                        .then(argument("keyword", StringArgumentType.string())
                                .executes(TextFinderCommands::searchCommand)))
                .then(literal("display")
                        .executes(TextFinderCommands::displayCommand))
                .then(literal("refilter")
                        .then(argument("newKeyword", StringArgumentType.string())
                                .executes(TextFinderCommands::refilterCommand)))
                .then(literal("clear")
                        .executes(TextFinderCommands::clearCommand)));

        // 简写指令
        dispatcher.register(literal("tf")
                .then(argument("keyword", StringArgumentType.string())
                        .executes(TextFinderCommands::searchCommand)));

        dispatcher.register(literal("td")
                .executes(TextFinderCommands::displayCommand));

        dispatcher.register(literal("trf")
                .then(argument("newKeyword", StringArgumentType.string())
                        .executes(TextFinderCommands::refilterCommand)));
    }

    // 以下指令处理方法的参数类型需从 ServerCommandSource 改为 FabricClientCommandSource
    private static int searchCommand(CommandContext<FabricClientCommandSource> context) {
        String keyword = StringArgumentType.getString(context, "keyword");
        FabricClientCommandSource source = context.getSource();

        // 检查是否在游戏中（客户端指令源获取玩家）
        if (source.getPlayer() == null) {
            source.sendError(Text.literal("请在游戏中使用此指令!"));
            return 0;
        }

        // 开始搜索（逻辑不变）
        SignSearchManager.getInstance().startSearch(keyword);
        source.sendFeedback(Text.literal("§a开始搜索告示牌中的 '").append(keyword).append("'..."));
        return 1;
    }

    // 同样修改 displayCommand/refilterCommand/clearCommand 的参数类型为 FabricClientCommandSource
    private static int displayCommand(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        SignSearchManager manager = SignSearchManager.getInstance();

        if (manager.isSearching()) {
            source.sendFeedback(Text.literal("§e搜索正在进行中... 已检查 " + manager.getTotalSignsChecked() + " 个告示牌"));
            return 0;
        }

        var results = manager.getFoundSigns();
        if (results.isEmpty()) {
            source.sendFeedback(Text.literal("§e未找到匹配的告示牌"));
            return 0;
        }

        // 显示结果（逻辑不变）
        source.sendFeedback(Text.literal("§a找到 " + results.size() + " 个匹配的告示牌:"));
        int displaycount = Math.min(results.size(), 10);

        for (int index = 0; index < displaycount; index++) {
            SignData sign = results.get(index);
            source.sendFeedback(Text.literal(
                    "§b" + (index + 1) + ". §r" +
                            "坐标: " + sign.getPos().toShortString() + " §7" +
                            (sign.getFrontTexts().getFirst().getString().length() > 20 ?
                                    sign.getFrontTexts().getFirst().getString().substring(0, 20) + "..." :
                                    sign.getFrontTexts().getFirst().getString())
            ));
        }

        if (results.size() > 10) {
            source.sendFeedback(Text.literal("§e还有 " + (results.size() - 10) + " 个结果未显示"));
        }

        return 1;
    }

    private static int refilterCommand(CommandContext<FabricClientCommandSource> context) {
        String newKeyword = StringArgumentType.getString(context, "newKeyword");
        FabricClientCommandSource source = context.getSource();

        SignSearchManager manager = SignSearchManager.getInstance();
        manager.refilterSigns(newKeyword);

        source.sendFeedback(Text.literal("§a已用新关键词 '").append(newKeyword).append("' 重新筛选结果"));
        return displayCommand(context);
    }

    private static int clearCommand(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        SignSearchManager.getInstance().clearFoundSigns();
        source.sendFeedback(Text.literal("§a已清除所有搜索结果"));
        return 1;
    }
}