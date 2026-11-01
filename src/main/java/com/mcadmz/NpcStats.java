package com.mcadmz;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

/** Stats estilo DragonMineZ guardados no NBT persistente do NPC do Comes Alive. */
public final class NpcStats {
    public static final String ROOT = "mcadmz";
    public static final String[] KEYS = {"str", "skp", "res", "vit", "pwr", "ene"};
    public static final String[] LABELS = {"Forca", "Golpe", "Resistencia", "Vitalidade", "Poder de Ki", "Energia"};

    private final VillagerEntityMCA npc;
    private final CompoundTag t;

    private NpcStats(VillagerEntityMCA npc, CompoundTag t) { this.npc = npc; this.t = t; }

    public static NpcStats of(VillagerEntityMCA npc) {
        CompoundTag root = npc.getPersistentData();
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) root.put(ROOT, new CompoundTag());
        return new NpcStats(npc, root.getCompound(ROOT));
    }

    public int base() { return McadmzConfig.BASE_STAT.get(); }
    public int invested(int i) { return t.getInt("inv_" + KEYS[i]); }
    public int floor(int i) { return t.getInt("fl_" + KEYS[i]); }
    public int effective(int i) { return base() + floor(i) + invested(i); }
    public int cap(int i) { return McadmzConfig.STAT_CAP.get() + floor(i); }
    public long tp() { return t.getLong("tp"); }
    public void addTp(long v) { t.putLong("tp", Math.max(0L, tp() + v)); }

    public int totalInvested() {
        int s = 0;
        for (int i = 0; i < KEYS.length; i++) s += invested(i);
        return s;
    }

    public int level() { return 1 + totalInvested() / 6; }

    public long power() {
        long sum = 0;
        for (int i = 0; i < KEYS.length; i++) sum += effective(i);
        return (long) (sum * (1.0 + level() / 10.0));
    }

    public boolean initialized() { return t.getBoolean("init"); }
    public void markInitialized() { t.putBoolean("init", true); }
    public int incTries() { int n = t.getInt("tries") + 1; t.putInt("tries", n); return n; }

    public void setFloor(int[] f) {
        for (int i = 0; i < KEYS.length; i++) t.putInt("fl_" + KEYS[i], f[i]);
    }

    public long nextAttack() { return t.getLong("next_atk"); }
    public void setNextAttack(long v) { t.putLong("next_atk", v); }

    public int[] effectiveArray() {
        int[] r = new int[KEYS.length];
        for (int i = 0; i < r.length; i++) r[i] = effective(i);
        return r;
    }

    public Set<String> techniques() {
        Set<String> s = new HashSet<>();
        ListTag l = t.getList("tech", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) s.add(l.getString(i));
        return s;
    }

    public boolean knows(Technique q) { return techniques().contains(q.id); }

    public void learn(Technique q) {
        if (knows(q)) return;
        ListTag l = t.getList("tech", Tag.TAG_STRING);
        l.add(StringTag.valueOf(q.id));
        t.put("tech", l);
    }

    public Technique bestRanged() {
        Technique best = null;
        for (Technique q : Technique.values()) {
            if (q.range > 0 && knows(q) && (best == null || q.mult > best.mult)) best = q;
        }
        return best;
    }

    public int costFor(int i) {
        int c = McadmzConfig.TP_COST_BASE.get();
        return c + c * invested(i) / 50;
    }

    /** Compra pontos no stat i gastando TP. Retorna quantos pontos foram comprados. */
    public int buy(int i, int points) {
        int bought = 0;
        for (int k = 0; k < points; k++) {
            if (effective(i) >= cap(i)) break;
            int c = costFor(i);
            if (tp() < c) break;
            t.putLong("tp", tp() - c);
            t.putInt("inv_" + KEYS[i], invested(i) + 1);
            bought++;
        }
        if (bought > 0) sync();
        return bought;
    }

    public void autoAllocate() {
        for (int n = 0; n < 100; n++) {
            int pick = -1;
            for (int i = 0; i < KEYS.length; i++) {
                if (effective(i) >= cap(i) || tp() < costFor(i)) continue;
                if (pick < 0 || invested(i) < invested(pick)) pick = i;
            }
            if (pick < 0) break;
            buy(pick, 1);
        }
    }

    public void sync() {
        MinecraftServer s = npc.getServer();
        if (s != null) ModData.get(s).putStats(npc.getUUID(), effectiveArray());
    }
}
