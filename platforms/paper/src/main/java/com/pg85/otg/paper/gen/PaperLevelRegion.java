package com.pg85.otg.paper.gen;

import com.pg85.otg.OTG;
import com.pg85.otg.config.settings.biome.BiomeSettings;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ICachedBiomeProvider;
import com.pg85.otg.paper.materials.PaperMaterialData;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.biome.ReplaceBlockMatrix;
import com.pg85.otg.util.gen.OTGWorldInfo;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.LocalMaterials;
import com.pg85.otg.util.minecraft.TreeType;
import com.pg85.otg.util.nbt.NamedBinaryTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * World gen region for a live world, used outside of decoration for /otg spawn, /otg edit, /otg export and saplings.
 * Can also be used for non-OTG worlds, in which case anything that needs OTG biomes or the OTG chunk generator is unavailable.
 */
public class PaperLevelRegion extends PaperWorldGenRegion {
    private final ServerLevel level;

    private PaperLevelRegion(Preset preset, ServerLevel level, BlockPos pos, @Nullable OTGPaperChunkGenerator chunkGenerator) {
        super(
            preset.getFolderName(),
            OTG.getEngine().getPluginConfig(),
            preset.getPresetConfig(),
            new OTGWorldInfo(level.getMinY(), level.getMaxY()),
            level,
            pos.getX() >> 4,
            pos.getZ() >> 4,
            chunkGenerator,
            chunkGenerator != null ? chunkGenerator.getInternalGenerator().getCachedBiomeProvider() : new EmptyCachedBiomeProvider()
        );
        this.level = level;
    }

    /**
     * Creates a region for the given world. For OTG worlds the world's own preset is used,
     * for other worlds the given preset is used for settings.
     */
    public static PaperLevelRegion create(ServerLevel level, Preset preset, BlockPos pos) {
        if (level.getChunkSource().getGenerator() instanceof OTGPaperChunkGenerator chunkGenerator) {
            return new PaperLevelRegion(chunkGenerator.getPreset(), level, pos, chunkGenerator);
        }
        return new PaperLevelRegion(preset, level, pos, null);
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public boolean isOTGWorld() {
        return this.chunkGenerator != null;
    }

    private void requireOTGWorld() {
        if (this.chunkGenerator == null) {
            throw new UnsupportedOperationException("This method is not available for non-OTG worlds, you're trying to use an unsupported feature.");
        }
    }

    @Override
    public ICachedBiomeProvider getCachedBiomeProvider() {
        requireOTGWorld();
        return super.getCachedBiomeProvider();
    }

    @Override
    public IBiome getBiomeForDecoration(int x, int z) {
        return this.getCachedBiomeProvider().getBiome(x, z);
    }

    @Override
    public BiomeSettings getBiomeConfigForDecoration(int worldX, int worldZ) {
        return this.getCachedBiomeProvider().getBiomeConfig(worldX, worldZ);
    }

    @Override
    public double getBiomeBlocksNoiseValue(int xInWorld, int zInWorld) {
        requireOTGWorld();
        return super.getBiomeBlocksNoiseValue(xInWorld, zInWorld);
    }

    // There's no decoration area for a live world, so only the world height is checked
    @Override
    public LocalMaterialData getMaterial(int x, int y, int z) {
        if (y < this.otgWorldInfo.minY() || y > this.otgWorldInfo.maxY()) {
            return LocalMaterials.AIR;
        }
        return getMaterialDirect(x, y, z);
    }

    @Override
    public int getHighestBlockYAt(int x, int z, boolean findSolid, boolean findLiquid, boolean ignoreLiquid, boolean ignoreSnow, boolean ignoreLeaves) {
        int heightMapY = this.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return getHighestBlockYAt(this.level, x, heightMapY, z, findSolid, findLiquid, ignoreLiquid, ignoreSnow, ignoreLeaves);
    }

    @Override
    public int getHeightMapHeight(int x, int z) {
        return this.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    @Override
    public int getLightLevel(int x, int y, int z) {
        if (y < this.otgWorldInfo.minY() || y > this.otgWorldInfo.maxY()) {
            return -1;
        }
        return this.level.getLightEmission(new BlockPos(x, y, z));
    }

    @Override
    public void setBlock(int x, int y, int z, LocalMaterialData material, NamedBinaryTag nbt, ReplaceBlockMatrix replaceBlocksMatrix) {
        if (material.isEmpty()) {
            // Happens when configs contain blocks that don't exist.
            return;
        }
        super.setBlock(x, y, z, material, nbt, replaceBlocksMatrix);
        if (material.isLiquid()) {
            BlockPos pos = new BlockPos(x, y, z);
            this.level.scheduleTick(pos, ((PaperMaterialData) material).getState().getFluidState().getType(), 0);
        }
    }

    @Override
    public boolean placeTree(TreeType type, Random rand, int x, int y, int z) {
        requireOTGWorld();
        return super.placeTree(type, rand, x, y, z);
    }

    @Override
    public void placeDungeon(Random random, int x, int y, int z) {
        requireOTGWorld();
        super.placeDungeon(random, x, y, z);
    }

    @Override
    public void placeFossil(Random random, int x, int y, int z) {
        requireOTGWorld();
        super.placeFossil(random, x, y, z);
    }

    @Override
    public LocalMaterialData getMaterialWithoutLoading(int x, int y, int z) {
        requireOTGWorld();
        return super.getMaterialWithoutLoading(x, y, z);
    }

    @Override
    public int getHighestBlockYAtWithoutLoading(int x, int z, boolean findSolid, boolean findLiquid, boolean ignoreLiquid, boolean ignoreSnow, boolean ignoreLeaves) {
        requireOTGWorld();
        return super.getHighestBlockYAtWithoutLoading(x, z, findSolid, findLiquid, ignoreLiquid, ignoreSnow, ignoreLeaves);
    }

    @Override
    public boolean chunkHasDefaultStructure(Random worldRandom, ChunkCoordinate chunkCoordinate) {
        requireOTGWorld();
        return super.chunkHasDefaultStructure(worldRandom, chunkCoordinate);
    }
}
