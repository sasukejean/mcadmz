package com.mcadmz;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.entity.ai.relationship.AgeState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Clique direito em um NPC adulto solteiro: casa com ele sem limite de esposas. */
public class ExtraRingItem extends Item {
    public ExtraRingItem(Properties p) { super(p); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof VillagerEntityMCA npc)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (npc.getAgeState() != AgeState.ADULT) {
            sp.sendSystemMessage(Component.literal("Essa pessoa ainda nao e adulta."));
            return InteractionResult.FAIL;
        }
        if (npc.getRelationships().isMarried()) {
            sp.sendSystemMessage(Component.literal("Essa pessoa ja e casada."));
            return InteractionResult.FAIL;
        }
        try {
            npc.getRelationships().marry(sp);
        } catch (Throwable t) {
            sp.sendSystemMessage(Component.literal("Falha ao casar (veja o log): " + t));
            return InteractionResult.FAIL;
        }
        ModData.get(sp.server).addWife(sp.getUUID(), npc.getUUID());
        WifeLinks.restoreAll(sp);
        if (!sp.getAbilities().instabuild) stack.shrink(1);
        ((ServerLevel) npc.level()).sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.8, npc.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        int n = ModData.get(sp.server).wivesOf(sp.getUUID()).size();
        sp.sendSystemMessage(Component.literal("Voce se casou com " + npc.getName().getString() + "! Esposas extras: " + n));
        return InteractionResult.CONSUME;
    }
}
