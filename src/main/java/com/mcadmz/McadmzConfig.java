package com.mcadmz;

import net.minecraftforge.common.ForgeConfigSpec;

public final class McadmzConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue AUTO_ALLOCATE;
    public static final ForgeConfigSpec.BooleanValue COMBAT_HELP;
    public static final ForgeConfigSpec.IntValue BASE_STAT;
    public static final ForgeConfigSpec.IntValue STAT_CAP;
    public static final ForgeConfigSpec.IntValue TP_COST_BASE;
    public static final ForgeConfigSpec.IntValue TP_PASSIVE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue INHERIT_FRACTION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        AUTO_ALLOCATE = b.comment("NPCs gastam TP sozinhos distribuindo entre os stats").define("autoAllocate", true);
        COMBAT_HELP = b.comment("NPCs da familia ajudam o jogador em combate").define("combatHelp", true);
        BASE_STAT = b.comment("Valor inicial de cada stat (nivel 1)").defineInRange("baseStat", 5, 1, 1000);
        STAT_CAP = b.comment("Limite base de cada stat (filhos ganham o piso herdado por cima)").defineInRange("statCap", 5000, 10, 1000000);
        TP_COST_BASE = b.comment("Custo base em TP por ponto de stat").defineInRange("tpCostBase", 20, 1, 100000);
        TP_PASSIVE_SECONDS = b.comment("A cada quantos segundos o NPC ganha 1 TP passivo (0 = desliga)").defineInRange("tpPassiveSeconds", 30, 0, 100000);
        INHERIT_FRACTION = b.comment("Fracao da SOMA dos stats dos pais herdada pelo filho").defineInRange("inheritFraction", 0.10, 0.0, 10.0);
        SPEC = b.build();
    }

    private McadmzConfig() {}
}
