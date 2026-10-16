package com.pg85.otg.fabric.gen;

import com.pg85.otg.fabric.materials.FabricMaterialData;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.gen.ChunkBuffer;
import com.pg85.otg.util.materials.LocalMaterialData;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Getter
public class FabricChunkBuffer extends ChunkBuffer {
    private final ChunkAccess chunkAccess;

    public FabricChunkBuffer(ChunkAccess chunkAccess) {
        this.chunkAccess = chunkAccess;
    }

    @Override
    public ChunkCoordinate getChunkCoordinate() {
        return ChunkCoordinate.fromChunkCoords(chunkAccess.getPos().x, chunkAccess.getPos().z);
    }

    @Override
    public void setBlock(int blockX, int blockY, int blockZ, LocalMaterialData material) {
        FabricMaterialData fabricMaterialData = (FabricMaterialData) material;
        chunkAccess.setBlockState(new BlockPos(blockX, blockY, blockZ), fabricMaterialData.getState(), 0);
    }

    @Override
    public LocalMaterialData getBlock(int blockX, int blockY, int blockZ) {
        return FabricMaterialData.ofBlockState(chunkAccess.getBlockState(new BlockPos(blockX, blockY, blockZ)));
    }
}
