package com.xiaoou.rush.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * 「挂牌」功能的数据读写：把原版悬挂告示牌挂到玩家/村民身上，并搬运牌子上的 4 行文字。
 *
 * <p>文字结构刻意照抄原版告示牌方块实体的 NBT：{@code front_text.messages} 是 4 个 JSON 文本。
 * 物品上整块塞进 {@code BlockEntityTag}，所以把牌子放下当方块时文字会由原版自动带过去。
 * 村民则把同样的一份 {@code front_text} 连同告示牌种类写进持久化数据，
 * <b>这个标签存在本身就代表「已挂」</b>，不再单独存标记位。
 */
public final class SignBoardData {

    /** 悬挂告示牌固定 4 行 */
    public static final int LINES = 4;

    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    /** 方块实体类型 id：原版放下牌子时会用它来挑方块实体类型，缺了文字就恢复不出来 */
    private static final String ID = "id";
    private static final String HANGING_SIGN_ID = "minecraft:hanging_sign";
    private static final String FRONT_TEXT = "front_text";
    private static final String MESSAGES = "messages";
    private static final String COLOR = "color";
    private static final String GLOWING_TEXT = "has_glowing_text";

    /** 村民持久化数据里的键 */
    private static final String ENTITY_TAG = "createlaborrush:sign_board";
    private static final String ITEM_ID = "item";

    private SignBoardData() {
    }

    // ---------------- 物品 ----------------

    /** 把 4 行文字写进物品 NBT（等于重新给牌子写字，会盖掉原有内容） */
    public static void writeToItem(ItemStack stack, String[] lines) {
        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString(ID, HANGING_SIGN_ID);
        blockEntity.put(FRONT_TEXT, frontText(lines));
        stack.getOrCreateTag().put(BLOCK_ENTITY_TAG, blockEntity);
    }

    /** 读物品上的 4 行文字；没写过字返回 null */
    public static String[] readFromItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BLOCK_ENTITY_TAG)) {
            return null;
        }
        return readMessages(tag.getCompound(BLOCK_ENTITY_TAG));
    }

    // ---------------- 村民 ----------------

    /** 该实体身上是否已经挂着牌子 */
    public static boolean hasSign(Entity entity) {
        return entity.getPersistentData().contains(ENTITY_TAG);
    }

    /** 把牌子挂到实体身上：记下牌子种类 + 它当前写着的 4 行文字 */
    public static void hangOn(Entity entity, ItemStack signStack) {
        String[] lines = readFromItem(signStack);
        if (lines == null) {
            lines = new String[LINES];
            Arrays.fill(lines, "");
        }
        CompoundTag data = new CompoundTag();
        data.putString(ITEM_ID, BuiltInRegistries.ITEM.getKey(signStack.getItem()).toString());
        data.put(FRONT_TEXT, frontText(lines));
        entity.getPersistentData().put(ENTITY_TAG, data);
    }

    /** 读实体身上牌子的 4 行文字；没挂返回 null */
    public static String[] readFromEntity(Entity entity) {
        if (!hasSign(entity)) {
            return null;
        }
        return readMessages(entity.getPersistentData().getCompound(ENTITY_TAG));
    }

    /** 把牌子取下来：原样还原成对应的悬挂告示牌物品，并从实体数据里删掉（没挂则返回空） */
    public static ItemStack takeFrom(Entity entity) {
        if (!hasSign(entity)) {
            return ItemStack.EMPTY;
        }
        CompoundTag data = entity.getPersistentData().getCompound(ENTITY_TAG);
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(data.getString(ITEM_ID)));
        ItemStack stack = new ItemStack(item);
        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString(ID, HANGING_SIGN_ID);
        blockEntity.put(FRONT_TEXT, data.getCompound(FRONT_TEXT).copy());
        stack.getOrCreateTag().put(BLOCK_ENTITY_TAG, blockEntity);
        entity.getPersistentData().remove(ENTITY_TAG);
        return stack;
    }

    // ---------------- 内部 ----------------

    /**
     * 构造原版告示牌的 {@code front_text} 复合标签。
     *
     * <p>{@code messages} 存 4 个 JSON 文本；{@code color} / {@code has_glowing_text}
     * 是原版解码这个结构时一并读的字段，缺了会导致放下牌子时文字丢失，所以按默认值一起写上。
     */
    private static CompoundTag frontText(String[] lines) {
        ListTag messages = new ListTag();
        for (int i = 0; i < LINES; i++) {
            String line = i < lines.length && lines[i] != null ? lines[i] : "";
            messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        }
        CompoundTag front = new CompoundTag();
        front.put(MESSAGES, messages);
        front.putString(COLOR, "black");
        front.putBoolean(GLOWING_TEXT, false);
        return front;
    }

    /** 从 front_text 里读出 4 行纯文本；没有 messages 字段说明没写过字 */
    private static String[] readMessages(CompoundTag front) {
        ListTag messages = front.getList(MESSAGES, Tag.TAG_STRING);
        if (messages.isEmpty()) {
            return null;
        }
        String[] lines = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            lines[i] = i < messages.size() ? plainText(messages.getString(i)) : "";
        }
        return lines;
    }

    /** 玩家可能把 messages 改成任意字符串，解析不了就当成纯文本显示 */
    private static String plainText(String json) {
        try {
            return Component.Serializer.fromJson(json).getString();
        } catch (Exception e) {
            return json;
        }
    }
}
