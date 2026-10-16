package com.pg85.otg.paper.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.customobject.bo2.BO2;
import com.pg85.otg.customobject.bo3.BO3;
import com.pg85.otg.customobject.bo3.bo3function.BO3RandomBlockFunction;
import com.pg85.otg.customobject.bo4.BO4;
import com.pg85.otg.customobject.bo4.BO4Config;
import com.pg85.otg.customobject.bo4.bo4function.BO4RandomBlockFunction;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.customobject.config.io.FileSettingsReaderBO4;
import com.pg85.otg.customobject.creator.ObjectCreator;
import com.pg85.otg.customobject.creator.ObjectType;
import com.pg85.otg.customobject.structures.StructuredCustomObject;
import com.pg85.otg.customobject.util.Corner;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.paper.commands.region.Region;
import com.pg85.otg.paper.commands.region.RegionSelections;
import com.pg85.otg.paper.gen.PaperLevelRegion;
import com.pg85.otg.paper.materials.PaperMaterialData;
import com.pg85.otg.paper.util.PaperNBTHelper;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class EditCommand {

    private static final Map<UUID, EditSession> sessions = new ConcurrentHashMap<>();
    private static final String[] FLAGS = new String[]
        { "-nofix", "-update", "-wrongleaves" };

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /otg edit <preset> <object> [flags]
        return Commands.literal("edit")
            .executes(EditCommand::help)
            .then(Commands.argument("preset", StringArgumentType.string())
                .suggests((ctx, builder) -> ObjectSuggestions.suggestPresets(builder, true))
                .then(Commands.argument("object", StringArgumentType.word())
                    .suggests(ObjectSuggestions::suggestObjects)
                    .executes(EditCommand::execute)
                    .then(Commands.argument("flags", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FLAGS, builder))
                        .executes(EditCommand::execute))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> registerFinish() {
        // /otg finishedit
        return Commands.literal("finishedit")
            .executes(EditCommand::finish);
    }

    static LiteralArgumentBuilder<CommandSourceStack> registerCancel() {
        // /otg canceledit
        return Commands.literal("canceledit")
            .executes(EditCommand::cancelSession);
    }

    static void removeSession(UUID player) {
        sessions.remove(player);
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = CommandHelper.requirePlayer(source);
        if (player == null) return 0;

        try {
            String presetFolderName = StringArgumentType.getString(context, "preset");
            String objectName = StringArgumentType.getString(context, "object");
            boolean immediate = false, doFixing = true, leaveIllegalLeaves = false;
            try {
                String flags = StringArgumentType.getString(context, "flags");
                immediate = flags.contains("-update");
                doFixing = !flags.contains("-nofix");
                leaveIllegalLeaves = flags.contains("-wrongleaves");
            } catch (IllegalArgumentException ignored) {}

            presetFolderName = presetFolderName.equalsIgnoreCase("global") ? null : presetFolderName;
            boolean isGlobal = presetFolderName == null;

            StructuredCustomObject inputObject;
            try {
                inputObject = getStructuredObject(objectName, presetFolderName);
            } catch (InvalidConfigException e) {
                source.sendFailure(Component.literal("Error loading object " + objectName));
                return 0;
            }

            if (inputObject == null) {
                source.sendFailure(Component.literal("Could not find " + objectName));
                return 0;
            }

            ObjectType type = inputObject.getType();
            Preset preset = ObjectCommandUtils.getPresetOrDefault(presetFolderName);
            if (preset == null) {
                source.sendFailure(Component.literal("Could not find preset " + (presetFolderName == null ? "" : presetFolderName)));
                return 0;
            }

            // The world gen region is a wrapper for the world that ObjectCreator can interact with
            ServerLevel level = source.getLevel();
            PaperLevelRegion worldGenRegion = PaperLevelRegion.create(level, preset, player.blockPosition());
            Region region = ObjectCommandUtils.getRegionFromObject(player.blockPosition(), inputObject, level.getMinY(), level.getMaxY());
            Corner center = region.getCenter();

            // Prepare area for spawning
            ObjectCommandUtils.cleanArea(worldGenRegion, region.getMin(), region.getMax(), true);

            // -- Spawn and update --
            // Spawn code, taken and modified from BO3.java :: spawnForced()
            List<BlockFunction<?>> extraBlocks = spawnAndFixObject(center.x(), center.y(), center.z(), inputObject, worldGenRegion, doFixing, preset.getFolderName());

            // Save the object and clean the area
            Path path = ObjectCommandUtils.getObjectFolderPath(isGlobal ? null : preset.getPresetFolder())
                .resolve(ObjectCommandUtils.getFoldersFromObject(inputObject));

            if (immediate) {
                StructuredCustomObject object = ObjectCommandUtils.exportEditedObject(type, region, center, inputObject, path, extraBlocks, preset.getFolderName(), leaveIllegalLeaves, worldGenRegion);
                if (object != null) {
                    source.sendSuccess(() -> Component.literal("Successfully updated " + type.getType() + " " + inputObject.getName()), false);
                    return 1;
                }
                source.sendFailure(Component.literal("Failed to update " + type.getType() + " " + inputObject.getName()));
                return 0;
            }

            // Store the info, wait for /otg finishedit
            sessions.put(player.getUUID(), new EditSession(type, worldGenRegion, inputObject, extraBlocks, path, preset.getFolderName(), center, leaveIllegalLeaves));
            source.sendSuccess(() -> Component.literal("You can now edit the object"), false);
            source.sendSuccess(() -> Component.literal("To change the area of the object, use /otg region"), false);
            source.sendSuccess(() -> Component.literal("When you are done editing, do /otg finishedit"), false);
            source.sendSuccess(() -> Component.literal("To cancel, do /otg canceledit"), false);
            if (!extraBlocks.isEmpty()) {
                source.sendSuccess(() -> Component.literal("This object's center cannot be moved"), false);
            }

            RegionSelections.set(player.getUUID(), region);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Edit command encountered an error, please check the logs."));
            OTGLog.log(LogLevel.ERROR, LogCategory.MAIN, "Edit command encountered an error: " + e.getClass().getName() + " - " + e.getMessage());
            OTGLog.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
        }
        return 0;
    }

    private static StructuredCustomObject getStructuredObject(String objectName, String presetFolderName) throws InvalidConfigException {
        CustomObject objectToSpawn = ObjectCommandUtils.getObject(objectName, presetFolderName);

        if (objectToSpawn instanceof BO3 bo3) {
            return bo3;
        }

        if (presetFolderName == null) {
            presetFolderName = OTG.getEngine().getPresetLoader().getDefaultPresetFolderName();
        }

        if (objectToSpawn instanceof BO4 bo4) {
            // Load the BO4 with all of its blocks
            File file = bo4.getConfig().getFile();
            return new BO4(
                objectName,
                file,
                new BO4Config(new FileSettingsReaderBO4(objectName, file), true, presetFolderName, OTG.getEngine().getOTGRootFolder())
            );
        } else if (objectToSpawn instanceof BO2 bo2) {
            // Convert the BO2 to a BO3 before editing
            return new BO3(
                bo2.getName(),
                ObjectType.BO3.getObjectFilePathFromName(bo2.getName(), bo2.getFile().getParentFile().toPath()).toFile(),
                bo2.getConvertedConfig(presetFolderName, OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getCustomObjectManager(),
                    OTGMaterialReader.get(), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker())
            );
        }
        return null;
    }

    private static int finish(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = CommandHelper.requirePlayer(source);
        if (player == null) return 0;

        try {
            EditSession session = sessions.get(player.getUUID());
            Region region = RegionSelections.get(player.getUUID());
            if (session == null) {
                source.sendFailure(Component.literal("No active session, do '/otg edit' to start one"));
                return 0;
            } else if (region.getMin() == null) {
                source.sendFailure(Component.literal("Please mark two positions with /otg region"));
                return 0;
            } else if (ObjectCommandUtils.isOutsideBounds(region, session.type)) {
                source.sendFailure(Component.literal("Selection is too big! Maximum size is 16x16 for BO4 and 32x32 for BO3"));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Cleaning up..."), false);

            StructuredCustomObject object = ObjectCreator.createObject(
                session.type,
                region.getMin(),
                region.getMax(),
                // Don't let someone change the center if there are non-spawned blocks
                session.extraBlocks.isEmpty() && region.getCenter() != null ? region.getCenter() : session.originalCenterPoint,
                null,
                session.object.getName(),
                false,
                session.leaveIllegalLeaves,
                session.objectPath,
                session.genRegion,
                new PaperNBTHelper(),
                session.extraBlocks,
                session.object.getConfig(),
                session.presetFolderName,
                OTG.getEngine().getOTGRootFolder(),
                OTG.getEngine().getCustomObjectManager(),
                OTGMaterialReader.get(),
                OTG.getEngine().getCustomObjectResourcesManager(),
                OTG.getEngine().getModLoadedChecker()
            );

            if (object != null) {
                OTG.getEngine().getCustomObjectManager().getGlobalObjects().addObjectToPreset(session.presetFolderName, object.getName(), object.getConfig().getFile(), object);
                source.sendSuccess(() -> Component.literal("Successfully edited " + session.type.getType() + " " + object.getName()), false);
            } else {
                source.sendFailure(Component.literal("Failed to edit " + session.type.getType() + " " + session.object.getName()));
            }
            ObjectCommandUtils.cleanArea(session.genRegion, region.getMin(), region.getMax(), false);
            sessions.remove(player.getUUID());
            return object != null ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Edit command encountered an error, please check logs."));
            OTGLog.log(LogLevel.ERROR, LogCategory.MAIN, "Edit command encountered an error: " + e.getClass().getName());
            OTGLog.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
        }
        return 0;
    }

    private static int cancelSession(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = CommandHelper.requirePlayer(source);
        if (player == null) return 0;

        EditSession session = sessions.get(player.getUUID());
        Region region = RegionSelections.get(player.getUUID());
        if (session != null && region.getMin() != null) {
            ObjectCommandUtils.cleanArea(session.genRegion, region.getMin(), region.getMax(), false);
            sessions.remove(player.getUUID());
            source.sendSuccess(() -> Component.literal("Edit session cancelled"), false);
            return 1;
        }
        source.sendFailure(Component.literal("No active edit session to cancel"));
        return 0;
    }

    private record EditSession(ObjectType type,
                               PaperLevelRegion genRegion,
                               StructuredCustomObject object,
                               List<BlockFunction<?>> extraBlocks,
                               Path objectPath, String presetFolderName,
                               Corner originalCenterPoint, boolean leaveIllegalLeaves) { }

    private static List<BlockFunction<?>> spawnAndFixObject(int x, int y, int z, StructuredCustomObject object, PaperLevelRegion worldGenRegion, boolean fixObject, String presetFolderName) {
        BlockFunction<?>[] blocks = object.getConfig().getBlockFunctions(presetFolderName, OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getCustomObjectManager(),
            OTGMaterialReader.get(), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());

        HashSet<BlockPos> updates = new HashSet<>();
        HashSet<BlockPos> gravityBlocksToCheck = new HashSet<>();
        Random random = new Random();

        List<BlockFunction<?>> unspawnedBlocks = new ArrayList<>();

        for (BlockFunction<?> block : blocks) {
            if (
                block.material == null
                || (block.nbt != null
                    || block instanceof BO3RandomBlockFunction
                    || block instanceof BO4RandomBlockFunction
                    || block.material.isBlank())
            ) {
                unspawnedBlocks.add(block);
                continue;
            }

            // Queue the block coordinate for processing
            if (fixObject && updateMap.contains(Identifier.tryParse(block.material.getRegistryName()))) {
                updates.add(new BlockPos(x + block.x, y + block.y, z + block.z));
            }

            // Make sure gravel and sand blocks don't fall down
            if (gravityBlocksSet.contains(block.material)) {
                gravityBlocksToCheck.add(new BlockPos(x + block.x, y + block.y, z + block.z));
            }

            // We set all leaves to be persistent so they don't disappear in the process
            // They lose persistence on export if they are legal
            if (block.material.isLeaves()) {
                block.material = PaperMaterialData.ofBlockState(((PaperMaterialData) block.material).getState()
                    .setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 7));
            }

            block.spawn(worldGenRegion, random, x + block.x, y + block.y, z + block.z);
        }

        for (BlockPos pos : gravityBlocksToCheck) {
            if (worldGenRegion.getMaterial(pos.getX(), pos.getY() - 1, pos.getZ()).isMaterial(LocalMaterials.STRUCTURE_VOID)) {
                // prop up any gravity blocks so they don't fall
                worldGenRegion.setBlock(pos.getX(), pos.getY() - 1, pos.getZ(), LocalMaterials.STRUCTURE_BLOCK);
            }
        }

        if (fixObject) {
            ServerLevel level = worldGenRegion.getLevel();
            for (BlockPos blockpos : updates) {
                BlockState blockstate = level.getBlockState(blockpos);
                if (blockstate.is(BlockTags.LEAVES)) {
                    // Schedule a tick on this block in 1 unit of time
                    level.scheduleTick(blockpos, blockstate.getBlock(), 1);
                } else {
                    BlockState updated = Block.updateFromNeighbourShapes(blockstate, level, blockpos);
                    level.setBlock(blockpos, updated, 20);
                }
            }
        }

        return unspawnedBlocks;
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("To use the edit command:").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal("/otg edit <preset> <object> [-nofix, -update, -wrongleaves]"), false);
        source.sendSuccess(() -> Component.literal(" - Preset is which preset to fetch the object from, and save it back to"), false);
        source.sendSuccess(() -> Component.literal(" - Object is the object you want to edit"), false);
        source.sendSuccess(() -> Component.literal(" - The -nofix flag disables block state fixing"), false);
        source.sendSuccess(() -> Component.literal(" - The -update flag immediately exports and cleans after fixing"), false);
        source.sendSuccess(() -> Component.literal(" - The -wrongleaves flag keeps leaves that would decay"), false);
        source.sendSuccess(() -> Component.literal(" - Complex objects cannot have their center moved"), false);
        source.sendSuccess(() -> Component.literal(" - An object is \"complex\" if it contains NBT or RandomBlock"), false);
        return 1;
    }

    // These maps are used to figure out what blocks to update
    private static final HashSet<LocalMaterialData> gravityBlocksSet = Stream.of(
        LocalMaterials.SAND, LocalMaterials.RED_SAND, LocalMaterials.GRAVEL
    ).collect(Collectors.toCollection(HashSet::new));

    private static final HashSet<Identifier> updateMap = Stream.of(
        "oak_fence", "birch_fence", "nether_brick_fence", "spruce_fence", "jungle_fence", "acacia_fence", "dark_oak_fence",
        "iron_door", "oak_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door",
        "glass_pane", "white_stained_glass_pane", "orange_stained_glass_pane", "magenta_stained_glass_pane",
        "light_blue_stained_glass_pane", "yellow_stained_glass_pane", "lime_stained_glass_pane", "pink_stained_glass_pane",
        "gray_stained_glass_pane", "light_gray_stained_glass_pane", "cyan_stained_glass_pane", "purple_stained_glass_pane",
        "blue_stained_glass_pane", "brown_stained_glass_pane", "green_stained_glass_pane", "red_stained_glass_pane",
        "black_stained_glass_pane",
        "purpur_stairs", "oak_stairs", "cobblestone_stairs", "brick_stairs", "stone_brick_stairs", "nether_brick_stairs",
        "spruce_stairs", "sandstone_stairs", "birch_stairs", "jungle_stairs", "quartz_stairs", "acacia_stairs",
        "dark_oak_stairs", "prismarine_stairs", "prismarine_brick_stairs", "dark_prismarine_stairs", "red_sandstone_stairs",
        "polished_granite_stairs", "smooth_red_sandstone_stairs", "mossy_stone_brick_stairs", "polished_diorite_stairs",
        "mossy_cobblestone_stairs", "end_stone_brick_stairs", "stone_stairs", "smooth_sandstone_stairs",
        "smooth_quartz_stairs", "granite_stairs", "andesite_stairs", "red_nether_brick_stairs", "polished_andesite_stairs",
        "diorite_stairs",
        "cobblestone_wall", "mossy_cobblestone_wall", "brick_wall", "prismarine_wall", "red_sandstone_wall",
        "mossy_stone_brick_wall", "granite_wall", "stone_brick_wall", "nether_brick_wall", "andesite_wall",
        "red_nether_brick_wall", "sandstone_wall", "end_stone_brick_wall", "diorite_wall",
        "iron_bars", "trapped_chest", "chest", "redstone_wire",
        "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves", "dark_oak_leaves",
        "vine"
    ).map(Identifier::withDefaultNamespace).collect(Collectors.toCollection(HashSet::new));

    private EditCommand() {}
}
