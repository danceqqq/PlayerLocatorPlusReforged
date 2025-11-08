package com.myangel.playerlocatorplus.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.myangel.playerlocatorplus.config.ColorMode;
import com.myangel.playerlocatorplus.config.PlayerLocatorConfig;
import com.myangel.playerlocatorplus.server.ServerTracker;
import com.myangel.playerlocatorplus.util.PlayerDataState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Locale;

public final class PLPCommand {
    private static final SimpleCommandExceptionType INVALID_COLOR = new SimpleCommandExceptionType(Component.translatable("argument.color.invalid"));

    private PLPCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("plp")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ServerTracker.markConfigDirty();
                            ServerTracker.fullResend(source.getServer());
                            source.sendSuccess(() -> Component.literal("Player Locator config reloaded"), false);
                            return 1;
                        }))
                .then(Commands.literal("random")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerTracker.sendFakePlayers(player);
                            return 1;
                        }))
                .then(Commands.literal("color")
                        .then(Commands.argument("color", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(ChatFormatting.values())
                                        .filter(ChatFormatting::isColor)
                                        .map(formatting -> formatting.getName().toLowerCase(Locale.ROOT)), builder))
                                .executes(ctx -> changeColor(ctx, true))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> changeColor(ctx, false))))));
    }

    private static int changeColor(CommandContext<CommandSourceStack> ctx, boolean self) throws CommandSyntaxException {
        if (PlayerLocatorConfig.getServerValues().colorMode() != ColorMode.CUSTOM) {
            ctx.getSource().sendFailure(Component.translatable("commands.player-locator-plus.color.wrong-color-mode"));
            return 0;
        }

        ServerPlayer target = self ? ctx.getSource().getPlayerOrException() : EntityArgument.getPlayer(ctx, "player");
        String colorInput = StringArgumentType.getString(ctx, "color");
        int color = parseColor(colorInput);

        PlayerDataState state = PlayerDataState.get(ctx.getSource().getServer());
        state.setCustomColor(target.getUUID(), color);

        if (self) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.player-locator-plus.color.self",
                    formatColor(color)
            ), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.player-locator-plus.color.other",
                    target.getName(),
                    formatColor(color)
            ), false);
        }
        return 1;
    }

    private static int parseColor(String input) throws CommandSyntaxException {
        String value = input.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 6 && value.chars().allMatch(ch -> Character.digit(ch, 16) != -1)) {
            return Integer.parseInt(value, 16);
        }
        ChatFormatting formatting = ChatFormatting.getByName(value);
        if (formatting != null && formatting.getColor() != null) {
            return formatting.getColor();
        }
        throw INVALID_COLOR.create();
    }

    private static Component formatColor(int color) {
        String hex = String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
        return Component.literal(hex).withStyle(style -> style.withColor(color & 0xFFFFFF));
    }
}
