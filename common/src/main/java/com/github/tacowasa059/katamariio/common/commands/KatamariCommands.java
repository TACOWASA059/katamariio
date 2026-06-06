package com.github.tacowasa059.katamariio.common.commands;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class KatamariCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("katamari")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("size")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 20.0f))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> {
                                                ICustomPlayerData data = (ICustomPlayerData) p;
                                                data.katamariIO$setSize(value);
                                                data.katamariIO$setRenderSize(value);
                                            });
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Set size to " +
                                                            ChatFormatting.AQUA + value + ChatFormatting.GREEN + " for " + players.size() + " player(s)."), false);
                                            return 1;
                                        })))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                                    float size = ((ICustomPlayerData) player).katamariIO$getSize();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Player size: " + ChatFormatting.AQUA + size), false);
                                    return 1;
                                })))
                .then(Commands.literal("restitution")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 1.2f))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> ((ICustomPlayerData) p).katamariIO$setRestitutionCoefficient(value));
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Set restitution to " +
                                                            ChatFormatting.AQUA + value + ChatFormatting.GREEN + " for " + players.size() + " player(s)."), false);
                                            return 1;
                                        })))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                                    float value = ((ICustomPlayerData) player).katamariIO$getRestitutionCoefficient();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Restitution coefficient: " + ChatFormatting.AQUA + value), false);
                                    return 1;
                                })))
                .then(Commands.literal("ball")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean flag = BoolArgumentType.getBool(ctx, "enabled");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> {
                                                ICustomPlayerData data = (ICustomPlayerData) p;
                                                if (flag) {
                                                    data.katamariIO$setFlag(true);
                                                } else {
                                                    data.katamariIO$setFlagAndSizeAndRestitution(
                                                            false,
                                                            KatamariIO.DEFAULT_BALL_SIZE,
                                                            KatamariIO.DEFAULT_BALL_SIZE,
                                                            data.katamariIO$getRestitutionCoefficient()
                                                    );
                                                }
                                            });
                                            ChatFormatting flagColor = flag ? ChatFormatting.AQUA : ChatFormatting.RED;
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Set ball flag to " + flagColor + flag +
                                                            ChatFormatting.GREEN + " for " + players.size() + " player(s)."), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("resetBlocks")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    players.forEach(p -> {
                                        ICustomPlayerData data = (ICustomPlayerData) p;
                                        data.katamariIO$setFlagAndSizeAndRestitution(
                                                data.katamariIO$getFlag(),
                                                KatamariIO.DEFAULT_BALL_SIZE,
                                                KatamariIO.DEFAULT_BALL_SIZE,
                                                data.katamariIO$getRestitutionCoefficient()
                                        );
                                        data.katamariIO$clearAttachedBlocks();
                                    });
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            ChatFormatting.GOLD + "[KatamariIO] " + ChatFormatting.GREEN + "Cleared attached blocks for " +
                                                    ChatFormatting.AQUA + players.size() + ChatFormatting.GREEN + " player(s)."), true);
                                    return players.size();
                                }))));
    }
}
