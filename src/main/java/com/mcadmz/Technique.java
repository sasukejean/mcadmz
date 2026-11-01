package com.mcadmz;

public enum Technique {
    KI_BLAST("ki_blast", "Disparo de Ki", 50, 1.0, 40, 24),
    KAMEHAMEHA("kamehameha", "Kamehameha", 300, 3.0, 120, 32),
    KAIOKEN("kaioken", "Kaioken (+40% dano com menos de 50% de vida)", 200, 1.0, 0, 0);

    public final String id;
    public final String label;
    public final int tpCost;
    public final double mult;
    public final int cooldown;
    public final int range;

    Technique(String id, String label, int tpCost, double mult, int cooldown, int range) {
        this.id = id; this.label = label; this.tpCost = tpCost; this.mult = mult;
        this.cooldown = cooldown; this.range = range;
    }

    public static Technique byId(String id) {
        for (Technique t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}
