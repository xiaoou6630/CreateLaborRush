package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AchievementHandler {

    // ====== 成就 ID 常量 ======
    // Layer 1
    public static final ResourceLocation ROOT = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "root");
    public static final ResourceLocation FIRST_WHIP = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "first_whip");
    public static final ResourceLocation SUSTAINABLE = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "sustainable");

    // Layer 2
    public static final ResourceLocation WHIP_10_TIMES = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "whip_10_times");
    public static final ResourceLocation FIRE_ASPECT_1 = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "fire_aspect_1");
    public static final ResourceLocation FIRST_BELL_ENCHANT = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "first_bell_enchant");

    // Layer 3
    public static final ResourceLocation FIRE_ASPECT_2 = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "fire_aspect_2");
    public static final ResourceLocation REBELLION_3_PEOPLE = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "rebellion_3_people");
    public static final ResourceLocation PEACE_AMBASSADOR = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "peace_ambassador");

    // Layer 4
    public static final ResourceLocation LIGHTNING_WHIP = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "lightning_whip");
    public static final ResourceLocation LIGHTNING_BELL = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "lightning_bell");
    public static final ResourceLocation FIRST_REBELLION = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "first_rebellion");

    // Layer 5
    public static final ResourceLocation LIGHTNING_5_WHIP = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "lightning_5_whip");
    public static final ResourceLocation SUPPRESSOR = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "suppressor");
    public static final ResourceLocation REBELLION_LEADER_3 = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "rebellion_leader_3");
    public static final ResourceLocation PEACE_BELL = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "peace_bell");
    public static final ResourceLocation DEMOLITION_SQUAD = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "demolition_squad");

    // Layer 6
    public static final ResourceLocation REBEL_KILLER_3 = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "rebel_killer_3");
    public static final ResourceLocation LIGHTNING_SUPPRESSOR = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "lightning_suppressor");

    // Layer 7
    public static final ResourceLocation ALL_ACHIEVEMENTS = ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "all_achievements");

    private AchievementHandler() {
    }

    /**
     * 授予成就
     */
    public static void grantAchievement(ServerPlayer player, ResourceLocation achievementId) {
        var server = player.getServer();
        if (server == null) return;

        var advancement = server.getAdvancements().get(achievementId);
        if (advancement == null) return;

        var playerAdvancements = player.getAdvancements();
        if (!playerAdvancements.getOrStartProgress(advancement).isDone()) {
            playerAdvancements.award(advancement, "impossible");
        }
    }
}