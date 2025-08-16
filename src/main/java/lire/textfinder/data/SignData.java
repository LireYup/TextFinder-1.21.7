package lire.textfinder.data;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * 存储单个告示牌的所有相关数据
 */
public class SignData {
    private final BlockPos pos;
    private final Block block;
    private final int blockId;
    private final List<Text> frontTexts;
    private final String frontColor;
    private final boolean frontGlowing;
    private final List<Text> backTexts;
    private final String backColor;
    private final boolean backGlowing;

    public SignData(BlockPos pos, BlockState state, List<Text> frontTexts, String frontColor, boolean frontGlowing,
                    List<Text> backTexts, String backColor, boolean backGlowing) {
        this.pos = pos;
        this.block = state.getBlock();
        this.blockId = Block.getRawIdFromState(state);
        this.frontTexts = frontTexts;
        this.frontColor = frontColor;
        this.frontGlowing = frontGlowing;
        this.backTexts = backTexts;
        this.backColor = backColor;
        this.backGlowing = backGlowing;
    }

    // Getters
    public BlockPos getPos() { return pos; }
    public Block getBlock() { return block; }
    public int getBlockId() { return blockId; }
    public List<Text> getFrontTexts() { return frontTexts; }
    public String getFrontColor() { return frontColor; }
    public boolean isFrontGlowing() { return frontGlowing; }
    public List<Text> getBackTexts() { return backTexts; }
    public String getBackColor() { return backColor; }
    public boolean isBackGlowing() { return backGlowing; }

    /**
     * 检查告示牌是否匹配搜索关键词
     */
    public boolean matches(String searchContext) {
        String processedKeyword = processString(searchContext);

        // 检查正面文本
        for (Text text : frontTexts) {
            String textStr = processString(text.getString());
            if (textStr.contains(processedKeyword)) {
                return true;
            }
        }

        // 检查背面文本
        return matchesBack(searchContext);
    }

    /**
     * 检查背面是否匹配搜索关键词
     */
    public boolean matchesBack(String searchContext) {
        String processedKeyword = processString(searchContext);

        for (Text text : backTexts) {
            String textStr = processString(text.getString());
            if (textStr.contains(processedKeyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字符串预处理：去除前后空格 + 全角转半角
     */
    private String processString(String str) {
        if (str == null) return "";
        // 去除前后空格
        String trimmed = str.trim();
        // 全角转半角
        StringBuilder sb = new StringBuilder();
        for (char c : trimmed.toCharArray()) {
            if (c == 12288) { // 全角空格
                sb.append((char) 32);
            } else if (c >= 65281 && c <= 65374) { // 全角字符范围
                sb.append((char) (c - 65248));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 获取包含匹配文本的面的文本内容
     */
    public List<Text> getMatchingTexts(String searchContext) {
        String processedKeyword = processString(searchContext);

        for (Text text : frontTexts) {
            if (processString(text.getString()).contains(processedKeyword)) {
                return frontTexts;
            }
        }
        return backTexts;
    }
}