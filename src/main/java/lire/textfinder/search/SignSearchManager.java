package lire.textfinder.search;

import lire.textfinder.TextFinder;
import lire.textfinder.data.SignData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

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

        // 使用配置中的搜索范围
        int renderDistance = TextFinder.config.getsearchrange();

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

        // 获取每tick最大搜索数量配置
        int maxpertick = TextFinder.config.getmaxsearchamountpertick();
        int processedthistick = 0;

        // 遍历区块，达到每tick最大数量时暂停
        while (chunkIterator.hasNext() && processedthistick < maxpertick) {
            WorldChunk chunk = chunkIterator.next();

            // 获取区块中的所有方块实体
            List<BlockEntity> blockEntities = new ArrayList<>(chunk.getBlockEntities().values());
            int entitycount = blockEntities.size();

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

        // 处理debug模式的进度输出
        handleDebugProgressOutput(client);

        // 所有区块处理完毕，结束搜索
        if (!chunkIterator.hasNext() && nextBlockEntityIndex == 0) {
            isSearching = false;
            int foundCount = foundSigns.size();
            TextFinder.LOGGER.info("Search completed. Found {} matching signs.", foundCount);

            // 发送结果消息给玩家
            if (client.player != null) {
                client.player.sendMessage(Text.literal("找到 " + foundCount + " 个匹配的告示牌"), false);
                outputSearchResults(client);
            }
        }
    }

    /**
     * 处理debug模式的进度输出
     */
    private void handleDebugProgressOutput(MinecraftClient client) {
        if (TextFinder.config.getoutputcomplexity() == 4 && client.player != null) {
            int debugInterval = TextFinder.config.getdebugpgt();
            if (totalSignsChecked % debugInterval == 0 && totalSignsChecked > 0) {
                client.player.sendMessage(Text.literal(
                        "已找到 " + foundSigns.size() + "|" + totalSignsChecked + " 个告示牌"
                ), false);
            }
        }
    }

    /**
     * 根据输出复杂度输出搜索结果
     */
    public void outputSearchResults(MinecraftClient client) {
        if (client.player == null || foundSigns.isEmpty()) return;

        int outputComplexity = TextFinder.config.getoutputcomplexity();

        for (int i = 0; i < foundSigns.size(); i++) {
            SignData sign = foundSigns.get(i);
            Text message = switch (outputComplexity) {
                case 1 -> createSimpleOutput(sign);
                case 2 -> createNormalOutput(sign, i + 1);
                case 3 -> createComplexOutput(sign, i + 1);
                case 4 -> createDebugOutput(sign, i + 1);
                default -> Text.literal("无效的输出模式");
            };
            client.player.sendMessage(message, false);
        }
    }

    /**
     * 创建简单模式输出
     */
    private Text createSimpleOutput(SignData sign) {
        BlockPos pos = sign.getPos();
        StringBuilder content = new StringBuilder();

        // 获取包含关键词的文本行
        List<Text> matchingTexts = sign.getMatchingTexts(currentSearchContext);
        boolean first = true;

        for (Text text : matchingTexts) {
            String textStr = text.getString();
            if (!textStr.isEmpty() && textStr.contains(currentSearchContext)) {
                if (!first) {
                    content.append(Formatting.GRAY).append("; ");
                }
                content.append(text.getString());
                first = false;
            }
        }

        return Text.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " ")
                .append(Text.literal(content.toString()).formatted(getFormattingFromColor(sign.getFrontColor())));
    }

    /**
     * 创建普通模式输出
     */
    private Text createNormalOutput(SignData sign, int index) {
        BlockPos pos = sign.getPos();
        StringBuilder frontContent = new StringBuilder();
        StringBuilder backContent = new StringBuilder();

        // 处理正面文本
        for (Text text : sign.getFrontTexts()) {
            String textStr = text.getString();
            if (!textStr.isEmpty()) {
                if (!frontContent.isEmpty()) {
                    frontContent.append(Formatting.GRAY).append("; ");
                }
                frontContent.append(textStr);
            }
        }

        // 处理反面文本
        for (Text text : sign.getBackTexts()) {
            String textStr = text.getString();
            if (!textStr.isEmpty()) {
                if (!backContent.isEmpty()) {
                    backContent.append(Formatting.GRAY).append("; ");
                }
                backContent.append(textStr);
            }
        }

        // 检查是否两面都有关键字
        boolean frontMatches = sign.getMatchingTexts(currentSearchContext) == sign.getFrontTexts();
        boolean backMatches = sign.getMatchingTexts(currentSearchContext) == sign.getBackTexts() ||
                (frontMatches && sign.matchesBack(currentSearchContext));

        String content = frontMatches ? frontContent.toString() : backContent.toString();
        if (frontMatches && backMatches) {
            content = frontContent + " | " + backContent;
        }

        return Text.literal(index + " : " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " | " + content);
    }

    /**
     * 创建复杂模式输出
     */
    private Text createComplexOutput(SignData sign, int index) {
        BlockPos pos = sign.getPos();
        StringBuilder frontContent = new StringBuilder();
        StringBuilder backContent = new StringBuilder();

        // 处理正面文本
        for (Text text : sign.getFrontTexts()) {
            if (!frontContent.isEmpty()) {
                frontContent.append(Formatting.GRAY).append("; ");
            }
            frontContent.append(text.getString());
        }

        // 处理反面文本
        for (Text text : sign.getBackTexts()) {
            if (!backContent.isEmpty()) {
                backContent.append(Formatting.GRAY).append("; ");
            }
            backContent.append(text.getString());
        }

        return Text.literal("在" + pos.getX() + " " + pos.getY() + " " + pos.getZ() +
                "处找到第" + index + "个告示牌 | 告示牌内容：正面：" +
                frontContent + " 反面：" + backContent);
    }

    /**
     * 创建Debug模式输出
     */
    private Text createDebugOutput(SignData sign, int index) {
        BlockPos pos = sign.getPos();
        StringBuilder frontText = new StringBuilder();
        StringBuilder backText = new StringBuilder();

        // 处理正面文本
        for (Text text : sign.getFrontTexts()) {
            String textStr = text.getString();
            if (!frontText.isEmpty()) {
                frontText.append("|");
            }
            frontText.append(textStr.isEmpty() ? Formatting.GRAY.toString() + Formatting.ITALIC + "无" + Formatting.RESET : textStr);
        }

        // 处理反面文本
        for (Text text : sign.getBackTexts()) {
            String textStr = text.getString();
            if (!backText.isEmpty()) {
                backText.append("|");
            }
            backText.append(textStr.isEmpty() ? Formatting.GRAY.toString() + Formatting.ITALIC + "无" + Formatting.RESET : textStr);
        }

        return Text.literal("[序号：" + index + "][坐标：" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]" +
                "{文本：[" + frontText + "][正面是否发光：" + (sign.isFrontGlowing() ? "是" : "否") + "]" +
                "[" + backText + "][反面是否发光：" + (sign.isBackGlowing() ? "是" : "否") + "]}" +
                "[方块ID：" + sign.getBlockId() + "]");
    }

    /**
     * 从颜色字符串获取格式
     */
    private Formatting getFormattingFromColor(String color) {
        if (color == null || color.equals("default")) {
            return Formatting.WHITE;
        }
        try {
            return Formatting.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Formatting.WHITE;
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
                sign.getFrontText().getColor().getName() : "default";
        String backColor = sign.getBackText().getColor() != null ?
                sign.getBackText().getColor().getName() : "default";

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
    }
}