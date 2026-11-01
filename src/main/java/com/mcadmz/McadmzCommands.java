package com.mcadmz;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import forge.net.conczin.mca.entity.VillagerEntityMCA;
import forge.net.conczin.mca.server.world.data.FamilyTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = McaDmz.MODID)
public final class McadmzCommands {
    private McadmzCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        List<String> techIds = new ArrayList<>();
        for (Technique t : Technique.values()) techIds.add(t.id);

        e.getDispatcher().register(Commands.literal("mcadmz")
                .then(Commands.literal("scan")
                        .then(Commands.argument("npc", EntityArgument.entity()).executes(McadmzCommands::scan)))
                .then(Commands.literal("tp")
                        .then(Commands.literal("give")
                                .then(Commands.argument("npc", EntityArgument.entity())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(McadmzCommands::tpGive))))
                        .then(Commands.literal("add").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("npc", EntityArgument.entity())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(McadmzCommands::tpAdd)))))
                .then(Commands.literal("allocate")
                        .then(Commands.argument("npc", EntityArgument.entity())
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.asList(NpcStats.KEYS), b))
                                        .then(Commands.argument("points", IntegerArgumentType.integer(1)).executes(McadmzCommands::allocate)))))
                .then(Commands.literal("teach")
                        .then(Commands.argument("npc", EntityArgument.entity())
                                .then(Commands.argument("technique", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(techIds, b))
                                        .executes(McadmzCommands::teach))))
                .then(Commands.literal("techniques").executes(McadmzCommands::listTech))
                .then(Commands.literal("wives").executes(McadmzCommands::wives)));
    }

    private static VillagerEntityMCA npc(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Entity en = EntityArgument.getEntity(c, "npc");
        if (en instanceof VillagerEntityMCA n) return n;
        throw new SimpleCommandExceptionType(Component.literal("O alvo nao e um NPC do Comes Alive.")).create();
    }

    private static void say(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
    }

    private static int scan(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        VillagerEntityMCA n = npc(c);
        for (Component line : ScouterItem.describe(n)) c.getSource().sendSuccess(() -> line, false);
        return 1;
    }

    private static int tpAdd(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        VillagerEntityMCA n = npc(c);
        int amt = IntegerArgumentType.getInteger(c, "amount");
        NpcStats.of(n).addTp(amt);
        say(c, "+" + amt + " TP para " + n.getName().getString());
        return 1;
    }

    private static int tpGive(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        VillagerEntityMCA n = npc(c);
        ServerPlayer p = c.getSource().getPlayerOrException();
        int amt = IntegerArgumentType.getInteger(c, "amount");
        if (DmzBridge.playerTp(p) < 0) {
            throw new SimpleCommandExceptionType(Component.literal("DragonMineZ nao encontrado: nao ha TP de jogador para transferir.")).create();
        }
        if (!DmzBridge.takePlayerTp(p, amt)) {
            throw new SimpleCommandExceptionType(Component.literal("Voce nao tem TP suficiente.")).create();
        }
        NpcStats.of(n).addTp(amt);
        say(c, "Voce transferiu " + amt + " TP para " + n.getName().getString());
        return 1;
    }

    private static int allocate(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        VillagerEntityMCA n = npc(c);
        String key = StringArgumentType.getString(c, "stat");
        int pts = IntegerArgumentType.getInteger(c, "points");
        int idx = Arrays.asList(NpcStats.KEYS).indexOf(key.toLowerCase());
        if (idx < 0) throw new SimpleCommandExceptionType(Component.literal("Stat invalido. Use: " + String.join(", ", NpcStats.KEYS))).create();
        NpcStats s = NpcStats.of(n);
        int bought = s.buy(idx, pts);
        say(c, NpcStats.LABELS[idx] + ": +" + bought + " (agora " + s.effective(idx) + "), TP restante " + s.tp());
        return bought;
    }

    private static int teach(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        VillagerEntityMCA n = npc(c);
        Technique t = Technique.byId(StringArgumentType.getString(c, "technique"));
        if (t == null) throw new SimpleCommandExceptionType(Component.literal("Tecnica desconhecida. Use /mcadmz techniques")).create();
        NpcStats s = NpcStats.of(n);
        if (s.knows(t)) throw new SimpleCommandExceptionType(Component.literal("O NPC ja conhece essa tecnica.")).create();
        if (s.tp() < t.tpCost) {
            throw new SimpleCommandExceptionType(Component.literal("TP insuficiente: precisa de " + t.tpCost + ", tem " + s.tp())).create();
        }
        s.addTp(-t.tpCost);
        s.learn(t);
        say(c, n.getName().getString() + " aprendeu " + t.label);
        return 1;
    }

    private static int listTech(CommandContext<CommandSourceStack> c) {
        for (Technique t : Technique.values()) say(c, t.id + " - " + t.label + " (" + t.tpCost + " TP)");
        return 1;
    }

    private static int wives(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<UUID> ws = ModData.get(p.server).wivesOf(p.getUUID());
        if (ws.isEmpty()) { say(c, "Nenhuma esposa extra registrada."); return 0; }
        FamilyTree tree = FamilyTree.get(p.serverLevel());
        for (UUID w : ws) {
            String name = tree.getOrEmpty(w).map(x -> x.getName()).orElse(w.toString());
            say(c, "- " + name);
        }
        return ws.size();
    }
}
