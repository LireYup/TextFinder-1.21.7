package lire.textfinder.search;

import lire.textfinder.TextFinder;
import lire.textfinder.data.SignData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 管理告示牌搜索过程和结果的类
 */
public class SignSearchManager {
    // 存储搜索到的符合条件的告示牌
    private final List<SignData> foundSigns = new CopyOnWriteArrayList<>();
    // 存储所有检测过的告示牌总数（用于调试输出）
    private int totalSignsChecked = 0;
    // 所有检测过的告示牌总数（用于进度显示）
    private int totalSignsProcessed = 0;
    // 当前搜索上下文
    private String currentSearchContext = "";
    // 搜索是否正在进行中
    private boolean isSearching = false;
    // 记录下一个要搜索的区块迭代器
    private Iterator<WorldChunk> chunkIterator;
    // 记录当前区块中要搜索的方块实体索引
    private int nextBlockEntityIndex = 0;

    private static SignSearchManager instance;

    private SignSearchManager() {}

    public static synchronized SignSearchManager getInstance() {
        if (instance == null) {
            instance = new SignSearchManager();
        }
        return instance;
    }

    /**
     * 开始新的搜索
     */
    public void startSearch(String searchContext) {
        resetSearch();
        this.currentSearchContext = searchContext;
        this.isSearching = true;

        // 初始化区块迭代器（1.21.7兼容方式）
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            List<WorldChunk> loadedChunks = collectLoadedChunks(client);
            this.chunkIterator = loadedChunks.iterator();
        }

        TextFinder.LOGGER.info("Starting search for: {}", searchContext);
    }

    /**
     * 收集所有已加载的区块（1.21.7兼容实现）
     */
    private List<WorldChunk> collectLoadedChunks(MinecraftClient client) {
        List<WorldChunk> chunks = new ArrayList<>();
        if (client.world == null || client.player == null) return chunks;

        // 获取玩家所在区块
        BlockPos playerPos = client.player.getBlockPos();
        ChunkPos playerChunkPos = new ChunkPos(playerPos);

        // 1.21.7兼容方案：使用默认渲染距离8
        int renderDistance = 8;

        // 搜索玩家周围一定范围内的区块
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                ChunkPos chunkPos = new ChunkPos(
                        playerChunkPos.x + x,
                        playerChunkPos.z + z
                );
                // 获取区块（1.21.7兼容方式）
                WorldChunk chunk = client.world.getChunkManager()
                        .getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }
        return chunks;
    }

    /**
     * 重置搜索状态
     */
    public void resetSearch() {
        foundSigns.clear();
        totalSignsChecked = 0;
        totalSignsProcessed = 0; // 重置总处理数
        currentSearchContext = "";
        isSearching = false;
        chunkIterator = null;
        nextBlockEntityIndex = 0;
    }

    /**
     * 继续搜索过程（每个游戏刻调用一次）
     */
    public void tickSearch() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isSearching || client.world == null || chunkIterator == null) {
            return;
        }

        // 修正配置方法调用（全小写方法名）
        int maxpertick = TextFinder.config.getmaxsearchamountpertick();
        int processedthistick = 0;

        // 遍历区块
        while (chunkIterator.hasNext() && processedthistick < maxpertick) {
            WorldChunk chunk = chunkIterator.next();

            // 获取区块中的所有方块实体
            List<BlockEntity> blockEntities = new ArrayList<>(chunk.getBlockEntities().values());
            int entitycount = blockEntities.size();
            totalSignsProcessed += entitycount; // 累加总处理数

            // 遍历方块实体
            while (nextBlockEntityIndex < entitycount && processedthistick < maxpertick) {
                BlockEntity blockEntity = blockEntities.get(nextBlockEntityIndex);
                nextBlockEntityIndex++;

                // 检查是否是告示牌
                if (blockEntity instanceof SignBlockEntity signBlockEntity) {
                    totalSignsChecked++;
                    processedthistick++;

                    // 提取告示牌数据
                    SignData signData = createSignData(signBlockEntity);

                    // 检查是否匹配搜索内容
                    if (signData.matches(currentSearchContext)) {
                        foundSigns.add(signData);
                    }
                }
            }

            // 当前区块处理完毕，重置方块实体索引
            if (nextBlockEntityIndex >= entitycount) {
                nextBlockEntityIndex = 0;
            } else {
                // 本tick已达到最大处理数量，退出循环
                break;
            }
        }

        // 所有区块处理完毕，结束搜索
        if (!chunkIterator.hasNext() && nextBlockEntityIndex == 0) {
            isSearching = false;
            int foundCount = foundSigns.size();
            TextFinder.LOGGER.info("Search completed. Found {} matching signs.", foundCount);

            // 发送结果消息给玩家
            client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("找到 " + foundCount + " 个匹配的告示牌"), false);
            }
        }
    }

    /**
     * 从告示牌方块实体创建SignData对象
     */
    private SignData createSignData(SignBlockEntity sign) {
        BlockPos pos = sign.getPos();
        BlockState state = sign.getCachedState();

        // 获取正反面文本
        List<Text> frontTexts = List.of(
                sign.getFrontText().getMessage(0, false),
                sign.getFrontText().getMessage(1, false),
                sign.getFrontText().getMessage(2, false),
                sign.getFrontText().getMessage(3, false)
        );
        List<Text> backTexts = List.of(
                sign.getBackText().getMessage(0, false),
                sign.getBackText().getMessage(1, false),
                sign.getBackText().getMessage(2, false),
                sign.getBackText().getMessage(3, false)
        );

        // 获取颜色和发光状态
        String frontColor = sign.getFrontText().getColor() != null ?
                sign.getFrontText().getColor().toString() : "default";
        String backColor = sign.getBackText().getColor() != null ?
                sign.getBackText().getColor().toString() : "default";

        return new SignData(
                pos,
                state,
                frontTexts,
                frontColor,
                sign.getFrontText().isGlowing(),
                backTexts,
                backColor,
                sign.getBackText().isGlowing()
        );
    }

    /**
     * 重新筛选已找到的告示牌
     */
    public void refilterSigns(String newContext) {
        List<SignData> filtered = new ArrayList<>();
        for (SignData sign : foundSigns) {
            if (sign.matches(newContext)) {
                filtered.add(sign);
            }
        }
        foundSigns.clear();
        foundSigns.addAll(filtered);
        currentSearchContext = newContext;
    }

    /**
     * 输出搜索结果到命令源（根据数字类型的输出复杂度）
     * 复杂度等级：1=极简(仅坐标)，2=简单(坐标+首行文本)，3=详细(坐标+多行文本)，4=调试(全信息+进度)
     */
    public void outputSearchResults(FabricClientCommandSource source) {
        if (isSearching()) {
            source.sendFeedback(Text.literal("§e搜索正在进行中... 已检查 " + getTotalSignsChecked() + " 个告示牌"));
            return;
        }

        List<SignData> results = getFoundSigns();
        if (results.isEmpty()) {
            source.sendFeedback(Text.literal("§e未找到匹配的告示牌"));
            return;
        }

        // 读取数字类型的输出复杂度（关键修复：适配配置中的3/4等级）
        int complexity;
        try {
            complexity = Integer.parseInt(TextFinder.config.getoutputcomplexity());
        } catch (NumberFormatException e) {
            complexity = 2; // 默认简单模式
        }

        // 复杂度4：显示进度信息（匹配日志中的"已找到2/4个告示牌"）
        if (complexity >= 4) {
            source.sendFeedback(Text.literal("已找到" + results.size() + "/" + totalSignsChecked + "个告示牌"));
        }

        source.sendFeedback(Text.literal("§a找到 " + results.size() + " 个匹配的告示牌:"));
        int displayCount = Math.min(results.size(), 10);

        for (int index = 0; index < displayCount; index++) {
            SignData sign = results.get(index);
            source.sendFeedback(formatSignText(sign, index + 1, complexity));
        }

        if (results.size() > 10) {
            source.sendFeedback(Text.literal("§e还有 " + (results.size() - 10) + " 个结果未显示"));
        }
    }

    /**
     * 根据数字复杂度格式化单个告示牌信息
     */
    private Text formatSignText(SignData sign, int index, int complexity) {
        BlockPos pos = sign.getPos();
        StringBuilder sb = new StringBuilder();
        sb.append("§b").append(index).append(". §r坐标: ").append(pos.toShortString());

        // 复杂度2及以上：显示首行文本
        if (complexity >= 2) {
            String firstLine = sign.getFrontTexts().getFirst().getString();
            if (firstLine.isEmpty() && !sign.getBackTexts().isEmpty()) {
                firstLine = sign.getBackTexts().getFirst().getString();
            }
            sb.append(" §7").append(truncateText(firstLine, 20));
        }

        // 复杂度3及以上：显示多行文本和颜色
        if (complexity >= 3) {
            if (!sign.getFrontTexts().get(1).getString().isEmpty()) {
                sb.append(" / ").append(truncateText(sign.getFrontTexts().get(1).getString(), 15));
            }
            sb.append(" §6[颜色: ").append(sign.getFrontColor()).append("]");
        }

        // 复杂度4及以上：显示发光状态和方块类型
        if (complexity >= 4) {
            sb.append(" §d[发光: ").append(sign.isFrontGlowing() ? "是" : "否").append("]");
            sb.append(" §f[").append(sign.getState().getBlock().getName().getString()).append("]");
        }

        return Text.literal(sb.toString());
    }

    /**
     * 截断长文本
     */
    private String truncateText(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // Getters
    public List<SignData> getFoundSigns() {
        return new ArrayList<>(foundSigns);
    }

    public int getTotalSignsChecked() {
        return totalSignsChecked;
    }

    public boolean isSearching() {
        return isSearching;
    }

    public String getCurrentSearchContext() {
        return currentSearchContext;
    }

    /**
     * 清除所有找到的告示牌（进入世界或开始新搜索时调用）
     */
    public void clearFoundSigns() {
        foundSigns.clear();
        totalSignsChecked = 0;
        totalSignsProcessed = 0;
    }
}