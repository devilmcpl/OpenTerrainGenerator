package com.pg85.otg.paper.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pg85.otg.paper.commands.region.Region;
import com.pg85.otg.paper.commands.region.RegionSelections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class RegionCommand {

    private static final String[] DIRECTION_OPTIONS = new String[]{"north", "south", "east", "west", "up", "down"};
    private static final String[] REGION_MARKER_OPTIONS = new String[]{"1", "2", "center"};

    static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("region")
            // /otg region mark [1|2|center]
            .then(Commands.literal("mark")
                .executes(ctx -> mark(ctx.getSource()))
                .then(Commands.argument("point", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(REGION_MARKER_OPTIONS, builder))
                    .executes(ctx -> mark(ctx.getSource(), StringArgumentType.getString(ctx, "point")))))
            // /otg region clear
            .then(Commands.literal("clear")
                .executes(ctx -> clear(ctx.getSource())))
            // /otg region expand <direction> <value>
            .then(Commands.literal("expand")
                .then(Commands.argument("direction", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DIRECTION_OPTIONS, builder))
                    .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ctx -> expand(ctx.getSource(),
                            StringArgumentType.getString(ctx, "direction"),
                            IntegerArgumentType.getInteger(ctx, "value"))))))
            // /otg region shrink <direction> <value>
            .then(Commands.literal("shrink")
                .then(Commands.argument("direction", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DIRECTION_OPTIONS, builder))
                    .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(ctx -> expand(ctx.getSource(),
                            StringArgumentType.getString(ctx, "direction"),
                            -IntegerArgumentType.getInteger(ctx, "value"))))));
    }

    private static int mark(CommandSourceStack src) {
        ServerPlayer player = CommandHelper.requirePlayer(src);
        if (player == null) return 0;
        if (RegionSelections.get(player.getUUID()).setPos(player.blockPosition())) {
            src.sendSuccess(() -> Component.literal("Point 1 marked"), false);
        } else {
            src.sendSuccess(() -> Component.literal("Point 2 marked"), false);
        }
        return 1;
    }

    private static int mark(CommandSourceStack src, String input) {
        ServerPlayer player = CommandHelper.requirePlayer(src);
        if (player == null) return 0;
        Region region = RegionSelections.get(player.getUUID());
        switch (input) {
            case "min", "pos1", "1" -> {
                region.setPos1(player.blockPosition());
                src.sendSuccess(() -> Component.literal("Point 1 marked"), false);
            }
            case "max", "pos2", "2" -> {
                region.setPos2(player.blockPosition());
                src.sendSuccess(() -> Component.literal("Point 2 marked"), false);
            }
            case "center" -> {
                region.setCenter(Region.cornerFromBlockPos(player.blockPosition()));
                src.sendSuccess(() -> Component.literal("Center marked"), false);
            }
            default -> {
                src.sendFailure(Component.literal(input + " is not recognized"));
                return 0;
            }
        }
        return 1;
    }

    private static int clear(CommandSourceStack src) {
        ServerPlayer player = CommandHelper.requirePlayer(src);
        if (player == null) return 0;
        RegionSelections.get(player.getUUID()).clear();
        src.sendSuccess(() -> Component.literal("Position cleared"), false);
        return 1;
    }

    private static int expand(CommandSourceStack src, String direction, int value) {
        ServerPlayer player = CommandHelper.requirePlayer(src);
        if (player == null) return 0;
        Region region = RegionSelections.get(player.getUUID());
        if (region.getMax() == null) {
            src.sendFailure(Component.literal("Please mark two positions before modifying or exporting the region"));
            return 0;
        }

        switch (direction) {
            case "south" -> { // positive Z
                if (region.getPos2().getZ() >= region.getPos1().getZ()) region.setPos2(region.getPos2().south(value));
                else region.setPos1(region.getPos1().south(value));
            }
            case "north" -> { // negative Z
                if (region.getPos2().getZ() < region.getPos1().getZ()) region.setPos2(region.getPos2().north(value));
                else region.setPos1(region.getPos1().north(value));
            }
            case "east" -> { // positive X
                if (region.getPos2().getX() >= region.getPos1().getX()) region.setPos2(region.getPos2().east(value));
                else region.setPos1(region.getPos1().east(value));
            }
            case "west" -> { // negative X
                if (region.getPos2().getX() < region.getPos1().getX()) region.setPos2(region.getPos2().west(value));
                else region.setPos1(region.getPos1().west(value));
            }
            case "up" -> { // positive y
                if (region.getPos2().getY() >= region.getPos1().getY()) region.setPos2(region.getPos2().above(value));
                else region.setPos1(region.getPos1().above(value));
            }
            case "down" -> { // negative y
                if (region.getPos2().getY() < region.getPos1().getY()) region.setPos2(region.getPos2().below(value));
                else region.setPos1(region.getPos1().below(value));
            }
            default -> {
                src.sendFailure(Component.literal("Unrecognized direction " + direction));
                return 0;
            }
        }

        src.sendSuccess(() -> Component.literal("Region modified"), false);
        return 1;
    }

    private RegionCommand() {}
}
