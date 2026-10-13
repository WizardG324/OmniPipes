package com.wizardg.omnipipes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// /omnipipes stats | networks | network <pos>: pipe diagnostics for server admins and moderators (permission level 2).
@EventBusSubscriber(modid = OmniPipes.MODID)
public final class PipesCommand {
    private static final int WINDOW = 100; // ticks the activity numbers cover

    public static final Set<PipeBlockEntity> LOADED = Collections.newSetFromMap(new IdentityHashMap<>());
    public static long nanos, tries, moves, rebuilds;
    private static long lastNanos, lastTries, lastMoves, lastRebuilds;
    private static int ticks;
    private static boolean measured;

    private PipesCommand() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticks < WINDOW) return;
        ticks = 0;
        lastNanos = nanos;
        lastTries = tries;
        lastMoves = moves;
        lastRebuilds = rebuilds;
        nanos = tries = moves = rebuilds = 0;
        measured = true;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LOADED.clear();
        measured = false;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("omnipipes")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stats").executes(PipesCommand::stats))
                .then(Commands.literal("networks").executes(PipesCommand::networks))
                .then(Commands.literal("network").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(PipesCommand::network))));
    }

    private static int stats(CommandContext<CommandSourceStack> context) {
        List<PipeBlockEntity.NetworkInfo> networks = networkInfos();
        int extracting = networks.stream().mapToInt(PipeBlockEntity.NetworkInfo::extractSides).sum();
        int sleeping = networks.stream().mapToInt(PipeBlockEntity.NetworkInfo::sleeping).sum();
        double seconds = WINDOW / 20.0;
        send(context, Component.translatable("command.omni_pipes.stats", count(LOADED.size()), count(networks.size()), count(extracting), count(sleeping)));
        send(context, (!measured ? Component.translatable("command.omni_pipes.stats.collecting")
                : Component.translatable("command.omni_pipes.stats.activity", String.format(Locale.ROOT, "%.0f", seconds),
                String.format(Locale.ROOT, "%.2f", lastNanos / 1e6 / WINDOW), count(Math.round(lastTries / seconds)),
                count(Math.round(lastMoves / seconds)), String.format(Locale.ROOT, "%.1f", lastRebuilds / seconds))).withStyle(ChatFormatting.GRAY));
        return LOADED.size();
    }

    private static int networks(CommandContext<CommandSourceStack> context) {
        List<PipeBlockEntity.NetworkInfo> networks = networkInfos();
        networks.sort(Comparator.comparingInt(PipeBlockEntity.NetworkInfo::pipes).reversed());
        if (networks.isEmpty()) send(context, Component.translatable("command.omni_pipes.networks.none"));
        for (PipeBlockEntity.NetworkInfo info : networks.subList(0, Math.min(5, networks.size()))) send(context, describe(info));
        return networks.size();
    }

    private static int network(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!(context.getSource().getLevel().getBlockEntity(pos) instanceof PipeBlockEntity pipe)) {
            context.getSource().sendFailure(Component.translatable("command.omni_pipes.network.not_pipe"));
            return 0;
        }
        send(context, describe(pipe.networkInfo()));
        return 1;
    }

    private static List<PipeBlockEntity.NetworkInfo> networkInfos() {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<PipeBlockEntity.NetworkInfo> out = new java.util.ArrayList<>();
        for (PipeBlockEntity pipe : List.copyOf(LOADED))
            if (!pipe.isRemoved() && seen.add(pipe.networkId())) out.add(pipe.networkInfo());
        return out;
    }

    private static MutableComponent describe(PipeBlockEntity.NetworkInfo info) {
        BlockPos at = info.at();
        String tp = "/execute in " + info.dimension() + " run tp @s " + at.getX() + " " + (at.getY() + 1) + " " + at.getZ();
        MutableComponent where = Component.literal("[" + at.getX() + ", " + at.getY() + ", " + at.getZ() + "]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.SuggestCommand(tp))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("command.omni_pipes.network.teleport", info.dimension()))));
        return Component.translatable("command.omni_pipes.network", count(info.pipes()), count(info.targets()), count(info.extractSides()),
                count(info.sleeping()), count(info.blocked()), where);
    }

    private static String count(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    private static void send(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSuccess(() -> message, false);
    }
}
