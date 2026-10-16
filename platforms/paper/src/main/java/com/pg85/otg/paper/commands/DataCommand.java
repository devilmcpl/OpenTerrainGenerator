package com.pg85.otg.paper.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

final class DataCommand {

    private static final String[] DATA_TYPES = new String[]
        { "biome", "block", "entity", "sound", "particle", "configured_feature" };

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /otg data <type>
        return Commands.literal("data")
            .executes(ctx -> execute(ctx.getSource(), ""))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DATA_TYPES, builder))
                .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "type"))));
    }

    private static int execute(CommandSourceStack source, String type) {
        Registry<?> registry;
        switch (type.toLowerCase(Locale.ROOT)) {
            case "biome" -> registry = source.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
            case "block" -> registry = BuiltInRegistries.BLOCK;
            case "entity" -> registry = BuiltInRegistries.ENTITY_TYPE;
            case "sound" -> registry = BuiltInRegistries.SOUND_EVENT;
            case "particle" -> registry = BuiltInRegistries.PARTICLE_TYPE;
            case "configured_feature" -> registry = source.getServer().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
            default -> {
                source.sendFailure(Component.literal("Usage: /otg data <" + String.join("|", DATA_TYPES) + ">"));
                return 0;
            }
        }

        List<Identifier> keys = registry.keySet().stream().sorted().toList();
        File folder = OTG.getEngine().getOTGRootFolder().resolve("output").toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File output = new File(folder, ("data-output-" + type + ".txt").toLowerCase(Locale.ROOT));
        try (FileWriter writer = new FileWriter(output)) {
            for (Identifier key : keys) {
                writer.write(key.toString() + "\n");
            }
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write " + output.getPath()));
            OTGLog.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("File exported as " + output.getPath()), true);
        return 1;
    }

    private DataCommand() {}
}
