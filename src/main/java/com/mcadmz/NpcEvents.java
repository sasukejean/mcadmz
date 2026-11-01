package com.mcadmz;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.entity.ai.relationship.AgeState;
import forge.net.conczin.mca.server.world.data.FamilyTreeNode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = McaDmz.MODID)
public final class NpcEvents {
    private static final UUID HP_UUID = UUID.fromString("5b0f2d4e-7a31-4c55-9a52-3d1b6f0c1a01");
    private static boolean internal = false;

    private NpcEvents() {}

    // ---------- tick do NPC ----------
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof VillagerEntityMCA npc)) return;
        if (npc.level().isClientSide) return;
        int t = npc.tickCount;
        if (t % 10 != 0) return;

        NpcStats s = NpcStats.of(npc);
        combat(npc, s);

        if (t % 20 != 0) return;
        if (!s.initialized()) {
            inherit(npc, s);
            return;
        }
        int sec = McadmzConfig.TP_PASSIVE_SECONDS.get();
        if (sec > 0 && t % (20 * sec) == 0 && npc.getAgeState() == AgeState.ADULT) s.addTp(1);
        if (McadmzConfig.AUTO_ALLOCATE.get()) s.autoAllocate();
        applyAttributes(npc, s);
    }

    // ---------- heranca: 10% da SOMA dos stats dos dois pais; nivel 1 (investido = 0) ----------
    private static void inherit(VillagerEntityMCA npc, NpcStats s) {
        MinecraftServer srv = npc.getServer();
        if (srv == null) return;
        FamilyTreeNode node = npc.getRelationships().getFamilyEntry();
        UUID f = node.father();
        UUID m = node.mother();
        int tries = s.incTries();
        if (f == null && m == null) {
            if (tries >= 10) { s.markInitialized(); s.sync(); }
            return;
        }
        int base = s.base();
        int[] fs = parentStats(srv, f);
        int[] ms = parentStats(srv, m);
        double frac = McadmzConfig.INHERIT_FRACTION.get();
        int[] floor = new int[NpcStats.KEYS.length];
        for (int i = 0; i < floor.length; i++) {
            int a = fs != null ? fs[i] : base;
            int b = ms != null ? ms[i] : base;
            floor[i] = (int) Math.round(frac * (a + b));
        }
        s.setFloor(floor);
        s.markInitialized();
        s.sync();
    }

    private static int[] parentStats(MinecraftServer srv, UUID id) {
        if (id == null) return null;
        ServerPlayer p = srv.getPlayerList().getPlayer(id);
        if (p != null) {
            int[] r = DmzBridge.readPlayer(p);
            if (r != null) return r;
        }
        return ModData.get(srv).getStats(id);
    }

    // ---------- atributos ----------
    private static void applyAttributes(VillagerEntityMCA npc, NpcStats s) {
        AttributeInstance hp = npc.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        double bonus = (s.effective(3) - s.base()) * 0.5;
        AttributeModifier old = hp.getModifier(HP_UUID);
        if (old != null && Math.abs(old.getAmount() - bonus) < 0.01) return;
        if (old != null) hp.removeModifier(HP_UUID);
        if (bonus > 0) {
            hp.addPermanentModifier(new AttributeModifier(HP_UUID, "mcadmz_vit", bonus, AttributeModifier.Operation.ADDITION));
        }
    }

    // ---------- dano ----------
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!internal && e.getSource().getEntity() instanceof VillagerEntityMCA atk) {
            NpcStats s = NpcStats.of(atk);
            e.setAmount(e.getAmount() + (float) ((s.effective(0) - s.base()) * 0.3));
        }
        if (e.getEntity() instanceof VillagerEntityMCA def) {
            NpcStats s = NpcStats.of(def);
            double res = Math.max(0, s.effective(2) - s.base());
            e.setAmount((float) (e.getAmount() * 100.0 / (100.0 + res * 1.5)));
        }
    }

    // ---------- TP por kills ----------
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof Player || dead instanceof Villager) return;
        long reward = Math.max(1L, (long) (dead.getMaxHealth() / 2.0f));
        if (e.getSource().getEntity() instanceof VillagerEntityMCA npc) {
            NpcStats.of(npc).addTp(reward);
        } else if (e.getSource().getEntity() instanceof ServerPlayer p) {
            AABB box = p.getBoundingBox().inflate(24);
            for (VillagerEntityMCA n : p.level().getEntitiesOfClass(VillagerEntityMCA.class, box)) {
                if (isFamilyOf(n, p)) NpcStats.of(n).addTp(Math.max(1L, reward / 2));
            }
        }
    }

    // ---------- jogador ----------
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 200 != 0) return;
        int[] r = DmzBridge.readPlayer(p);
        if (r != null) ModData.get(p.server).putStats(p.getUUID(), r);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) WifeLinks.restoreAll(p);
    }

    // ---------- familia / combate ----------
    public static boolean isFamilyOf(VillagerEntityMCA npc, Player p) {
        FamilyTreeNode n = npc.getRelationships().getFamilyEntry();
        UUID id = p.getUUID();
        if (id.equals(n.partner()) || id.equals(n.father()) || id.equals(n.mother())) return true;
        MinecraftServer srv = npc.getServer();
        return srv != null && ModData.get(srv).isWife(id, npc.getUUID());
    }

    private static void combat(VillagerEntityMCA npc, NpcStats s) {
        if (!McadmzConfig.COMBAT_HELP.get()) return;
        if (npc.getAgeState().ordinal() < AgeState.TEEN.ordinal()) return;
        ServerLevel lvl = (ServerLevel) npc.level();
        Player owner = null;
        for (Player p : lvl.players()) {
            if (p.distanceToSqr(npc) <= 24 * 24 && isFamilyOf(npc, p)) { owner = p; break; }
        }
        if (owner == null) return;

        LivingEntity target = null;
        LivingEntity a = owner.getLastHurtByMob();
        if (a != null && a.isAlive() && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100) target = a;
        else {
            LivingEntity b = owner.getLastHurtMob();
            if (b != null && b.isAlive() && owner.tickCount - owner.getLastHurtMobTimestamp() < 100) target = b;
        }
        if (target == null || target instanceof Player || target instanceof VillagerEntityMCA) return;
        if (!(target instanceof Enemy) && target != a) return;
        if (npc.distanceToSqr(target) > 32 * 32) return;

        long now = lvl.getGameTime();
        double dist = npc.distanceTo(target);
        npc.getLookControl().setLookAt(target);
        if (now < s.nextAttack()) return;

        if (dist <= 3.0) {
            double dmg = 2 + (s.effective(0) - s.base()) * 0.35 + (s.effective(1) - s.base()) * 0.15;
            hurtInternal(target, npc, (float) dmg);
            s.setNextAttack(now + 20);
            return;
        }
        Technique tech = s.bestRanged();
        if (tech != null && dist <= tech.range) {
            double dmg = (2 + (s.effective(4) - s.base()) * 0.5) * tech.mult;
            if (s.knows(Technique.KAIOKEN) && npc.getHealth() < npc.getMaxHealth() * 0.5f) dmg *= 1.4;
            beam(lvl, npc, target, tech);
            hurtInternal(target, npc, (float) dmg);
            double red = Math.min(0.5, (s.effective(5) - s.base()) / 2000.0);
            s.setNextAttack(now + Math.max(20, (int) (tech.cooldown * (1.0 - red))));
        } else {
            npc.getNavigation().moveTo(target, 1.2);
        }
    }

    private static void hurtInternal(LivingEntity target, LivingEntity src, float amount) {
        internal = true;
        try {
            target.hurt(src.damageSources().mobAttack(src), amount);
        } finally {
            internal = false;
        }
        if (target instanceof Mob m) m.setTarget(src);
    }

    private static void beam(ServerLevel lvl, LivingEntity from, LivingEntity to, Technique t) {
        Vec3 a = from.getEyePosition().add(0, -0.2, 0);
        Vec3 b = to.getEyePosition();
        Vec3 d = b.subtract(a);
        int steps = Math.max(1, (int) (d.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.add(d.scale(i / (double) steps));
            lvl.sendParticles(t == Technique.KAMEHAMEHA ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                    p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
