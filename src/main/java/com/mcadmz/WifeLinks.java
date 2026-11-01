package com.mcadmz;

import forge.net.conczin.mca.entity.ai.relationship.RelationshipState;
import forge.net.conczin.mca.server.world.data.FamilyTree;
import forge.net.conczin.mca.server.world.data.FamilyTreeNode;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.UUID;

/** Mantem varias esposas: reaplica o vinculo "casada com o jogador" em cada esposa depois de novos casamentos. */
public final class WifeLinks {
    private WifeLinks() {}

    public static void restoreAll(ServerPlayer p) {
        ModData d = ModData.get(p.server);
        FamilyTree tree = FamilyTree.get(p.serverLevel());
        for (UUID wife : d.wivesOf(p.getUUID())) {
            tree.getOrEmpty(wife).ifPresent(n -> force(n, p.getUUID()));
        }
    }

    private static void force(FamilyTreeNode node, UUID player) {
        try {
            Field fp = FamilyTreeNode.class.getDeclaredField("partner");
            fp.setAccessible(true);
            fp.set(node, player);
            Field fs = FamilyTreeNode.class.getDeclaredField("relationshipState");
            fs.setAccessible(true);
            fs.set(node, RelationshipState.MARRIED_TO_PLAYER);
            node.markDirty();
        } catch (Throwable ignored) {
        }
    }
}
