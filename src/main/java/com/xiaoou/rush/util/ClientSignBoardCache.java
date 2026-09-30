package com.xiaoou.rush.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端缓存的「谁身上挂着牌子、写了什么」。
 *
 * <p>村民的牌子存在实体持久化数据里，而 NeoForge 不会把 {@code getPersistentData()}
 * 同步给客户端，所以服务端在挂上/摘下/玩家开始追踪时补发一份过来（见 SignBoardSyncPayload）。
 * key = 实体 id。
 */
public class ClientSignBoardCache {

    private static final Map<Integer, String[]> SIGNS = new HashMap<>();

    /** lines 为 null 表示牌子被摘掉了 */
    public static void set(int entityId, String[] lines) {
        if (lines == null) {
            SIGNS.remove(entityId);
        } else {
            SIGNS.put(entityId, lines);
        }
    }

    public static String[] get(int entityId) {
        return SIGNS.get(entityId);
    }
}
