package com.xiaoou.rush.client;

import com.xiaoou.rush.network.HangingSignEditPayload;
import com.xiaoou.rush.util.HangingSignData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 「挂牌」写字界面：直接用原版告示牌编辑界面（{@link SignEditScreen}）编辑 4 行文字。
 *
 * <p>原版界面是绑在告示牌方块实体上的，所以这里给它一块"离屏假 BE"（{@link DetachedSign}）：
 * 用原版悬挂告示牌的方块状态撑起排版，把物品上现有的文字灌进去。玩家打完字关掉界面后，
 * 再从假 BE 里把文字取回来，按 {@link HangingSignEditPayload} 回传服务端落盘
 * （客户端改物品数据不会被同步，必须走服务端）。
 *
 * <p><b>为什么不用原版的保存路径：</b>原版 {@code AbstractSignEditScreen.removed()} 会发一个
 * {@code ServerboundSignUpdatePacket}，指向真实告示牌方块的坐标。这里的假 BE 坐标只是占位的
 * {@link BlockPos#ZERO}，走那条路会把包发去一个根本不存在的位置，所以覆写 {@code removed()}
 * 换成自己的包。原版界面在输入时会实时把内容写回假 BE，关闭后直接读即可。
 */
public class HangingSignEditScreen extends SignEditScreen {

    /** 离屏假 BE，用于关闭界面后取回文字 */
    private final SignBlockEntity sign;
    /** 要写字的牌子在哪个槽位：true = 胸甲栏，false = 主手 */
    private final boolean chestSlot;
    /** 防止 removed() 被重复触发时把文字发两遍 */
    private boolean submitted;

    public HangingSignEditScreen(SignBlockEntity sign, boolean chestSlot) {
        // isFrontText=true：挂牌只写正面；isTextFilteringEnabled=false：与客户端默认写法一致
        super(sign, true, false);
        this.sign = sign;
        this.chestSlot = chestSlot;
    }

    /**
     * 造一块离屏假告示牌，灌入物品上现有的 4 行文字。
     *
     * <p>方块状态取原版悬挂告示牌：原版界面据此解析木材类型，并按悬挂牌的排版渲染。
     */
    public static SignBlockEntity createDetachedSign(ItemStack source) {
        String[] lines = HangingSignData.readLines(source);
        SignText text = new SignText();
        for (int i = 0; i < HangingSignData.LINES; i++) {
            text = text.setMessage(i, Component.literal(lines[i]));
        }
        return new DetachedSign(Blocks.OAK_HANGING_SIGN.defaultBlockState(), text);
    }

    /**
     * 关掉界面（含被背包等其它界面顶掉）时把文字取回来发给服务端。
     *
     * <p>刻意不调用 {@code super.removed()}——那是给真告示牌方块发原版更新包的；
     * 这里直接把假 BE 里已经实时写好的 4 行打包成自己的包。
     */
    @Override
    public void removed() {
        if (this.submitted) {
            return;
        }
        this.submitted = true;

        SignText text = this.sign.getText(true);
        List<String> lines = new ArrayList<>(HangingSignData.LINES);
        for (int i = 0; i < HangingSignData.LINES; i++) {
            lines.add(text.getMessage(i, false).getString());
        }
        // 与 ConfigSyncClient 一致：断开连接时（没有连接）就不发
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        PacketDistributor.sendToServer(new HangingSignEditPayload(lines, this.chestSlot));
    }

    /**
     * 离屏假告示牌方块实体：只把文字存在自己兜里，不碰世界。
     *
     * <p>父类的 {@code setText} 会走 {@code markUpdated() → level.sendBlockUpdated(...)}，
     * 而假 BE 没有 level（{@code null}）会直接 NPE，所以覆写 {@code setText/getText} 绕开世界那一套。
     * 同理把 {@code playerIsTooFarAwayToEdit} 固定为 false：父类实现要读 level 里的玩家，
     * 假 BE 会 NPE，导致原版界面一 tick 就自动关掉。
     */
    private static final class DetachedSign extends HangingSignBlockEntity {

        private SignText text;

        DetachedSign(BlockState state, SignText text) {
            // 必须走 HangingSignBlockEntity：SignBlockEntity 的构造写死 BlockEntityType.SIGN，
            // 配上悬挂牌方块状态会被 BlockEntity 的校验判为"方块实体类型与方块状态不匹配"直接抛异常
            super(BlockPos.ZERO, state);
            this.text = text;
        }

        @Override
        public SignText getText(boolean isFrontText) {
            return this.text;
        }

        @Override
        public boolean setText(SignText text, boolean isFrontText) {
            this.text = text;
            return true;
        }

        @Override
        public boolean playerIsTooFarAwayToEdit(UUID uuid) {
            return false;
        }
    }
}
