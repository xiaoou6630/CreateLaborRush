package com.xiaoou.rush.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.SignText;

/**
 * 「挂牌」功能的数据读写。
 *
 * <p><b>物品侧：</b>1.21 的告示牌文字不是独立的数据组件，而是存在方块实体的
 * {@code front_text} 里、通过 {@link DataComponents#BLOCK_ENTITY_DATA} 挂在物品上。
 * 所以这里也走同一条路：用 {@link SignText#DIRECT_CODEC} 编出原版一模一样的
 * {@code front_text} 结构写进 {@code BLOCK_ENTITY_DATA}。这样做的直接好处是——
 * 牌子放下当方块时，原版 {@code BlockItem} 会把这段数据合并进告示牌方块实体，文字不丢。
 *
 * <p><b>实体侧：</b>牌子挂在村民身上时，把整件物品（含文字）和一份纯文本行存进
 * {@code entity.getPersistentData()}：前者用于原样取回物品，后者供客户端渲染层零成本读取。
 * <b>注意</b>：持久化数据不会同步到客户端，所以村民身上的牌子目前只有服务端数据，
 * 客户端渲染层读不到内容（详见渲染层的说明）。
 */
public final class HangingSignData {

    /** 告示牌行数，与原版一致 */
    public static final int LINES = 4;

    /** 标记：该实体身上已挂牌 */
    public static final String KEY_WORN = "laborrush.sign.worn";
    /** 纯文本的 4 行文字，给渲染层读 */
    public static final String KEY_LINES = "laborrush.sign.lines";
    /** 完整物品 NBT，用于原样取回 */
    public static final String KEY_STACK = "laborrush.sign.stack";

    private static final String KEY_FRONT_TEXT = "front_text";

    /**
     * 方块实体类型 id。
     *
     * <p>1.21 的 {@code BLOCK_ENTITY_DATA} 是个"带校验"的数据组件：里面必须有一个能解析成
     * {@code BlockEntityType} 的 {@code id}，否则保存/发包时会直接抛
     * {@code Missing id for entity in: ...} 把存档写崩。原版告示牌方块被破坏时掉落的物品
     * 就是带着这个 {@code id} 的，所以手写时必须补上。
     */
    private static final String KEY_ID = "id";
    private static final String HANGING_SIGN_ID = "minecraft:hanging_sign";

    private HangingSignData() {
    }

    /**
     * 读物品正面的 4 行文字；没有文字数据时返回 4 个空串。
     */
    public static String[] readLines(ItemStack stack) {
        String[] lines = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            lines[i] = "";
        }
        CompoundTag frontText = frontTextTag(stack);
        if (frontText == null) {
            return lines;
        }
        SignText text = SignText.DIRECT_CODEC.parse(NbtOps.INSTANCE, frontText).result().orElse(null);
        if (text == null) {
            return lines;
        }
        for (int i = 0; i < LINES; i++) {
            lines[i] = text.getMessage(i, false).getString();
        }
        return lines;
    }

    /**
     * 把 4 行文字写进物品（写到原版告示牌的 {@code front_text} 结构里）。
     */
    public static void writeLines(ItemStack stack, String[] lines) {
        SignText text = new SignText();
        for (int i = 0; i < LINES; i++) {
            String line = lines[i] == null ? "" : lines[i];
            text = text.setMessage(i, Component.literal(line));
        }
        Tag encoded = SignText.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, text).getOrThrow();
        CustomData.update(DataComponents.BLOCK_ENTITY_DATA, stack, tag -> {
            tag.putString(KEY_ID, HANGING_SIGN_ID);
            tag.put(KEY_FRONT_TEXT, encoded);
        });
    }

    /** 造一块已经写好文字的告示牌物品 */
    public static ItemStack createSign(Item item, String[] lines) {
        ItemStack stack = new ItemStack(item);
        writeLines(stack, lines);
        return stack;
    }

    /** 该实体身上是否挂着牌子 */
    public static boolean hasWornSign(Entity entity) {
        return entity.getPersistentData().getBoolean(KEY_WORN);
    }

    /** 把牌子挂到实体身上：整件物品 + 纯文本行一起存进持久化数据 */
    public static void hangOn(Entity entity, ItemStack stack) {
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(KEY_WORN, true);

        String[] lines = readLines(stack);
        ListTag list = new ListTag();
        for (String line : lines) {
            list.add(StringTag.valueOf(line));
        }
        data.put(KEY_LINES, list);

        data.put(KEY_STACK, stack.copyWithCount(1).save(entity.registryAccess()));
    }

    /**
     * 读实体身上挂着的 4 行文字；没挂返回 {@code null}（调用方据此跳过渲染）。
     */
    public static String[] readWornLines(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(KEY_WORN)) {
            return null;
        }
        ListTag list = data.getList(KEY_LINES, Tag.TAG_STRING);
        String[] lines = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            lines[i] = i < list.size() ? list.getString(i) : "";
        }
        return lines;
    }

    /** 把牌子从实体身上取回成物品，并清掉持久化数据；没挂返回 {@link ItemStack#EMPTY} */
    public static ItemStack takeOff(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(KEY_WORN)) {
            return ItemStack.EMPTY;
        }
        ItemStack sign = data.contains(KEY_STACK, Tag.TAG_COMPOUND)
            ? ItemStack.parse(entity.registryAccess(), data.getCompound(KEY_STACK)).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;

        data.remove(KEY_WORN);
        data.remove(KEY_LINES);
        data.remove(KEY_STACK);
        return sign;
    }

    private static CompoundTag frontTextTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.getCompound(KEY_FRONT_TEXT);
    }
}
