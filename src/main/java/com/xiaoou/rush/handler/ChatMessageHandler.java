package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;

public class ChatMessageHandler {

    private static final Random RANDOM = new Random();

    private static final String[] REBELLION_SLOGAN_KEYS = {
        "chat.createlaborrush.slogan.rebellion.0",
        "chat.createlaborrush.slogan.rebellion.1",
        "chat.createlaborrush.slogan.rebellion.2",
        "chat.createlaborrush.slogan.rebellion.3",
        "chat.createlaborrush.slogan.rebellion.4"
    };

    private static final String[] CONTAGION_SLOGAN_KEYS = {
        "chat.createlaborrush.slogan.contagion.0",
        "chat.createlaborrush.slogan.contagion.1",
        "chat.createlaborrush.slogan.contagion.2",
        "chat.createlaborrush.slogan.contagion.3"
    };

    private static final String[] LEADER_SLOGAN_KEYS = {
        "chat.createlaborrush.slogan.leader.0",
        "chat.createlaborrush.slogan.leader.1",
        "chat.createlaborrush.slogan.leader.2"
    };

    private ChatMessageHandler() {
    }

    /**
     * 起义触发时随机发送一条口号
     */
    public static void sendRebellionChat(ServerLevel level, BlockPos pos) {
        String key = REBELLION_SLOGAN_KEYS[RANDOM.nextInt(REBELLION_SLOGAN_KEYS.length)];
        level.getServer().getPlayerList().getPlayers().forEach(
            player -> player.sendSystemMessage(Component.translatable(key))
        );
        CreateLaborRush.LOGGER.info("起义口号 [{}] 于 {}", key, pos);
    }

    /**
     * 传染发生时随机发送一条口号
     */
    public static void sendContagionChat(ServerLevel level, BlockPos pos) {
        String key = CONTAGION_SLOGAN_KEYS[RANDOM.nextInt(CONTAGION_SLOGAN_KEYS.length)];
        level.getServer().getPlayerList().getPlayers().forEach(
            player -> player.sendSystemMessage(Component.translatable(key))
        );
        CreateLaborRush.LOGGER.info("传染口号 [{}] 于 {}", key, pos);
    }

    /**
     * 首领登场时随机发送一条口号
     */
    public static void sendLeaderChat(ServerLevel level, BlockPos pos) {
        String key = LEADER_SLOGAN_KEYS[RANDOM.nextInt(LEADER_SLOGAN_KEYS.length)];
        level.getServer().getPlayerList().getPlayers().forEach(
            player -> player.sendSystemMessage(Component.translatable(key))
        );
        CreateLaborRush.LOGGER.info("首领口号 [{}] 于 {}", key, pos);
    }
}
