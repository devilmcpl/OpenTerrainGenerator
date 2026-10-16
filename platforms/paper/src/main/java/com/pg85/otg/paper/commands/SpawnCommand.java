package com.pg85.otg.paper.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.constants.settings.structure.CustomStructureType;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.customobject.bo4.BO4;
import com.pg85.otg.customobject.structures.CustomStructureCache;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.paper.gen.PaperLevelRegion;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

final class SpawnCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /otg spawn <preset> <object> [<x> <y> <z>] [force]
        return Commands.literal("spawn")
            .then(Commands.argument("preset", StringArgumentType.string())
                .suggests((ctx, builder) -> ObjectSuggestions.suggestPresets(builder, true))
                .then(Commands.argument("object", StringArgumentType.word())
                    .suggests(ObjectSuggestions::suggestObjects)
                    .executes(ctx -> execute(ctx.getSource(),
                        StringArgumentType.getString(ctx, "preset"),
                        StringArgumentType.getString(ctx, "object"),
                        BlockPos.containing(ctx.getSource().getPosition()),
                        false))
                    .then(Commands.argument("force", BoolArgumentType.bool())
                        .executes(ctx -> execute(ctx.getSource(),
                            StringArgumentType.getString(ctx, "preset"),
                            StringArgumentType.getString(ctx, "object"),
                            BlockPos.containing(ctx.getSource().getPosition()),
                            BoolArgumentType.getBool(ctx, "force"))))
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> execute(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "preset"),
                                    StringArgumentType.getString(ctx, "object"),
                                    new BlockPos(IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z")),
                                    false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                    .executes(ctx -> execute(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "preset"),
                                        StringArgumentType.getString(ctx, "object"),
                                        new BlockPos(IntegerArgumentType.getInteger(ctx, "x"), IntegerArgumentType.getInteger(ctx, "y"), IntegerArgumentType.getInteger(ctx, "z")),
                                        BoolArgumentType.getBool(ctx, "force")))))))));
    }

    private static int execute(CommandSourceStack source, String presetName, String objectName, BlockPos blockPos, boolean force) {
        try {
            presetName = presetName != null && presetName.equalsIgnoreCase("global") ? null : presetName;
            CustomObject objectToSpawn = ObjectCommandUtils.getObject(objectName, presetName);

            if (objectToSpawn == null) {
                source.sendFailure(Component.literal("Could not find an object by the name " + objectName + " in either " + presetName + " or Global objects"));
                return 0;
            }

            ServerLevel level = source.getLevel();
            Preset preset = ObjectCommandUtils.getPresetOrDefault(presetName);
            PaperLevelRegion genRegion = PaperLevelRegion.create(level, preset, blockPos);

            if (objectToSpawn instanceof BO4 bo4) {
                if (!(level.getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator generator)) {
                    source.sendFailure(Component.literal("BO4 objects can only be spawned in OTG worlds/dimensions."));
                    return 0;
                }
                if (generator.getPreset().getPresetConfig().getResourceSettings().getCustomStructureType() != CustomStructureType.BO4) {
                    source.sendFailure(Component.literal("Cannot spawn a BO4 structure in an isOTGPlus:false world, use a BO3 instead or recreate the world with IsOTGPlus:true in the worldconfig."));
                    return 0;
                }

                int playerX = blockPos.getX();
                int playerZ = blockPos.getZ();
                ChunkCoordinate playerChunk = ChunkCoordinate.fromBlockCoords(playerX, playerZ);

                // Matches any bo4/bo4data file ending with C[0-9]R[0-9], assuming it's not a start
                // bo4 for a structure, but rather a branch that should be spawned individually.
                if (bo4.getName().matches(".*C[0-9]([0-9]*)R[0-9]([0-9]*)$")) {
                    int x = playerChunk.getBlockX() + bo4.getConfig().getminX();
                    int z = playerChunk.getBlockZ() + bo4.getConfig().getminZ();
                    bo4.trySpawnAt(
                        genRegion,
                        new Random(),
                        Rotation.NORTH,
                        playerChunk,
                        x,
                        genRegion.getHighestBlockAboveYAt(x, z),
                        z,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        false,
                        genRegion.getCachedBiomeProvider().getBiomeConfig(x, z).getSurfaceSettings().getWaterLevelMax(),
                        false,
                        false,
                        false
                    );
                    return 1;
                }

                Path worldSaveFolder = level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).getParent();
                CustomStructureCache cache = generator.getStructureCache(worldSaveFolder);

                // Try spawning the structure in available chunks around the player
                int maxRadius = 1000;
                source.sendSuccess(() -> Component.literal("Trying to plot BO4 structure within " + maxRadius + " chunks of player, with height bounds " + (force ? "disabled" : "enabled") + ". This may take a while."), false);

                ChunkCoordinate chunkCoord;
                for (int cycle = 1; cycle < maxRadius; cycle++) {
                    for (int x1 = playerX - cycle; x1 <= playerX + cycle; x1++) {
                        for (int z1 = playerZ - cycle; z1 <= playerZ + cycle; z1++) {
                            if (x1 == playerX - cycle || x1 == playerX + cycle || z1 == playerZ - cycle || z1 == playerZ + cycle) {
                                chunkCoord = ChunkCoordinate.fromChunkCoords(
                                    playerChunk.getChunkX() + (x1 - playerX),
                                    playerChunk.getChunkZ() + (z1 - playerZ)
                                );

                                // Find an area of chunks nearby that hasn't been generated yet, so we can plot BO4's on top.
                                // The plotter will avoid any chunks that have already been plotted, but let's not spam it more
                                // than we need to.
                                if (
                                    !level.hasChunk(chunkCoord.getChunkX(), chunkCoord.getChunkZ()) ||
                                    !level.getChunk(chunkCoord.getChunkX(), chunkCoord.getChunkZ()).getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)
                                ) {
                                    // TODO: Add targetBiomes parameter for command.
                                    ChunkCoordinate chunkCoordSpawned = cache.plotBo4Structure(
                                        genRegion,
                                        bo4,
                                        new ArrayList<>(),
                                        chunkCoord,
                                        OTG.getEngine().getOTGRootFolder(),
                                        OTG.getEngine().getCustomObjectManager(),
                                        OTGMaterialReader.get(),
                                        OTG.getEngine().getCustomObjectResourcesManager(),
                                        OTG.getEngine().getModLoadedChecker(),
                                        force
                                    );

                                    if (chunkCoordSpawned != null) {
                                        sendSpawnedAt(source, objectToSpawn.getName(), chunkCoordSpawned.getBlockX(), chunkCoordSpawned.getBlockZ());
                                        return 1;
                                    }
                                }
                            }
                        }
                    }
                }
                source.sendFailure(Component.literal(objectToSpawn.getName() + " could not be spawned. This can happen if the world is currently generating chunks, if no biomes with enough space could be found, or if there is an error in the structure's files. Enable SpawnLog:true in OTG.ini and check the logs for more information."));
                return 0;
            }

            if (objectToSpawn.spawnForced(
                null,
                genRegion,
                new Random(),
                Rotation.NORTH,
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ(),
                genRegion.isOTGWorld()
            )) {
                sendSpawnedAt(source, objectToSpawn.getName(), blockPos.getX(), blockPos.getZ());
                return 1;
            }
            source.sendFailure(Component.literal("Failed to spawn object " + objectName));
        } catch (Exception e) {
            source.sendFailure(Component.literal("Something went wrong, please check logs"));
            OTGLog.log(LogLevel.ERROR, LogCategory.MAIN, "Error during spawn command: " + e.getClass().getName());
            OTGLog.printStackTrace(LogLevel.ERROR, LogCategory.MAIN, e);
        }
        return 0;
    }

    private static void sendSpawnedAt(CommandSourceStack source, String objectName, int x, int z) {
        source.sendSuccess(() -> Component.literal(objectName + " was spawned at: "), false);
        MutableComponent coordinates = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", x, "~", z))
            .withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.SuggestCommand("/tp @s " + x + " ~ " + z))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
        source.sendSuccess(() -> coordinates, false);
    }

    private SpawnCommand() {}
}
