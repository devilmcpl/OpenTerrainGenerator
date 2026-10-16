package com.pg85.otg.paper.util;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * Access to Minecraft internals that the Fabric platform reaches through mixin accessors.
 * Paper runs Mojang mapped, so the Mojang names are used directly.
 */
public final class PaperReflection {
    private static final MethodHandle WORLD_GEN_REGION_CACHE;
    private static final MethodHandle NOISE_CHUNK_GET_INTERPOLATED_STATE;

    static {
        try {
            WORLD_GEN_REGION_CACHE = MethodHandles.privateLookupIn(WorldGenRegion.class, MethodHandles.lookup())
                    .findGetter(WorldGenRegion.class, "cache", StaticCache2D.class);
            NOISE_CHUNK_GET_INTERPOLATED_STATE = MethodHandles.privateLookupIn(NoiseChunk.class, MethodHandles.lookup())
                    .findVirtual(NoiseChunk.class, "getInterpolatedState", MethodType.methodType(BlockState.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static StaticCache2D<GenerationChunkHolder> getCache(WorldGenRegion worldGenRegion) {
        try {
            return (StaticCache2D<GenerationChunkHolder>) (StaticCache2D<?>) WORLD_GEN_REGION_CACHE.invokeExact(worldGenRegion);
        } catch (Throwable t) {
            throw new IllegalStateException("Could not access WorldGenRegion cache", t);
        }
    }

    public static BlockState getInterpolatedState(NoiseChunk noiseChunk) {
        try {
            return (BlockState) NOISE_CHUNK_GET_INTERPOLATED_STATE.invokeExact(noiseChunk);
        } catch (Throwable t) {
            throw new IllegalStateException("Could not access NoiseChunk interpolated state", t);
        }
    }

    public static Field getField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not find field " + owner.getName() + "." + name, e);
        }
    }

    public static void setField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set field " + field.getDeclaringClass().getName() + "." + field.getName(), e);
        }
    }

    private PaperReflection() {}
}
