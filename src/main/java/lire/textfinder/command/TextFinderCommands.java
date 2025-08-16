package lire.textfinder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lire.textfinder.search.SignSearchManager;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class TextFinderCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // 主指令节点
        dispatcher.register(literal("textfinder")
                .then(literal("search")
                        .then(argument("keyword", greedyString())
                                .executes(TextFinderCommands::searchCommand)))
                .then(literal("display")
                        .executes(TextFinderCommands::displayCommand))
                .then(literal("refilter")
                        .then(argument("newKeyword", greedyString())
                                .executes(TextFinderCommands::refilterCommand)))
                .then(literal("clear")
                        .executes(TextFinderCommands::clearCommand)));

        // 简写指令
        dispatcher.register(literal("tf")
                .then(argument("keyword", greedyString())
                        .executes(TextFinderCommands::searchCommand)));

        dispatcher.register(literal("td")
                .executes(TextFinderCommands::displayCommand));

        dispatcher.register(literal("trf")
                .then(argument("newKeyword", greedyString())
                        .executes(TextFinderCommands::refilterCommand)));
    }

    // 搜索指令
    private static int searchCommand(CommandContext<FabricClientCommandSource> context) {
        String keyword = StringArgumentType.getString(context, "keyword");
        FabricClientCommandSource source = context.getSource();

        if (source.getPlayer() == null) {
            source.sendError(Text.literal("请在游戏中使用此指令!"));
            return 0;
        }

        SignSearchManager.getInstance().startSearch(keyword);
        source.sendFeedback(Text.literal("§a开始搜索告示牌中的 '").append(keyword).append("'..."));
        return 1;
    }

    // 显示结果指令
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

        // 调用管理器的输出方法，根据配置的复杂度输出结果
        manager.outputSearchResults(source.getClient());
        return 1;
    }

    // 重新筛选指令
    private static int refilterCommand(CommandContext<FabricClientCommandSource> context) {
        String newKeyword = StringArgumentType.getString(context, "newKeyword");
        FabricClientCommandSource source = context.getSource();

        SignSearchManager manager = SignSearchManager.getInstance();
        manager.refilterSigns(newKeyword);

        source.sendFeedback(Text.literal("§a已用新关键词 '").append(newKeyword).append("' 重新筛选结果"));
        return displayCommand(context);
    }

    // 清除结果指令
    private static int clearCommand(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        SignSearchManager.getInstance().clearFoundSigns();
        source.sendFeedback(Text.literal("§a已清除所有搜索结果"));
        return 1;
    }
}