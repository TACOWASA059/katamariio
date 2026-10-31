package com.github.tacowasa059.katamariio.common.commands;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.config.KatamariConfig;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.Collection;
import java.util.stream.Stream;

public class KatamariCommands {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BLOCK_OR_TAG = (context, builder) -> {
        Stream<String> tagSuggestions = BuiltInRegistries.BLOCK.getTagNames()
                .map(tagKey -> "#" + tagKey.location());
        Stream<String> blockSuggestions = BuiltInRegistries.BLOCK.keySet().stream()
                .map(ResourceLocation::toString);
        return SharedSuggestionProvider.suggest(Stream.concat(tagSuggestions, blockSuggestions), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BLOCK_FILTER_LIST = (context, builder) ->
            SharedSuggestionProvider.suggest(KatamariConfig.get().blockFilterList, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ENTITY_OR_TAG = (context, builder) -> {
        Stream<String> tagSuggestions = BuiltInRegistries.ENTITY_TYPE.getTagNames()
                .map(tagKey -> "#" + tagKey.location());
        Stream<String> entitySuggestions = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(ResourceLocation::toString);
        return SharedSuggestionProvider.suggest(Stream.concat(tagSuggestions, entitySuggestions), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ENTITY_FILTER_LIST = (context, builder) ->
            SharedSuggestionProvider.suggest(KatamariConfig.get().entityFilterList, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("katamari")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> sendHelp(ctx.getSource()))
                .then(Commands.literal("size")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer player : players) {
                                        float size = ((ICustomPlayerData) player).katamariIO$getSize();
                                        float outerSize = ((ICustomPlayerData) player).katamariIO$getOuterSize();
                                        if (Math.abs(outerSize - size) > 0.05f) {
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.katamariio.size.query.outer", player.getName().getString(), String.format("%.2f", size), String.format("%.2f", outerSize)), false);
                                        } else {
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.katamariio.size.query", player.getName().getString(), String.format("%.2f", size)), false);
                                        }
                                    }
                                    return players.size();
                                })
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 100.0f))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> {
                                                ICustomPlayerData data = (ICustomPlayerData) p;
                                                data.katamariIO$setSize(value);
                                                data.katamariIO$setRenderSize(value);
                                            });
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.katamariio.size.set", value, players.size()), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("restitution")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer player : players) {
                                        float value = ((ICustomPlayerData) player).katamariIO$getRestitutionCoefficient();
                                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                                "commands.katamariio.restitution.query", player.getName().getString(), value), false);
                                    }
                                    return players.size();
                                })
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.01f, 1.2f))
                                        .executes(ctx -> {
                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            players.forEach(p -> ((ICustomPlayerData) p).katamariIO$setRestitutionCoefficient(value));
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.katamariio.restitution.set", value, players.size()), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("ball")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    for (ServerPlayer p : players) {
                                        boolean flag = ((ICustomPlayerData) p).katamariIO$getFlag();
                                        String flagStr = (flag ? "§b" : "§c") + flag;
                                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                                "commands.katamariio.ball.query", p.getName().getString(), flagStr), false);
                                    }
                                    return players.size();
                                })
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
                                                    KatamariEntityAttacher.releaseAll(p);
                                                }
                                            });
                                            String flagStr = (flag ? "§b" : "§c") + flag;
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.katamariio.ball.set", flagStr, players.size()), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("resetBlocks")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    players.forEach(p -> ((ICustomPlayerData) p).katamariIO$clearAttachedBlocks());
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "commands.katamariio.reset_blocks.success", players.size()), false);
                                    return players.size();
                                })))
                .then(Commands.literal("resetEntities")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    players.forEach(p -> ((ICustomPlayerData) p).katamariIO$clearAttachedEntities());
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "commands.katamariio.reset_entities.success", players.size()), false);
                                    return players.size();
                                })))
                .then(Commands.literal("resetAll")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                    players.forEach(p -> ((ICustomPlayerData) p).katamariIO$resetAll());
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "commands.katamariio.reset_all.success", players.size()), false);
                                    return players.size();
                                })))
                .then(Commands.literal("help")
                        .executes(ctx -> sendHelp(ctx.getSource())))
                // Full config subcommands: /katamari config ...
                .then(Commands.literal("config")
                        .executes(ctx -> sendConfigStatus(ctx.getSource()))
                        .then(Commands.literal("get")
                                .executes(ctx -> sendConfigStatus(ctx.getSource())))
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    KatamariConfig.load();
                                    if (ctx.getSource().getServer() != null) {
                                        com.github.tacowasa059.katamariio.common.networks.ModNetwork.broadcastConfigSync(ctx.getSource().getServer());
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                             "commands.katamariio.config.reload"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("preset")
                                .then(Commands.literal("fast")
                                        .executes(ctx -> applyPreset(ctx.getSource(), KatamariConfig.Preset.FAST)))
                                .then(Commands.literal("balanced")
                                        .executes(ctx -> applyPreset(ctx.getSource(), KatamariConfig.Preset.BALANCED)))
                                .then(Commands.literal("strict")
                                        .executes(ctx -> applyPreset(ctx.getSource(), KatamariConfig.Preset.STRICT))))
                        .then(Commands.literal("mode")
                                .executes(ctx -> sendConfigStatus(ctx.getSource()))
                                .then(Commands.literal("blocks")
                                        .executes(ctx -> {
                                            setAttachBlocks(ctx.getSource(), true);
                                            return setAttachEntities(ctx.getSource(), false);
                                        }))
                                .then(Commands.literal("entities")
                                        .executes(ctx -> {
                                            setAttachBlocks(ctx.getSource(), false);
                                            return setAttachEntities(ctx.getSource(), true);
                                        }))
                                .then(Commands.literal("safe")
                                        .executes(ctx -> {
                                            setAttachBlocks(ctx.getSource(), false);
                                            return setAttachEntities(ctx.getSource(), false);
                                        }))
                                .then(Commands.literal("face")
                                        .executes(ctx -> {
                                            setAttachBlocks(ctx.getSource(), false);
                                            return setAttachEntities(ctx.getSource(), false);
                                        }))
                                .then(Commands.literal("full")
                                        .executes(ctx -> {
                                            setAttachBlocks(ctx.getSource(), true);
                                            return setAttachEntities(ctx.getSource(), true);
                                        })))
                        .then(Commands.literal("attachBlocks")
                                .executes(ctx -> getAttachBlocks(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAttachBlocks(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("attachEntities")
                                .executes(ctx -> getAttachEntities(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAttachEntities(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("renderAttachedBlocks")
                                .executes(ctx -> getRenderAttachedBlocks(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setRenderAttachedBlocks(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("maxSize")
                                .executes(ctx -> getMaxSize(ctx.getSource()))
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-1.0f, 500.0f))
                                        .executes(ctx -> setMaxSize(ctx.getSource(), FloatArgumentType.getFloat(ctx, "value")))))
                        .then(Commands.literal("removalsPerTick")
                                .executes(ctx -> getRemovalsPerTick(ctx.getSource()))
                                .then(Commands.argument("value", IntegerArgumentType.integer(-1, 1024))
                                        .executes(ctx -> setRemovalsPerTick(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("maxAttachedEntities")
                                .executes(ctx -> getMaxAttachedEntities(ctx.getSource()))
                                .then(Commands.argument("value", IntegerArgumentType.integer(-1, 256))
                                        .executes(ctx -> setMaxAttachedEntities(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("prune")
                                .executes(ctx -> getPrune(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setPrune(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("indicator")
                                .executes(ctx -> getIndicator(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setIndicator(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("disableFlyCheck")
                                .executes(ctx -> getDisableFlyCheck(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setDisableFlyCheck(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("protectPlayers")
                                .executes(ctx -> getProtectPlayers(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setProtectPlayers(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("attachBalls")
                                .executes(ctx -> getAttachBalls(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAttachBalls(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("attachWhileRiding")
                                .executes(ctx -> getAttachWhileRiding(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAttachWhileRiding(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("allowSelfDismount")
                                .executes(ctx -> getAllowSelfDismount(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAllowSelfDismount(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("allowPlayerDismount")
                                .executes(ctx -> getAllowSelfDismount(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setAllowSelfDismount(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("protectBosses")
                                .executes(ctx -> getProtectBosses(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setProtectBosses(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("protectNamed")
                                .executes(ctx -> getProtectNamed(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setProtectNamed(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("protectTamed")
                                .executes(ctx -> getProtectTamed(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setProtectTamed(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("protectIndestructible")
                                .executes(ctx -> getProtectIndestructible(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setProtectIndestructible(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("collisionBins")
                                .executes(ctx -> getCollisionBins(ctx.getSource()))
                                .then(Commands.argument("value", IntegerArgumentType.integer(8, 512))
                                        .executes(ctx -> setCollisionBins(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                        .then(Commands.literal("filter")
                                .then(Commands.literal("block")
                                        .then(Commands.literal("mode")
                                                .then(Commands.literal("none").executes(ctx -> setBlockFilterMode(ctx.getSource(), KatamariConfig.FilterMode.NONE)))
                                                .then(Commands.literal("blacklist").executes(ctx -> setBlockFilterMode(ctx.getSource(), KatamariConfig.FilterMode.BLACKLIST)))
                                                .then(Commands.literal("whitelist").executes(ctx -> setBlockFilterMode(ctx.getSource(), KatamariConfig.FilterMode.WHITELIST))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                        .suggests(SUGGEST_BLOCK_OR_TAG)
                                                        .executes(ctx -> addBlockFilter(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                        .suggests(SUGGEST_BLOCK_FILTER_LIST)
                                                        .executes(ctx -> removeBlockFilter(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("list")
                                                .executes(ctx -> listBlockFilter(ctx.getSource())))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> clearBlockFilter(ctx.getSource()))))
                                .then(Commands.literal("entity")
                                        .then(Commands.literal("mode")
                                                .then(Commands.literal("none").executes(ctx -> setEntityFilterMode(ctx.getSource(), KatamariConfig.FilterMode.NONE)))
                                                .then(Commands.literal("blacklist").executes(ctx -> setEntityFilterMode(ctx.getSource(), KatamariConfig.FilterMode.BLACKLIST)))
                                                .then(Commands.literal("whitelist").executes(ctx -> setEntityFilterMode(ctx.getSource(), KatamariConfig.FilterMode.WHITELIST))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                        .suggests(SUGGEST_ENTITY_OR_TAG)
                                                        .executes(ctx -> addEntityFilter(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                        .suggests(SUGGEST_ENTITY_FILTER_LIST)
                                                        .executes(ctx -> removeEntityFilter(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "id")))))
                                        .then(Commands.literal("list")
                                                .executes(ctx -> listEntityFilter(ctx.getSource())))
                                        .then(Commands.literal("clear")
                                                .executes(ctx -> clearEntityFilter(ctx.getSource())))))));
    }

    private static void saveAndSync(CommandSourceStack source) {
        KatamariConfig.save();
        if (source != null && source.getServer() != null) {
            com.github.tacowasa059.katamariio.common.networks.ModNetwork.broadcastConfigSync(source.getServer());
        }
    }

    private static int setBlockFilterMode(CommandSourceStack source, KatamariConfig.FilterMode mode) {
        KatamariConfig.get().blockFilterMode = mode;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.block.mode", mode.name()), false);
        return 1;
    }

    private static int addBlockFilter(CommandSourceStack source, String id) {
        String trimmed = id.trim();
        if (!KatamariConfig.get().blockFilterList.contains(trimmed)) {
            KatamariConfig.get().blockFilterList.add(trimmed);
            saveAndSync(source);
        }
        boolean isTag = trimmed.startsWith("#");
        Component kindComp = Component.translatable(isTag ? "commands.katamariio.filter.kind.tag" : "commands.katamariio.filter.kind.block");
        String formattedId = (isTag ? "§d" : "§b") + trimmed;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.block.add", kindComp, formattedId), false);
        return 1;
    }

    private static int removeBlockFilter(CommandSourceStack source, String id) {
        String trimmed = id.trim();
        boolean removed = KatamariConfig.get().blockFilterList.remove(trimmed);
        if (removed) {
            saveAndSync(source);
        }
        boolean isTag = trimmed.startsWith("#");
        Component kindComp = Component.translatable(isTag ? "commands.katamariio.filter.kind.tag" : "commands.katamariio.filter.kind.block");
        String formattedId = (isTag ? "§d" : "§b") + trimmed;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.block.remove", kindComp, formattedId), false);
        return 1;
    }

    private static int listBlockFilter(CommandSourceStack source) {
        KatamariConfig cfg = KatamariConfig.get();
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.block.header", cfg.blockFilterMode.name()), false);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.block.protect_indestructible", cfg.protectIndestructibleBlocks ? "§atrue" : "§cfalse"), false);
        for (String item : cfg.blockFilterList) {
            boolean isTag = item.startsWith("#");
            Component kindComp = Component.translatable(isTag ? "commands.katamariio.filter.kind.tag" : "commands.katamariio.filter.kind.block");
            String colorStr = isTag ? "§d" : "§f";
            source.sendSuccess(() -> Component.translatable("commands.katamariio.filter.item_format", colorStr, kindComp, item), false);
        }
        return cfg.blockFilterList.size();
    }

    private static int clearBlockFilter(CommandSourceStack source) {
        KatamariConfig.get().blockFilterList.clear();
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.block.clear"), false);
        return 1;
    }

    private static int setEntityFilterMode(CommandSourceStack source, KatamariConfig.FilterMode mode) {
        KatamariConfig.get().entityFilterMode = mode;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.mode", mode.name()), false);
        return 1;
    }

    private static int addEntityFilter(CommandSourceStack source, String id) {
        String trimmed = id.trim();
        if (!KatamariConfig.get().entityFilterList.contains(trimmed)) {
            KatamariConfig.get().entityFilterList.add(trimmed);
            saveAndSync(source);
        }
        boolean isTag = trimmed.startsWith("#");
        Component kindComp = Component.translatable(isTag ? "commands.katamariio.filter.kind.tag" : "commands.katamariio.filter.kind.entity");
        String formattedId = (isTag ? "§d" : "§b") + trimmed;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.add", kindComp, formattedId), false);
        return 1;
    }

    private static int removeEntityFilter(CommandSourceStack source, String id) {
        String trimmed = id.trim();
        boolean removed = KatamariConfig.get().entityFilterList.remove(trimmed);
        if (removed) {
            saveAndSync(source);
        }
        boolean isTag = trimmed.startsWith("#");
        Component kindComp = Component.translatable(isTag ? "commands.katamariio.filter.kind.tag" : "commands.katamariio.filter.kind.entity");
        String formattedId = (isTag ? "§d" : "§b") + trimmed;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.remove", kindComp, formattedId), false);
        return 1;
    }

    private static int listEntityFilter(CommandSourceStack source) {
        KatamariConfig cfg = KatamariConfig.get();
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.header", cfg.entityFilterMode.name()), false);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.protect_bosses", cfg.protectBosses ? "§atrue" : "§cfalse"), false);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.protect_named", cfg.protectNamedEntities ? "§atrue" : "§cfalse"), false);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.protect_tamed", cfg.protectTamedEntities ? "§atrue" : "§cfalse"), false);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.protect_players", cfg.protectPlayers ? "§atrue" : "§cfalse"), false);
        for (String item : cfg.entityFilterList) {
            boolean isTag = item.startsWith("#");
            Component kindComp = Component.translatable(isTag ? "commands.katamariio.filter.kind.tag" : "commands.katamariio.filter.kind.entity");
            String colorStr = isTag ? "§d" : "§f";
            source.sendSuccess(() -> Component.translatable("commands.katamariio.filter.item_format", colorStr, kindComp, item), false);
        }
        return cfg.entityFilterList.size();
    }

    private static int clearEntityFilter(CommandSourceStack source) {
        KatamariConfig.get().entityFilterList.clear();
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.filter.entity.clear"), false);
        return 1;
    }

    private static int setProtectBosses(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().protectBosses = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_bosses.set", enabled ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int getProtectBosses(CommandSourceStack source) {
        boolean val = KatamariConfig.get().protectBosses;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_bosses.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setProtectNamed(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().protectNamedEntities = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_named.set", enabled ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int getProtectNamed(CommandSourceStack source) {
        boolean val = KatamariConfig.get().protectNamedEntities;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_named.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setProtectTamed(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().protectTamedEntities = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_tamed.set", enabled ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int getProtectTamed(CommandSourceStack source) {
        boolean val = KatamariConfig.get().protectTamedEntities;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_tamed.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setProtectPlayers(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().protectPlayers = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_players.set", enabled ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int getProtectPlayers(CommandSourceStack source) {
        boolean val = KatamariConfig.get().protectPlayers;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_players.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setProtectIndestructible(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().protectIndestructibleBlocks = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_indestructible.set", enabled ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int getProtectIndestructible(CommandSourceStack source) {
        boolean val = KatamariConfig.get().protectIndestructibleBlocks;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.protect_indestructible.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setAttachBlocks(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().attachBlocks = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(enabled ?
                "commands.katamariio.config.attach_blocks.enabled" : "commands.katamariio.config.attach_blocks.disabled"), false);
        return 1;
    }

    private static int getAttachBlocks(CommandSourceStack source) {
        boolean val = KatamariConfig.get().attachBlocks;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.attach_blocks.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setAttachEntities(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().attachEntities = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(enabled ?
                "commands.katamariio.config.attach_entities.enabled" : "commands.katamariio.config.attach_entities.disabled"), false);
        return 1;
    }

    private static int getAttachEntities(CommandSourceStack source) {
        boolean val = KatamariConfig.get().attachEntities;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.attach_entities.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setAttachBalls(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().attachBalls = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(enabled ?
                "commands.katamariio.config.attach_balls.enabled" : "commands.katamariio.config.attach_balls.disabled"), false);
        return 1;
    }

    private static int getAttachBalls(CommandSourceStack source) {
        boolean val = KatamariConfig.get().attachBalls;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.attach_balls.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setAttachWhileRiding(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().attachWhileRiding = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(enabled ?
                "commands.katamariio.config.attach_while_riding.enabled" : "commands.katamariio.config.attach_while_riding.disabled"), false);
        return 1;
    }

    private static int getAttachWhileRiding(CommandSourceStack source) {
        boolean val = KatamariConfig.get().attachWhileRiding;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.attach_while_riding.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setAllowSelfDismount(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().allowSelfDismount = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(enabled ?
                "commands.katamariio.config.allow_self_dismount.enabled" : "commands.katamariio.config.allow_self_dismount.disabled"), false);
        return 1;
    }

    private static int getAllowSelfDismount(CommandSourceStack source) {
        boolean val = KatamariConfig.get().allowSelfDismount;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.allow_self_dismount.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }


    private static int setMaxSize(CommandSourceStack source, float val) {
        KatamariConfig.get().maxSize = val;
        saveAndSync(source);
        Component desc = val <= 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(val));
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.max_size", desc), false);
        return 1;
    }

    private static int getMaxSize(CommandSourceStack source) {
        float val = KatamariConfig.get().maxSize;
        Component desc = val <= 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(val));
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.max_size.query", desc), false);
        return 1;
    }

    private static int setRemovalsPerTick(CommandSourceStack source, int val) {
        KatamariConfig.get().maxRemovalsPerTick = val;
        saveAndSync(source);
        Component desc = val < 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(val));
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.max_removals_per_tick", desc), false);
        return 1;
    }

    private static int getRemovalsPerTick(CommandSourceStack source) {
        int val = KatamariConfig.get().maxRemovalsPerTick;
        Component desc = val < 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(val));
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.max_removals_per_tick.query", desc), false);
        return 1;
    }

    private static int setPrune(CommandSourceStack source, boolean val) {
        KatamariConfig.get().pruneInnerBlocks = val;
        saveAndSync(source);
        String valStr = (val ? "§b" : "§c") + val;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.prune", valStr), false);
        return 1;
    }

    private static int getPrune(CommandSourceStack source) {
        boolean val = KatamariConfig.get().pruneInnerBlocks;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.prune.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setIndicator(CommandSourceStack source, boolean val) {
        KatamariConfig.get().showPlayerIndicator = val;
        saveAndSync(source);
        String valStr = (val ? "§b" : "§c") + val;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.indicator", valStr), false);
        return 1;
    }

    private static int getIndicator(CommandSourceStack source) {
        boolean val = KatamariConfig.get().showPlayerIndicator;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.indicator.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setDisableFlyCheck(CommandSourceStack source, boolean val) {
        KatamariConfig.get().disableFlyCheck = val;
        saveAndSync(source);
        String valStr = (val ? "§b" : "§c") + val;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.disable_fly_check", valStr), false);
        return 1;
    }

    private static int getDisableFlyCheck(CommandSourceStack source) {
        boolean val = KatamariConfig.get().disableFlyCheck;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.disable_fly_check.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int setMaxAttachedEntities(CommandSourceStack source, int val) {
        KatamariConfig.get().maxAttachedEntities = val;
        saveAndSync(source);
        Component desc = val < 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(val));
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.max_attached_entities.set", desc), false);
        return 1;
    }

    private static int getMaxAttachedEntities(CommandSourceStack source) {
        int val = KatamariConfig.get().maxAttachedEntities;
        Component desc = val < 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(val));
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.max_attached_entities.query", desc), false);
        return 1;
    }

    private static int setRenderAttachedBlocks(CommandSourceStack source, boolean enabled) {
        KatamariConfig.get().renderAttachedBlocks = enabled;
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(enabled ?
                "commands.katamariio.config.render_attached_blocks.enabled" : "commands.katamariio.config.render_attached_blocks.disabled"), false);
        return 1;
    }

    private static int getRenderAttachedBlocks(CommandSourceStack source) {
        boolean val = KatamariConfig.get().renderAttachedBlocks;
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.render_attached_blocks.query", val ? "§btrue" : "§cfalse"), false);
        return 1;
    }

    private static int applyPreset(CommandSourceStack source, KatamariConfig.Preset preset) {
        KatamariConfig.get().applyPreset(preset);
        if (source != null && source.getServer() != null) {
            com.github.tacowasa059.katamariio.common.networks.ModNetwork.broadcastConfigSync(source.getServer());
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.preset.applied", preset.name()), false);
        return 1;
    }

    private static int setCollisionBins(CommandSourceStack source, int val) {
        KatamariConfig.get().collisionBins = Math.max(8, Math.min(512, val));
        saveAndSync(source);
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.collision_bins.set", val), false);
        return 1;
    }

    private static int getCollisionBins(CommandSourceStack source) {
        int val = KatamariConfig.get().getCollisionBins();
        source.sendSuccess(() -> Component.translatable(
                "commands.katamariio.config.collision_bins.query", val), false);
        return 1;
    }

    private static int sendConfigStatus(CommandSourceStack source) {
        KatamariConfig cfg = KatamariConfig.get();
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.header"), false);
        source.sendSuccess(() -> Component.translatable(cfg.attachBlocks ?
                "commands.katamariio.config.status.attach_blocks.enabled" : "commands.katamariio.config.status.attach_blocks.disabled"), false);
        source.sendSuccess(() -> Component.translatable(cfg.attachEntities ?
                "commands.katamariio.config.status.attach_entities.enabled" : "commands.katamariio.config.status.attach_entities.disabled"), false);
        source.sendSuccess(() -> Component.translatable(cfg.attachBalls ?
                "commands.katamariio.config.status.attach_balls.enabled" : "commands.katamariio.config.status.attach_balls.disabled"), false);
        source.sendSuccess(() -> Component.translatable(cfg.attachWhileRiding ?
                "commands.katamariio.config.status.attach_while_riding.enabled" : "commands.katamariio.config.status.attach_while_riding.disabled"), false);
        source.sendSuccess(() -> Component.translatable(cfg.allowSelfDismount ?
                "commands.katamariio.config.status.allow_self_dismount.enabled" : "commands.katamariio.config.status.allow_self_dismount.disabled"), false);
        source.sendSuccess(() -> Component.translatable(cfg.renderAttachedBlocks ?
                "commands.katamariio.config.status.render_attached_blocks.enabled" : "commands.katamariio.config.status.render_attached_blocks.disabled"), false);

        Component maxSizeComp = cfg.maxSize <= 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(cfg.maxSize));
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.max_size", maxSizeComp), false);

        Component removalsComp = cfg.maxRemovalsPerTick < 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(cfg.maxRemovalsPerTick));
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.max_removals_per_tick", removalsComp), false);

        Component maxEntitiesComp = cfg.maxAttachedEntities < 0 ? Component.translatable("commands.katamariio.common.unlimited") : Component.literal(String.valueOf(cfg.maxAttachedEntities));
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.max_attached_entities", maxEntitiesComp), false);

        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.prune_inner_blocks", cfg.pruneInnerBlocks ? "§atrue" : "§cfalse"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.show_player_indicator", cfg.showPlayerIndicator ? "§atrue" : "§cfalse"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.disable_fly_check", cfg.disableFlyCheck ? "§atrue" : "§cfalse"), false);

        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.collision_grid",
                cfg.getCollisionBins()), false);

        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.block_filter_mode",
                cfg.blockFilterMode.name(), cfg.blockFilterList.size(), cfg.protectIndestructibleBlocks ? "§atrue" : "§cfalse"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.config.status.entity_filter_mode",
                cfg.entityFilterMode.name(), cfg.entityFilterList.size(), cfg.protectBosses ? "§atrue" : "§cfalse"), false);
        return 1;
    }

    private static int sendHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.header"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.player_section"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.size_set"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.size_query"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.ball"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.restitution_set"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.restitution_query"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.reset_blocks"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.reset_entities"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.reset_all"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.server_section"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.config_get"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.config_reload"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.config_option"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.config_mode"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.config_preset"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.config_filter"), false);
        source.sendSuccess(() -> Component.translatable("commands.katamariio.help.footer"), false);
        return 1;
    }
}
