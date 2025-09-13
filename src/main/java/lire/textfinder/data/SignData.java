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
    private final BlockState state; // 新增：存储BlockState
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
        this.state = state; // 新增：初始化BlockState
        this.frontTexts = frontTexts;
        this.frontColor = frontColor;
        this.frontGlowing = frontGlowing;
        this.backTexts = backTexts;
        this.backColor = backColor;
        this.backGlowing = backGlowing;
    }

    // 新增：获取BlockState的getter方法
    public BlockState getState() {
        return state;
    }

    // 其他现有getter方法保持不变...
    public BlockPos getPos() {
        return pos;
    }

    public Block getBlock() {
        return block;
    }

    public int getBlockId() {
        return blockId;
    }

    public List<Text> getFrontTexts() {
        return frontTexts;
    }

    public String getFrontColor() {
        return frontColor;
    }

    public boolean isFrontGlowing() {
        return frontGlowing;
    }

    public List<Text> getBackTexts() {
        return backTexts;
    }

    public String getBackColor() {
        return backColor;
    }

    public boolean isBackGlowing() {
        return backGlowing;
    }

    // 其他现有方法保持不变...
    public boolean matches(String searchContext) {
        // 检查正面文本
        for (Text text : frontTexts) {
            if (text.getString().contains(searchContext)) {
                return true;
            }
        }
        // 检查背面文本
        for (Text text : backTexts) {
            if (text.getString().contains(searchContext)) {
                return true;
            }
        }
        return false;
    }

    public List<Text> getMatchingTexts(String searchContext) {
        for (Text text : frontTexts) {
            if (text.getString().contains(searchContext)) {
                return frontTexts;
            }
        }
        return backTexts;
    }
}