package com.pg85.otg.paper.gen;

import com.pg85.otg.paper.materials.PaperMaterialData;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.gen.ChunkBuffer;
import com.pg85.otg.util.materials.LocalMaterialData;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Getter
public class PaperChunkBuffer extends ChunkBuffer {
    private final ChunkAccess chunkAccess;

    public PaperChunkBuffer(ChunkAccess chunkAccess) {
        this.chunkAccess = chunkAccess;
    }

    @Override
    public ChunkCoordinate getChunkCoordinate() {
        return ChunkCoordinate.fromChunkCoords(chunkAccess.getPos().x, chunkAccess.getPos().z);
    }

    @Override
    public void setBlock(int blockX, int blockY, int blockZ, LocalMaterialData material) {
        PaperMaterialData paperMaterialData = (PaperMaterialData) material;
        chunkAccess.setBlockState(new BlockPos(blockX, blockY, blockZ), paperMaterialData.getState(), 0);
    }

    @Override
    public LocalMaterialData getBlock(int blockX, int blockY, int blockZ) {
        return PaperMaterialData.ofBlockState(chunkAccess.getBlockState(new BlockPos(blockX, blockY, blockZ)));
    }
}
