package com.xiaoou.rush.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端缓存的服务端附魔钟记录（key=维度:坐标，value={火焰附加等级, 引雷等级}）。
 */
public class ClientBellCache {

    private static Map<String, int[]> bells = new HashMap<>();

    public static void set(Map<String, int[]> data) {
        bells = data;
    }

    public static int[] get(String key) {
        return bells.get(key);
    }
}
