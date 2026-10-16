package com.pg85.otg.fabric.mixin.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class RegistryUtil {
    public static <T> Optional<Holder.Reference<T>> getAsReference(Registry<T> registry, Identifier key) {
        return registry.getOptional(key)
                .flatMap(registry::getResourceKey)
                .flatMap(registry::get);
    }
}
