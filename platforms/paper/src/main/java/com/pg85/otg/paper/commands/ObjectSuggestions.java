package com.pg85.otg.paper.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.customobject.creator.ObjectType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Suggestions for the object commands. */
final class ObjectSuggestions {

    private static final Function<String, String> filterNamesWithSpaces = (name -> name.contains(" ") ? "\"" + name + "\"" : name);

    static CompletableFuture<Suggestions> suggestPresets(SuggestionsBuilder builder, boolean global) {
        Set<String> set = OTG.getEngine().getPresetLoader().getAllPresetFolderNames().stream()
            .map(filterNamesWithSpaces).collect(Collectors.toSet());
        if (global) {
            set.add("global");
        }
        return SharedSuggestionProvider.suggest(set, builder);
    }

    static CompletableFuture<Suggestions> suggestObjects(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String presetFolderName = context.getArgument("preset", String.class);
        List<String> list;
        // Get global objects if global, else fetch based on preset
        if (presetFolderName.equalsIgnoreCase("global")) {
            list = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getGlobalObjectNames(OTG.getEngine().getOTGRootFolder());
        } else {
            list = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getAllBONamesForPreset(presetFolderName, OTG.getEngine().getOTGRootFolder());
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        return SharedSuggestionProvider.suggest(list.stream().map(filterNamesWithSpaces), builder);
    }

    static CompletableFuture<Suggestions> suggestTemplates(SuggestionsBuilder builder, String preset) {
        List<String> list;
        // Get global objects if global, else fetch based on preset
        if (preset.equalsIgnoreCase("global")) {
            list = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getGlobalTemplates(OTG.getEngine().getOTGRootFolder());
        } else {
            list = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getTemplatesForPreset(preset, OTG.getEngine().getOTGRootFolder());
        }
        if (list == null) {
            list = new ArrayList<>();
        }
        list = list.stream().map(filterNamesWithSpaces).collect(Collectors.toList());
        list.add("default");
        return SharedSuggestionProvider.suggest(list, builder);
    }

    static CompletableFuture<Suggestions> suggestTypes(SuggestionsBuilder builder) {
        // BO2 objects can't be exported
        return SharedSuggestionProvider.suggest(Stream.of(ObjectType.values()).filter(type -> type != ObjectType.BO2).map(ObjectType::getType), builder);
    }

    private ObjectSuggestions() {}
}
