package com.mcadmz;

import forge.net.conczin.mca.entity.ai.relationship.RelationshipState;
import forge.net.conczin.mca.server.world.data.FamilyTree;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Restaura os vínculos de esposas extras usando a API pública do Comes Alive.
 * updatePartner também marca a FamilyTree como modificada para persistência.
 */
public final class WifeLinks {
    private WifeLinks() {}

    public static void restoreAll(ServerPlayer player) {
        ModData data = ModData.get(player.server);
        FamilyTree tree = FamilyTree.get(player.serverLevel());

        for (UUID wifeId : data.wivesOf(player.getUUID())) {
            tree.getOrEmpty(wifeId).ifPresent(node ->
                    node.updatePartner(player, RelationshipState.MARRIED_TO_PLAYER));
        }
    }
}
