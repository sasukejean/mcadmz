package com.mcadmz;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/** Ponte por reflexao com o DragonMineZ (opcional). Se o mod nao estiver instalado, tudo retorna null/false. */
public final class DmzBridge {
    private static final String[] GETTERS = {"getStrength", "getStrikePower", "getResistance", "getVitality", "getKiPower", "getEnergy"};

    private DmzBridge() {}

    @SuppressWarnings("unchecked")
    private static Object data(Player p) throws Exception {
        Class<?> capCls = Class.forName("com.dragonminez.common.stats.StatsCapability");
        Capability<Object> cap = (Capability<Object>) capCls.getField("INSTANCE").get(null);
        LazyOptional<Object> lo = p.getCapability(cap);
        return lo.resolve().orElse(null);
    }

    public static int[] readPlayer(Player p) {
        try {
            Object d = data(p);
            if (d == null) return null;
            Object stats = d.getClass().getMethod("getStats").invoke(d);
            int[] r = new int[6];
            for (int i = 0; i < 6; i++) {
                r[i] = ((Number) stats.getClass().getMethod(GETTERS[i]).invoke(stats)).intValue();
            }
            return r;
        } catch (Throwable t) {
            return null;
        }
    }

    public static float playerTp(Player p) {
        try {
            Object d = data(p);
            if (d == null) return -1f;
            Object res = d.getClass().getMethod("getResources").invoke(d);
            return ((Number) res.getClass().getMethod("getTrainingPoints").invoke(res)).floatValue();
        } catch (Throwable t) {
            return -1f;
        }
    }

    public static boolean takePlayerTp(Player p, float amount) {
        try {
            Object d = data(p);
            if (d == null) return false;
            Object res = d.getClass().getMethod("getResources").invoke(d);
            float have = ((Number) res.getClass().getMethod("getTrainingPoints").invoke(res)).floatValue();
            if (have < amount) return false;
            res.getClass().getMethod("removeTrainingPoints", float.class).invoke(res, amount);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
