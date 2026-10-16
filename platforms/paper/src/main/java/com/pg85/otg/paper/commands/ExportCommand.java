package com.pg85.otg.paper.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.customobject.CustomObjectLoader;
import com.pg85.otg.customobject.creator.ObjectCreator;
import com.pg85.otg.customobject.creator.ObjectType;
import com.pg85.otg.customobject.structures.StructuredCustomObject;
import com.pg85.otg.customobject.util.Corner;
import com.pg85.otg.paper.commands.region.Region;
import com.pg85.otg.paper.commands.region.RegionSelections;
import com.pg85.otg.paper.gen.PaperLevelRegion;
import com.pg85.otg.paper.util.PaperNBTHelper;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

final class ExportCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /otg export <name> [type] [preset] [template]
        return Commands.literal("export")
            .executes(ExportCommand::help)
            .then(Commands.literal("help")
                .executes(ExportCommand::help))
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(ExportCommand::execute)
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> ObjectSuggestions.suggestTypes(builder))
                    .executes(ExportCommand::execute)
                    .then(Commands.argument("preset", StringArgumentType.string())
                        .suggests((ctx, builder) -> ObjectSuggestions.suggestPresets(builder, true))
                        .executes(ExportCommand::execute)
                        .then(Commands.argument("template", StringArgumentType.word())
                            .suggests((ctx, builder) -> ObjectSuggestions.suggestTemplates(builder, StringArgumentType.getString(ctx, "preset")))
                            .executes(ExportCommand::execute)))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = CommandHelper.requirePlayer(source);
        if (player == null) return 0;

        try {
            ExportOptions options = ExportOptions.get(player.getUUID());

            String objectName = StringArgumentType.getString(context, "name");
            ObjectType type = options.objectType;
            String presetName = options.preset;
            String templateName = options.template;
            boolean isGlobal = options.exportToGlobal;
            try {
                type = ObjectType.valueOf(StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT));
                presetName = StringArgumentType.getString(context, "preset");
                templateName = StringArgumentType.getString(context, "template");
            } catch (IllegalArgumentException ignored) {} // We can deal with any of these not being there

            if (presetName.equalsIgnoreCase("global")) {
                // Set folder name to default, in case we need fallback settings
                presetName = OTG.getEngine().getPresetLoader().getDefaultPresetFolderName();
                isGlobal = true;
            }

            if (type == ObjectType.BO2) {
                source.sendFailure(Component.literal("Cannot export BO2 objects"));
                return 0;
            }

            Region region = RegionSelections.getSelection(player.getBukkitEntity());
            if (region == null) {
                source.sendFailure(Component.literal("Please define a region with /otg region mark, or WorldEdit"));
                return 0;
            }

            Preset preset = ObjectCommandUtils.getPresetOrDefault(presetName);
            if (preset == null) {
                source.sendFailure(Component.literal("Could not find preset " + presetName));
                return 0;
            }

            Path objectPath = ObjectCommandUtils.getObjectFolderPath(isGlobal ? null : preset.getPresetFolder());
            if (!options.overwrite && type.getObjectFilePathFromName(objectName, objectPath).toFile().exists()) {
                source.sendFailure(Component.literal("File already exists, use /otg configexport overwrite true to overwrite it"));
                return 0;
            }

            Corner lowCorner = region.getMin();
            Corner highCorner = region.getMax();
            Corner center = region.getCenter() != null ? region.getCenter() :
                new Corner(
                    (highCorner.x() - lowCorner.x()) / 2 + lowCorner.x(),
                    Math.min(lowCorner.y(), highCorner.y()),
                    (highCorner.z() - lowCorner.z()) / 2 + lowCorner.z());

            // Fetch template or default settings
            File templateFile = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getTemplateFileForPreset(
                preset.getFolderName(), templateName, OTG.getEngine().getOTGRootFolder());
            CustomObjectLoader loader = OTG.getEngine().getCustomObjectManager().getObjectLoaders().get(type.getType().toLowerCase(Locale.ROOT));
            CustomObject template = loader.loadFromFile(templateName,
                templateFile != null ? templateFile : new File(type.getFileNameForTemplate(templateName)));

            // Initialize the settings
            if (!(template instanceof StructuredCustomObject structuredTemplate) || !template.onEnable(preset.getFolderName(), OTG.getEngine().getOTGRootFolder())) {
                source.sendFailure(Component.literal("Failed to load template \"" + templateName + "\""));
                return 0;
            }

            // Create a new BO from our settings
            StructuredCustomObject object = ObjectCreator.create(
                type,
                lowCorner,
                highCorner,
                center,
                options.centerBlock,
                objectName,
                options.includeAir,
                ObjectCommandUtils.isOutsideBounds(region, type),
                false,
                objectPath,
                PaperLevelRegion.create(source.getLevel(), preset, player.blockPosition()),
                new PaperNBTHelper(),
                null,
                structuredTemplate.getConfig(),
                preset.getFolderName(),
                OTG.getEngine().getOTGRootFolder(),
                OTG.getEngine().getCustomObjectManager(),
                OTGMaterialReader.get(),
                OTG.getEngine().getCustomObjectResourcesManager(),
                OTG.getEngine().getModLoadedChecker(),
                options.filter
            );

            // Send feedback, and register the object for immediate use
            if (object != null) {
                if (isGlobal) {
                    OTG.getEngine().getCustomObjectManager().registerGlobalObject(object, object.getConfig().getFile());
                } else {
                    OTG.getEngine().getCustomObjectManager().getGlobalObjects()
                        .addObjectToPreset(preset.getFolderName(), object.getName().toLowerCase(Locale.ROOT), object.getConfig().getFile(), object);
                }
                String createdName = objectName;
                ObjectType createdType = type;
                source.sendSuccess(() -> Component.literal("Successfully created " + createdType.getType() + " " + createdName), false);
                return 1;
            }
            source.sendFailure(Component.literal("Failed to create " + type.getType() + " " + objectName));
        } catch (Exception e) {
            source.sendFailure(Component.literal("Something went wrong, please check the logs"));
            OTGLog.log(LogLevel.ERROR, LogCategory.MAIN, "Error during export command: " + e.getClass().getName());
            OTGLog.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
        }
        return 0;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Usage: /otg export <object name> [type] [preset] [template]").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal(" - Exports the area selected with /otg region or WorldEdit"), false);
        source.sendSuccess(() -> Component.literal(" - Object name is the only required argument"), false);
        source.sendSuccess(() -> Component.literal(" - Type is either BO3 or BO4"), false);
        source.sendSuccess(() -> Component.literal(" - Preset determines where the object is saved, and where templates are searched for, use global for GlobalObjects"), false);
        source.sendSuccess(() -> Component.literal(" - Template is a BO3 file whose settings are used for the exported object"), false);
        source.sendSuccess(() -> Component.literal("    - Templates have file ending .BO3Template or .BO4Template"), false);
        source.sendSuccess(() -> Component.literal("    - Templates are not loaded as objects"), false);
        source.sendSuccess(() -> Component.literal(" - Default settings, center block, air and overwriting can be set with /otg configexport"), false);
        return 1;
    }

    private ExportCommand() {}
}
