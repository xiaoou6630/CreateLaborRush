package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class AchievementHandler {

    // ====== 成就 ID 常量 ======
    // Layer 1
    public static final ResourceLocation ROOT = new ResourceLocation(CreateLaborRush.MODID, "root");
    public static final ResourceLocation FIRST_WHIP = new ResourceLocation(CreateLaborRush.MODID, "first_whip");
    public static final ResourceLocation SUSTAINABLE = new ResourceLocation(CreateLaborRush.MODID, "sustainable");

    // Layer 2
    public static final ResourceLocation WHIP_10_TIMES = new ResourceLocation(CreateLaborRush.MODID, "whip_10_times");
    public static final ResourceLocation FIRE_ASPECT_1 = new ResourceLocation(CreateLaborRush.MODID, "fire_aspect_1");
    public static final ResourceLocation FIRST_BELL_ENCHANT = new ResourceLocation(CreateLaborRush.MODID, "first_bell_enchant");

    // Layer 3
    public static final ResourceLocation FIRE_ASPECT_2 = new ResourceLocation(CreateLaborRush.MODID, "fire_aspect_2");
    public static final ResourceLocation REBELLION_3_PEOPLE = new ResourceLocation(CreateLaborRush.MODID, "rebellion_3_people");
    public static final ResourceLocation PEACE_AMBASSADOR = new ResourceLocation(CreateLaborRush.MODID, "peace_ambassador");

    // Layer 4
    public static final ResourceLocation LIGHTNING_WHIP = new ResourceLocation(CreateLaborRush.MODID, "lightning_whip");
    public static final ResourceLocation LIGHTNING_BELL = new ResourceLocation(CreateLaborRush.MODID, "lightning_bell");
    public static final ResourceLocation FIRST_REBELLION = new ResourceLocation(CreateLaborRush.MODID, "first_rebellion");

    // Layer 5
    public static final ResourceLocation LIGHTNING_5_WHIP = new ResourceLocation(CreateLaborRush.MODID, "lightning_5_whip");
    public static final ResourceLocation SUPPRESSOR = new ResourceLocation(CreateLaborRush.MODID, "suppressor");
    public static final ResourceLocation REBELLION_LEADER_3 = new ResourceLocation(CreateLaborRush.MODID, "rebellion_leader_3");
    public static final ResourceLocation PEACE_BELL = new ResourceLocation(CreateLaborRush.MODID, "peace_bell");
    public static final ResourceLocation DEMOLITION_SQUAD = new ResourceLocation(CreateLaborRush.MODID, "demolition_squad");

    // Layer 6
    public static final ResourceLocation REBEL_KILLER_3 = new ResourceLocation(CreateLaborRush.MODID, "rebel_killer_3");
    public static final ResourceLocation LIGHTNING_SUPPRESSOR = new ResourceLocation(CreateLaborRush.MODID, "lightning_suppressor");

    // Layer 7
    public static final ResourceLocation ALL_ACHIEVEMENTS = new ResourceLocation(CreateLaborRush.MODID, "all_achievements");

    private AchievementHandler() {
    }

    /** 除"刑部尚书"之外的 18 个成就 */
    private static final List<ResourceLocation> ALL_ACHIEVEMENTS_LIST = List.of(
        FIRST_WHIP, SUSTAINABLE, WHIP_10_TIMES, FIRE_ASPECT_1, FIRST_BELL_ENCHANT,
        FIRE_ASPECT_2, REBELLION_3_PEOPLE, PEACE_AMBASSADOR, LIGHTNING_WHIP, LIGHTNING_BELL,
        FIRST_REBELLION, LIGHTNING_5_WHIP, SUPPRESSOR, REBELLION_LEADER_3, PEACE_BELL,
        DEMOLITION_SQUAD, REBEL_KILLER_3, LIGHTNING_SUPPRESSOR
    );

    /**
     * 授予成就
     */
    public static void grantAchievement(ServerPlayer player, ResourceLocation achievementId) {
        var server = player.getServer();
        if (server == null) return;

        var advancement = server.getAdvancements().getAdvancement(achievementId);
        if (advancement == null) return;

        var playerAdvancements = player.getAdvancements();
        if (!playerAdvancements.getOrStartProgress(advancement).isDone()) {
            playerAdvancements.award(advancement, "impossible");
        }

        // 其余 18 个都达成后自动授予"刑部尚书"。它自己的 criterion 是 minecraft:impossible，
        // 只能靠代码给，所以必须在这里补上，否则"完成所有成就"永远拿不到
        if (!ALL_ACHIEVEMENTS.equals(achievementId) && hasAllAchievements(player)) {
            grantAchievement(player, ALL_ACHIEVEMENTS);
        }
    }

    /** 检查除"刑部尚书"之外的 18 个成就是否都已达成 */
    private static boolean hasAllAchievements(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return false;

        var playerAdvancements = player.getAdvancements();
        for (ResourceLocation id : ALL_ACHIEVEMENTS_LIST) {
            var advancement = server.getAdvancements().getAdvancement(id);
            if (advancement == null) return false;
            if (!playerAdvancements.getOrStartProgress(advancement).isDone()) return false;
        }
        return true;
    }
}