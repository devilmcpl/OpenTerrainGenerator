package com.pg85.otg.paper.materials;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.util.materials.LocalMaterials;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PaperMaterials extends LocalMaterials
{
	// Default blocks in given tags.
	// Tags aren't loaded until datapacks are loaded, on world creation. We mirror the vanilla copy of the tag to solve this.
	private static final Block[] CORAL_BLOCKS_TAG = { Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK };
	private static final Block[] WALL_CORALS_TAG = { Blocks.TUBE_CORAL_WALL_FAN, Blocks.BRAIN_CORAL_WALL_FAN, Blocks.BUBBLE_CORAL_WALL_FAN, Blocks.FIRE_CORAL_WALL_FAN, Blocks.HORN_CORAL_WALL_FAN };
	private static final Block[] CORALS_TAG = { Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL, Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN };

	public static void init()
	{
		// Coral
		CORAL_BLOCKS = Arrays.stream(CORAL_BLOCKS_TAG).map(block -> PaperMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
		WALL_CORALS = Arrays.stream(WALL_CORALS_TAG).map(block -> PaperMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
		CORALS = Arrays.stream(CORALS_TAG).map(block -> PaperMaterialData.ofBlockState(block.defaultBlockState())).collect(Collectors.toList());
				
		// Blocks used in OTG code
		
		AIR = PaperMaterialData.ofBlockState(Blocks.AIR.defaultBlockState());
		CAVE_AIR = PaperMaterialData.ofBlockState(Blocks.CAVE_AIR.defaultBlockState());
		STRUCTURE_VOID = PaperMaterialData.ofBlockState(Blocks.STRUCTURE_VOID.defaultBlockState());
		COMMAND_BLOCK = PaperMaterialData.ofBlockState(Blocks.COMMAND_BLOCK.defaultBlockState());
		STRUCTURE_BLOCK = PaperMaterialData.ofBlockState(Blocks.STRUCTURE_BLOCK.defaultBlockState());
		GRASS = PaperMaterialData.ofBlockState(Blocks.SHORT_GRASS.defaultBlockState());
		DIRT = PaperMaterialData.ofBlockState(Blocks.DIRT.defaultBlockState());
		CLAY = PaperMaterialData.ofBlockState(Blocks.CLAY.defaultBlockState());
		TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.TERRACOTTA.defaultBlockState());
		WHITE_TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.WHITE_TERRACOTTA.defaultBlockState());
		ORANGE_TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.ORANGE_TERRACOTTA.defaultBlockState());
		YELLOW_TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.YELLOW_TERRACOTTA.defaultBlockState());
		BROWN_TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.BROWN_TERRACOTTA.defaultBlockState());
		RED_TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.RED_TERRACOTTA.defaultBlockState());
		SILVER_TERRACOTTA = PaperMaterialData.ofBlockState(Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState());
		STONE = PaperMaterialData.ofBlockState(Blocks.STONE.defaultBlockState());
		NETHERRACK = PaperMaterialData.ofBlockState(Blocks.NETHERRACK.defaultBlockState());
		END_STONE = PaperMaterialData.ofBlockState(Blocks.END_STONE.defaultBlockState());
		SAND = PaperMaterialData.ofBlockState(Blocks.SAND.defaultBlockState());
		RED_SAND = PaperMaterialData.ofBlockState(Blocks.RED_SAND.defaultBlockState());
		SANDSTONE = PaperMaterialData.ofBlockState(Blocks.SANDSTONE.defaultBlockState());
		RED_SANDSTONE = PaperMaterialData.ofBlockState(Blocks.RED_SANDSTONE.defaultBlockState());
		GRAVEL = PaperMaterialData.ofBlockState(Blocks.GRAVEL.defaultBlockState());
		MOSSY_COBBLESTONE = PaperMaterialData.ofBlockState(Blocks.MOSSY_COBBLESTONE.defaultBlockState());
		SNOW = PaperMaterialData.ofBlockState(Blocks.SNOW.defaultBlockState());
		SNOW_BLOCK = PaperMaterialData.ofBlockState(Blocks.SNOW_BLOCK.defaultBlockState());
		TORCH = PaperMaterialData.ofBlockState(Blocks.TORCH.defaultBlockState());
		BEDROCK = PaperMaterialData.ofBlockState(Blocks.BEDROCK.defaultBlockState());
		MAGMA = PaperMaterialData.ofBlockState(Blocks.MAGMA_BLOCK.defaultBlockState());
		ICE = PaperMaterialData.ofBlockState(Blocks.ICE.defaultBlockState());
		PACKED_ICE = PaperMaterialData.ofBlockState(Blocks.PACKED_ICE.defaultBlockState());
		BLUE_ICE = PaperMaterialData.ofBlockState(Blocks.BLUE_ICE.defaultBlockState());
		FROSTED_ICE = PaperMaterialData.ofBlockState(Blocks.FROSTED_ICE.defaultBlockState());
		GLOWSTONE = PaperMaterialData.ofBlockState(Blocks.GLOWSTONE.defaultBlockState());
		MYCELIUM = PaperMaterialData.ofBlockState(Blocks.MYCELIUM.defaultBlockState());
		STONE_SLAB = PaperMaterialData.ofBlockState(Blocks.STONE_SLAB.defaultBlockState());

		// Liquids
		WATER = PaperMaterialData.ofBlockState(Blocks.WATER.defaultBlockState());
		LAVA = PaperMaterialData.ofBlockState(Blocks.LAVA.defaultBlockState());

		// Trees
		ACACIA_LOG = PaperMaterialData.ofBlockState(Blocks.ACACIA_LOG.defaultBlockState());
		BIRCH_LOG = PaperMaterialData.ofBlockState(Blocks.BIRCH_LOG.defaultBlockState());
		DARK_OAK_LOG = PaperMaterialData.ofBlockState(Blocks.DARK_OAK_LOG.defaultBlockState());
		OAK_LOG = PaperMaterialData.ofBlockState(Blocks.OAK_LOG.defaultBlockState());
		SPRUCE_LOG = PaperMaterialData.ofBlockState(Blocks.SPRUCE_LOG.defaultBlockState());
		JUNGLE_LOG = PaperMaterialData.ofBlockState(Blocks.JUNGLE_LOG.defaultBlockState());
		ACACIA_WOOD = PaperMaterialData.ofBlockState(Blocks.ACACIA_WOOD.defaultBlockState());
		BIRCH_WOOD = PaperMaterialData.ofBlockState(Blocks.BIRCH_WOOD.defaultBlockState());
		DARK_OAK_WOOD = PaperMaterialData.ofBlockState(Blocks.DARK_OAK_WOOD.defaultBlockState());
		OAK_WOOD = PaperMaterialData.ofBlockState(Blocks.OAK_WOOD.defaultBlockState());
		SPRUCE_WOOD = PaperMaterialData.ofBlockState(Blocks.SPRUCE_WOOD.defaultBlockState());
		JUNGLE_WOOD = PaperMaterialData.ofBlockState(Blocks.JUNGLE_WOOD.defaultBlockState());

		STRIPPED_ACACIA_LOG = PaperMaterialData.ofBlockState(Blocks.STRIPPED_ACACIA_LOG.defaultBlockState());
		STRIPPED_BIRCH_LOG = PaperMaterialData.ofBlockState(Blocks.STRIPPED_BIRCH_LOG.defaultBlockState());
		STRIPPED_DARK_OAK_LOG = PaperMaterialData.ofBlockState(Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
		STRIPPED_JUNGLE_LOG = PaperMaterialData.ofBlockState(Blocks.STRIPPED_JUNGLE_LOG.defaultBlockState());
		STRIPPED_OAK_LOG = PaperMaterialData.ofBlockState(Blocks.STRIPPED_OAK_LOG.defaultBlockState());
		STRIPPED_SPRUCE_LOG = PaperMaterialData.ofBlockState(Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
		
		ACACIA_LEAVES = PaperMaterialData.ofBlockState(Blocks.ACACIA_LEAVES.defaultBlockState());
		BIRCH_LEAVES = PaperMaterialData.ofBlockState(Blocks.BIRCH_LEAVES.defaultBlockState());
		DARK_OAK_LEAVES = PaperMaterialData.ofBlockState(Blocks.DARK_OAK_LEAVES.defaultBlockState());
		JUNGLE_LEAVES = PaperMaterialData.ofBlockState(Blocks.JUNGLE_LEAVES.defaultBlockState());
		OAK_LEAVES = PaperMaterialData.ofBlockState(Blocks.OAK_LEAVES.defaultBlockState());
		SPRUCE_LEAVES = PaperMaterialData.ofBlockState(Blocks.SPRUCE_LEAVES.defaultBlockState());

		// Plants
		POPPY = PaperMaterialData.ofBlockState(Blocks.POPPY.defaultBlockState());
		BLUE_ORCHID = PaperMaterialData.ofBlockState(Blocks.BLUE_ORCHID.defaultBlockState());
		ALLIUM = PaperMaterialData.ofBlockState(Blocks.ALLIUM.defaultBlockState());
		AZURE_BLUET = PaperMaterialData.ofBlockState(Blocks.AZURE_BLUET.defaultBlockState());
		RED_TULIP = PaperMaterialData.ofBlockState(Blocks.RED_TULIP.defaultBlockState());
		ORANGE_TULIP = PaperMaterialData.ofBlockState(Blocks.ORANGE_TULIP.defaultBlockState());
		WHITE_TULIP = PaperMaterialData.ofBlockState(Blocks.WHITE_TULIP.defaultBlockState());
		PINK_TULIP = PaperMaterialData.ofBlockState(Blocks.PINK_TULIP.defaultBlockState());
		OXEYE_DAISY = PaperMaterialData.ofBlockState(Blocks.OXEYE_DAISY.defaultBlockState());		
		YELLOW_FLOWER = PaperMaterialData.ofBlockState(Blocks.DANDELION.defaultBlockState());
		DEAD_BUSH = PaperMaterialData.ofBlockState(Blocks.DEAD_BUSH.defaultBlockState());
		FERN = PaperMaterialData.ofBlockState(Blocks.FERN.defaultBlockState());
		LONG_GRASS = PaperMaterialData.ofBlockState(Blocks.SHORT_GRASS.defaultBlockState());
		
		RED_MUSHROOM_BLOCK = PaperMaterialData.ofBlockState(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState());
		BROWN_MUSHROOM_BLOCK = PaperMaterialData.ofBlockState(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState());		
		RED_MUSHROOM = PaperMaterialData.ofBlockState(Blocks.RED_MUSHROOM.defaultBlockState());
		BROWN_MUSHROOM = PaperMaterialData.ofBlockState(Blocks.BROWN_MUSHROOM.defaultBlockState());

		DOUBLE_TALL_GRASS_LOWER = PaperMaterialData.ofBlockState(Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		DOUBLE_TALL_GRASS_UPPER = PaperMaterialData.ofBlockState(Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
		LARGE_FERN_LOWER = PaperMaterialData.ofBlockState(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		LARGE_FERN_UPPER = PaperMaterialData.ofBlockState(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));		
		LILAC_LOWER = PaperMaterialData.ofBlockState(Blocks.LILAC.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		LILAC_UPPER = PaperMaterialData.ofBlockState(Blocks.LILAC.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));		
		PEONY_LOWER = PaperMaterialData.ofBlockState(Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		PEONY_UPPER = PaperMaterialData.ofBlockState(Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));		
		ROSE_BUSH_LOWER = PaperMaterialData.ofBlockState(Blocks.ROSE_BUSH.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		ROSE_BUSH_UPPER = PaperMaterialData.ofBlockState(Blocks.ROSE_BUSH.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));	
		SUNFLOWER_LOWER = PaperMaterialData.ofBlockState(Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		SUNFLOWER_UPPER = PaperMaterialData.ofBlockState(Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));

		ACACIA_SAPLING = PaperMaterialData.ofBlockState(Blocks.ACACIA_SAPLING.defaultBlockState());
		BAMBOO_SAPLING = PaperMaterialData.ofBlockState(Blocks.BAMBOO_SAPLING.defaultBlockState());
		BIRCH_SAPLING = PaperMaterialData.ofBlockState(Blocks.BIRCH_SAPLING.defaultBlockState());
		DARK_OAK_SAPLING = PaperMaterialData.ofBlockState(Blocks.DARK_OAK_SAPLING.defaultBlockState());
		JUNGLE_SAPLING = PaperMaterialData.ofBlockState(Blocks.JUNGLE_SAPLING.defaultBlockState());
		OAK_SAPLING = PaperMaterialData.ofBlockState(Blocks.OAK_SAPLING.defaultBlockState());
		SPRUCE_SAPLING = PaperMaterialData.ofBlockState(Blocks.SPRUCE_SAPLING.defaultBlockState());
		
		PUMPKIN = PaperMaterialData.ofBlockState(Blocks.PUMPKIN.defaultBlockState());
		CACTUS = PaperMaterialData.ofBlockState(Blocks.CACTUS.defaultBlockState());
		MELON_BLOCK = PaperMaterialData.ofBlockState(Blocks.MELON.defaultBlockState());
		VINE = PaperMaterialData.ofBlockState(Blocks.VINE.defaultBlockState());
		WATER_LILY = PaperMaterialData.ofBlockState(Blocks.LILY_PAD.defaultBlockState());
		SUGAR_CANE_BLOCK = PaperMaterialData.ofBlockState(Blocks.SUGAR_CANE.defaultBlockState());
		
		BlockState bambooState = Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1).setValue(BambooStalkBlock.LEAVES, BambooLeaves.NONE).setValue(BambooStalkBlock.STAGE, 0);
		BAMBOO = PaperMaterialData.ofBlockState(bambooState);
		BAMBOO_SMALL = PaperMaterialData.ofBlockState(bambooState.setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL));
		BAMBOO_LARGE = PaperMaterialData.ofBlockState(bambooState.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE));
		BAMBOO_LARGE_GROWING = PaperMaterialData.ofBlockState(bambooState.setValue(BambooStalkBlock.LEAVES, BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE, 1));
		PODZOL = PaperMaterialData.ofBlockState(Blocks.PODZOL.defaultBlockState());
		SEAGRASS = PaperMaterialData.ofBlockState(Blocks.SEAGRASS.defaultBlockState());
		TALL_SEAGRASS_LOWER = PaperMaterialData.ofBlockState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.LOWER));
		TALL_SEAGRASS_UPPER = PaperMaterialData.ofBlockState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER));
		KELP = PaperMaterialData.ofBlockState(Blocks.KELP.defaultBlockState());
		KELP_PLANT = PaperMaterialData.ofBlockState(Blocks.KELP_PLANT.defaultBlockState());
		VINE_SOUTH = PaperMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true));
		VINE_NORTH = PaperMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.NORTH, true));
		VINE_WEST = PaperMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST, true));
		VINE_EAST = PaperMaterialData.ofBlockState(Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true));
		SEA_PICKLE = PaperMaterialData.ofBlockState(Blocks.SEA_PICKLE.defaultBlockState());

		// Ores
		COAL_ORE = PaperMaterialData.ofBlockState(Blocks.COAL_ORE.defaultBlockState());
		DIAMOND_ORE = PaperMaterialData.ofBlockState(Blocks.DIAMOND_ORE.defaultBlockState());
		EMERALD_ORE = PaperMaterialData.ofBlockState(Blocks.EMERALD_ORE.defaultBlockState());
		GOLD_ORE = PaperMaterialData.ofBlockState(Blocks.GOLD_ORE.defaultBlockState());
		IRON_ORE = PaperMaterialData.ofBlockState(Blocks.IRON_ORE.defaultBlockState());
		LAPIS_ORE = PaperMaterialData.ofBlockState(Blocks.LAPIS_ORE.defaultBlockState());
		QUARTZ_ORE = PaperMaterialData.ofBlockState(Blocks.NETHER_QUARTZ_ORE.defaultBlockState());
		REDSTONE_ORE = PaperMaterialData.ofBlockState(Blocks.REDSTONE_ORE.defaultBlockState());

		// Ore blocks
		GOLD_BLOCK = PaperMaterialData.ofBlockState(Blocks.GOLD_BLOCK.defaultBlockState());
		IRON_BLOCK = PaperMaterialData.ofBlockState(Blocks.IRON_BLOCK.defaultBlockState());
		REDSTONE_BLOCK = PaperMaterialData.ofBlockState(Blocks.REDSTONE_BLOCK.defaultBlockState());
		DIAMOND_BLOCK = PaperMaterialData.ofBlockState(Blocks.DIAMOND_BLOCK.defaultBlockState());
		LAPIS_BLOCK = PaperMaterialData.ofBlockState(Blocks.LAPIS_BLOCK.defaultBlockState());
		COAL_BLOCK = PaperMaterialData.ofBlockState(Blocks.COAL_BLOCK.defaultBlockState());
		QUARTZ_BLOCK = PaperMaterialData.ofBlockState(Blocks.QUARTZ_BLOCK.defaultBlockState());
		EMERALD_BLOCK = PaperMaterialData.ofBlockState(Blocks.EMERALD_BLOCK.defaultBlockState());

		BERRY_BUSH = PaperMaterialData.ofBlockState(Blocks.SWEET_BERRY_BUSH.defaultBlockState());
	}
}
