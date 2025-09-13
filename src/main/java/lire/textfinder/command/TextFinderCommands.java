package lire.textfinder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lire.textfinder.search.SignSearchManager;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class TextFinderCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // 主指令节点
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

    // 关键修复：调用新的outputSearchResults方法，直接传入命令源
    private static int displayCommand(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        SignSearchManager manager = SignSearchManager.getInstance();

        // 直接调用管理器的输出方法，传入命令源
        manager.outputSearchResults(source);
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