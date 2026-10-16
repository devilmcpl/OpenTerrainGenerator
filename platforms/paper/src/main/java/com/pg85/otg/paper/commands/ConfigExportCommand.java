package com.pg85.otg.paper.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.customobject.creator.ObjectType;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.materials.LocalMaterialData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.function.BiFunction;

final class ConfigExportCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("configexport")
            .then(Commands.literal("preset")
                .then(Commands.argument("preset", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> ObjectSuggestions.suggestPresets(builder, true))
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        options.preset = StringArgumentType.getString(ctx, "preset");
                        src.sendSuccess(() -> Component.literal(options.preset + " set as your default preset"), false);
                        return 1;
                    }))))
            .then(Commands.literal("filter")
                .then(Commands.literal("add")
                    .then(Commands.argument("block", StringArgumentType.greedyString())
                        .executes(ctx -> withOptions(ctx, (src, options) -> {
                            LocalMaterialData material = readMaterial(src, StringArgumentType.getString(ctx, "block"));
                            if (material == null) return 0;
                            options.filter.add(material);
                            src.sendSuccess(() -> Component.literal("Block " + material.getRegistryName() + " added to filter"), false);
                            return 1;
                        }))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("block", StringArgumentType.greedyString())
                        .executes(ctx -> withOptions(ctx, (src, options) -> {
                            LocalMaterialData material = readMaterial(src, StringArgumentType.getString(ctx, "block"));
                            if (material == null) return 0;
                            options.filter.remove(material);
                            src.sendSuccess(() -> Component.literal("Block " + material.getRegistryName() + " removed from filter"), false);
                            return 1;
                        }))))
                .then(Commands.literal("list")
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        src.sendSuccess(() -> Component.literal("Current exclusion filter:").withStyle(ChatFormatting.GOLD), false);
                        options.filter.forEach(item ->
                            src.sendSuccess(() -> Component.literal("- " + item.getRegistryName()).withStyle(ChatFormatting.GREEN), false));
                        return 1;
                    }))))
            .then(Commands.literal("type")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> ObjectSuggestions.suggestTypes(builder))
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        try {
                            options.objectType = ObjectType.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            src.sendFailure(Component.literal("Unknown object type " + StringArgumentType.getString(ctx, "type")));
                            return 0;
                        }
                        src.sendSuccess(() -> Component.literal(options.objectType.getType() + " set as your default object type"), false);
                        return 1;
                    }))))
            .then(Commands.literal("centerblock")
                .then(Commands.argument("block", StringArgumentType.greedyString())
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        LocalMaterialData material = readMaterial(src, StringArgumentType.getString(ctx, "block"));
                        if (material == null) return 0;
                        options.centerBlock = material;
                        src.sendSuccess(() -> Component.literal(material.getRegistryName() + " set as center block"), false);
                        return 1;
                    }))))
            .then(Commands.literal("template")
                .then(Commands.argument("template", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        return ObjectSuggestions.suggestTemplates(builder, player == null ? "global" : ExportOptions.get(player.getUUID()).preset);
                    })
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        options.template = StringArgumentType.getString(ctx, "template");
                        src.sendSuccess(() -> Component.literal(options.template + " set as your default template"), false);
                        return 1;
                    }))))
            .then(Commands.literal("overwrite")
                .then(Commands.argument("overwrite", BoolArgumentType.bool())
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        options.overwrite = BoolArgumentType.getBool(ctx, "overwrite");
                        src.sendSuccess(() -> Component.literal("Overwriting existing files " + (options.overwrite ? "enabled" : "disabled")), false);
                        return 1;
                    }))))
            .then(Commands.literal("global")
                .then(Commands.argument("global", BoolArgumentType.bool())
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        options.exportToGlobal = BoolArgumentType.getBool(ctx, "global");
                        src.sendSuccess(() -> Component.literal("Writing objects to GlobalObjects " + (options.exportToGlobal ? "enabled" : "disabled")), false);
                        return 1;
                    }))))
            .then(Commands.literal("includeair")
                .then(Commands.argument("includeair", BoolArgumentType.bool())
                    .executes(ctx -> withOptions(ctx, (src, options) -> {
                        options.includeAir = BoolArgumentType.getBool(ctx, "includeair");
                        src.sendSuccess(() -> Component.literal("Exporting air blocks " + (options.includeAir ? "enabled" : "disabled")), false);
                        return 1;
                    }))));
    }

    private static int withOptions(CommandContext<CommandSourceStack> ctx, BiFunction<CommandSourceStack, ExportOptions, Integer> action) {
        ServerPlayer player = CommandHelper.requirePlayer(ctx.getSource());
        if (player == null) return 0;
        return action.apply(ctx.getSource(), ExportOptions.get(player.getUUID()));
    }

    private static LocalMaterialData readMaterial(CommandSourceStack src, String input) {
        try {
            LocalMaterialData material = OTGMaterialReader.get().readMaterial(input);
            if (material != null) {
                return material;
            }
        } catch (InvalidConfigException ignored) {}
        src.sendFailure(Component.literal("Unknown block " + input));
        return null;
    }

    private ConfigExportCommand() {}
}
