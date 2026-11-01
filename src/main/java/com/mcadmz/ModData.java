package com.mcadmz;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Dados do mundo: espelho dos stats (pais podem estar descarregados) e lista de esposas por jogador. */
public class ModData extends SavedData {
    private final Map<UUID, int[]> stats = new HashMap<>();
    private final Map<UUID, List<UUID>> wives = new HashMap<>();

    public static ModData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(ModData::load, ModData::new, "mcadmz_data");
    }

    public int[] getStats(UUID id) { return id == null ? null : stats.get(id); }

    public void putStats(UUID id, int[] v) {
        stats.put(id, v.clone());
        setDirty();
    }

    public List<UUID> wivesOf(UUID player) { return wives.getOrDefault(player, Collections.emptyList()); }

    public boolean isWife(UUID player, UUID npc) { return wivesOf(player).contains(npc); }

    public void addWife(UUID player, UUID npc) {
        List<UUID> l = wives.computeIfAbsent(player, k -> new ArrayList<>());
        if (!l.contains(npc)) l.add(npc);
        setDirty();
    }

    public static ModData load(CompoundTag tag) {
        ModData d = new ModData();
        ListTag st = tag.getList("stats", Tag.TAG_COMPOUND);
        for (int i = 0; i < st.size(); i++) {
            CompoundTag c = st.getCompound(i);
            try { d.stats.put(UUID.fromString(c.getString("id")), c.getIntArray("v")); } catch (Exception ignored) {}
        }
        ListTag wl = tag.getList("wives", Tag.TAG_COMPOUND);
        for (int i = 0; i < wl.size(); i++) {
            CompoundTag c = wl.getCompound(i);
            try {
                UUID p = UUID.fromString(c.getString("id"));
                List<UUID> l = new ArrayList<>();
                ListTag w = c.getList("w", Tag.TAG_STRING);
                for (int k = 0; k < w.size(); k++) l.add(UUID.fromString(w.getString(k)));
                d.wives.put(p, l);
            } catch (Exception ignored) {}
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag st = new ListTag();
        for (Map.Entry<UUID, int[]> e : stats.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.getKey().toString());
            c.putIntArray("v", e.getValue());
            st.add(c);
        }
        tag.put("stats", st);
        ListTag wl = new ListTag();
        for (Map.Entry<UUID, List<UUID>> e : wives.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.getKey().toString());
            ListTag w = new ListTag();
            for (UUID u : e.getValue()) w.add(StringTag.valueOf(u.toString()));
            c.put("w", w);
            wl.add(c);
        }
        tag.put("wives", wl);
        return tag;
    }
}
