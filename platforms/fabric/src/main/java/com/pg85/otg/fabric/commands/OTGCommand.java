package com.pg85.otg.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class OTGCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext buildContext,
                                Commands.CommandSelection selection) {
        dispatcher.register(
            Commands.literal("otg")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(BiomeCommand.register())
                .then(TpCommand.register())
                .then(MapCommand.register())
                .then(PresetCommand.register())
                .then(SettingsCommand.register())
                .then(PackCommand.register())
        );
    }

    private OTGCommand() {}
}
