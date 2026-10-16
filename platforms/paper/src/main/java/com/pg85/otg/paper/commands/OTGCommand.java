package com.pg85.otg.paper.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class OTGCommand {

    @SuppressWarnings("UnstableApiUsage")
    public static void register(io.papermc.paper.command.brigadier.Commands commands) {
        LiteralCommandNode<CommandSourceStack> otgCommand = Commands.literal("otg")
                .requires(src -> src.getBukkitSender().hasPermission("otg.cmd"))
                .then(BiomeCommand.register())
                .then(TpCommand.register())
                .then(MapCommand.register())
                .then(PresetCommand.register())
                .then(SettingsCommand.register())
                .then(PackCommand.register())
                .then(RegionCommand.register())
                .then(ExportCommand.register())
                .then(ConfigExportCommand.register())
                .then(EditCommand.register())
                .then(EditCommand.registerFinish())
                .then(EditCommand.registerCancel())
                .then(SpawnCommand.register())
                .then(StructureCommand.register())
                .then(FlushCommand.register())
                .then(DataCommand.register())
                .build();
        // Paper's command source is implemented by the vanilla CommandSourceStack, so the vanilla command tree can be
        // registered as is. Only Brigadier's own argument types are used, which Paper accepts.
        commands.register(toPaperNode(otgCommand), "OpenTerrainGenerator commands");
    }

    @SuppressWarnings("unchecked")
    private static LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack> toPaperNode(LiteralCommandNode<CommandSourceStack> node) {
        return (LiteralCommandNode<io.papermc.paper.command.brigadier.CommandSourceStack>) (Object) node;
    }

    private OTGCommand() {}
}
