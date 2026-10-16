package com.pg85.otg.paper.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

final class FlushCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /otg flush
        return Commands.literal("flush")
            .executes(FlushCommand::flushCache);
    }

    private static int flushCache(CommandContext<CommandSourceStack> ctx) {
        if (!CommandHelper.requireOTGWorld(ctx)) return 0;
        OTGLog.log(LogLevel.INFO, LogCategory.MAIN, "Unloading BO2/BO3/BO4 files");
        OTG.getEngine().getCustomObjectManager().reloadCustomObjectFiles();
        ctx.getSource().sendSuccess(() -> Component.literal("Objects unloaded."), false);
        return 1;
    }

    private FlushCommand() {}
}
