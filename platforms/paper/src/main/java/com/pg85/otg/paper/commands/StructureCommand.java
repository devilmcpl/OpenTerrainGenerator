package com.pg85.otg.paper.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pg85.otg.OTG;
import com.pg85.otg.customobject.bo3.BO3;
import com.pg85.otg.customobject.bo4.BO4;
import com.pg85.otg.customobject.structures.CustomStructure;
import com.pg85.otg.customobject.structures.bo4.BO4CustomStructure;
import com.pg85.otg.paper.gen.OTGPaperChunkGenerator;
import com.pg85.otg.util.ChunkCoordinate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

final class StructureCommand {

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /otg structure
        return Commands.literal("structure")
            .executes(StructureCommand::showStructureInfo);
    }

    private static int showStructureInfo(CommandContext<CommandSourceStack> ctx) {
        if (!CommandHelper.requireOTGWorld(ctx)) return 0;
        CommandSourceStack source = ctx.getSource();
        OTGPaperChunkGenerator generator = CommandHelper.getGenerator(source.getLevel());
        if (generator == null) return 0;

        String structureInfo = "";
        ChunkCoordinate playerChunk = ChunkCoordinate.fromBlockCoords((int) source.getPosition().x, (int) source.getPosition().z);
        Path worldSaveFolder = source.getLevel().getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).getParent();
        CustomStructure worldInfoChunk = generator.getStructureCache(worldSaveFolder).getChunkData(playerChunk);
        if (worldInfoChunk != null) {
            Path otgRootFolder = OTG.getEngine().getOTGRootFolder();
            if (worldInfoChunk instanceof BO4CustomStructure bo4CustomStructure) {
                structureInfo += "-- BO4 Info -- \r\nName: " + ((BO4) worldInfoChunk.start.getObject(otgRootFolder)).getConfig().getName().replace("Start", "");
                String branchesInChunk = bo4CustomStructure.getObjectsToSpawnInfo().get(playerChunk);
                if (branchesInChunk != null && !branchesInChunk.isEmpty()) {
                    structureInfo += "\r\n" + branchesInChunk;
                }
            } else {
                structureInfo += "-- BO3 Info -- \r\nName: " + ((BO3) worldInfoChunk.start.getObject(otgRootFolder)).getConfig().getName();
            }
        } else {
            structureInfo = "No OTG structure found in this chunk";
        }
        String message = structureInfo;
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private StructureCommand() {}
}
