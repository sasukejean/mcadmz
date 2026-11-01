package com.mcadmz;

import forge.net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ScouterItem extends Item {
    public ScouterItem(Properties p) { super(p); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof VillagerEntityMCA npc)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            for (Component c : describe(npc)) sp.sendSystemMessage(c);
        }
        return InteractionResult.CONSUME;
    }

    public static java.util.List<Component> describe(VillagerEntityMCA npc) {
        NpcStats s = NpcStats.of(npc);
        java.util.List<Component> out = new java.util.ArrayList<>();
        out.add(Component.literal("[Scouter] " + npc.getName().getString() + " - Nivel " + s.level()
                + " - Poder " + s.power() + " - TP " + s.tp()));
        for (int i = 0; i < NpcStats.KEYS.length; i++) {
            out.add(Component.literal("  " + NpcStats.LABELS[i] + ": " + s.effective(i)
                    + " (herdado " + s.floor(i) + ", investido " + s.invested(i) + ", limite " + s.cap(i) + ")"));
        }
        String techs = s.techniques().isEmpty() ? "nenhuma" : String.join(", ", s.techniques());
        out.add(Component.literal("  Tecnicas: " + techs));
        return out;
    }
}
