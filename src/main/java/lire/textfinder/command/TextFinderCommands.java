package lire.textfinder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import lire.textfinder.search.SignSearchManager;
import lire.textfinder.data.SignData;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class TextFinderCommands {

    // 修正参数未使用警告
    @SuppressWarnings("unused")
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
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

    /**
     * 搜索指令处理
     */
    private static int searchCommand(CommandContext<ServerCommandSource> context) {
        String keyword = StringArgumentType.getString(context, "keyword");
        ServerCommandSource source = context.getSource();

        // 检查是否在游戏中
        if (source.getPlayer() == null) {
            source.sendError(Text.literal("请在游戏中使用此指令!"));
            return 0;
        }

        // 开始搜索
        SignSearchManager.getInstance().startSearch(keyword);
        source.sendFeedback(() -> Text.literal("§a开始搜索告示牌中的 '").append(keyword).append("'..."), false);
        return 1;
    }

    /**
     * 显示结果指令处理
     */
    private static int displayCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        SignSearchManager manager = SignSearchManager.getInstance();

        if (manager.isSearching()) {
            source.sendFeedback(() -> Text.literal("§e搜索正在进行中... 已检查 " + manager.getTotalSignsChecked() + " 个告示牌"), false);
            return 0;
        }

        var results = manager.getFoundSigns();
        if (results.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§e未找到匹配的告示牌"), false);
            return 0;
        }

        // 显示结果（最多显示10个）
        source.sendFeedback(() -> Text.literal("§a找到 " + results.size() + " 个匹配的告示牌:"), false);
        int displaycount = Math.min(results.size(), 10);

        // 修复lambda变量问题：使用临时变量存储索引
        for (int i = 0; i < displaycount; i++) {
            final int index = i;  // 声明为final
            SignData sign = results.get(i);
            // 使用getFirst()替换get(0)
            source.sendFeedback(() -> Text.literal(
                    "§b" + (index + 1) + ". §r" +
                            "坐标: " + sign.getPos().toShortString() + " §7" +
                            (sign.getFrontTexts().getFirst().getString().length() > 20 ?
                                    sign.getFrontTexts().getFirst().getString().substring(0, 20) + "..." :
                                    sign.getFrontTexts().getFirst().getString())
            ), false);
        }

        if (results.size() > 10) {
            source.sendFeedback(() -> Text.literal("§e还有 " + (results.size() - 10) + " 个结果未显示"), false);
        }

        return 1;
    }

    /**
     * 重新筛选指令处理
     */
    private static int refilterCommand(CommandContext<ServerCommandSource> context) {
        String newKeyword = StringArgumentType.getString(context, "newKeyword");
        ServerCommandSource source = context.getSource();

        SignSearchManager manager = SignSearchManager.getInstance();
        manager.refilterSigns(newKeyword);

        source.sendFeedback(() -> Text.literal("§a已用新关键词 '").append(newKeyword).append("' 重新筛选结果"), false);
        return displayCommand(context); // 显示筛选后的结果
    }

    /**
     * 清除结果指令处理
     */
    private static int clearCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        SignSearchManager.getInstance().clearFoundSigns();
        source.sendFeedback(() -> Text.literal("§a已清除所有搜索结果"), false);
        return 1;
    }
}