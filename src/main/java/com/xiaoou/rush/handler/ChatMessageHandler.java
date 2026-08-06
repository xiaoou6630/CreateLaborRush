package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class ChatMessageHandler {

    private static final Random RANDOM = new Random();

    private static final String[] REBELLION_SLOGANS = {
        "罢工！罢工！罢工！",
        "我们不是机器！",
        "是时候站起来了！",
        "我们要尊严！",
        "兄弟们，跟我上！"
    };

    private static final String[] CONTAGION_SLOGANS = {
        "团结就是力量！",
        "我们人多！",
        "一个都不能少！",
        "跟我走！"
    };

    private static final String[] LEADER_SLOGANS = {
        "我就是领袖！",
        "跟我走！",
        "听我指挥！"
    };

    private ChatMessageHandler() {
    }

    /**
     * 起义触发时随机发送一条口号
     */
    public static void sendRebellionChat(ServerLevel level, BlockPos pos) {
        String text = REBELLION_SLOGANS[RANDOM.nextInt(REBELLION_SLOGANS.length)];
        level.getServer().getPlayerList().getPlayers().forEach(
            player -> player.sendSystemMessage(Component.literal(text))
        );
        CreateLaborRush.LOGGER.info("起义口号 [{}] 于 {}", text, pos);
    }

    /**
     * 传染发生时随机发送一条口号
     */
    public static void sendContagionChat(ServerLevel level, BlockPos pos) {
        String text = CONTAGION_SLOGANS[RANDOM.nextInt(CONTAGION_SLOGANS.length)];
        level.getServer().getPlayerList().getPlayers().forEach(
            player -> player.sendSystemMessage(Component.literal(text))
        );
        CreateLaborRush.LOGGER.info("传染口号 [{}] 于 {}", text, pos);
    }

    /**
     * 首领登场时随机发送一条口号
     */
    public static void sendLeaderChat(ServerLevel level, BlockPos pos) {
        String text = LEADER_SLOGANS[RANDOM.nextInt(LEADER_SLOGANS.length)];
        level.getServer().getPlayerList().getPlayers().forEach(
            player -> player.sendSystemMessage(Component.literal(text))
        );
        CreateLaborRush.LOGGER.info("首领口号 [{}] 于 {}", text, pos);
    }
}