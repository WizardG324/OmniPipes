package com.wizardg.omnipipes.compat.jade;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.util.ModFormat;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

// Shows the looked at pipe connection's mode, and for extracting sides what it moves plus its speed and amounts.
@WailaPlugin
public class ModJadePlugin implements IWailaPlugin {
    public static final ResourceLocation PIPE = ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "pipe");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PipeDataProvider.INSTANCE, PipeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PipeTooltipProvider.INSTANCE, PipeBlock.class);
    }

    // The server and client halves are separate providers that share the same uid.
    public enum PipeDataProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        // Everything that only lives in the block entity, for the connection the player looks at.
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof PipeBlockEntity be)) return;
            Direction dir = PipeBlock.clickedSide(accessor.getPosition(), accessor.getHitResult());
            PipeBlockEntity.Rates rates = be.rates(dir);
            data.putInt("side", dir.ordinal());
            data.putInt("speed", be.getSpeed(dir));
            data.putBoolean("creative", be.isCreative(dir));
            data.putInt("items", rates.items());
            data.putInt("fluid", be.movesType(dir, ModItems.FLUID_UPGRADE.get()) ? rates.fluid() : -1);
            data.putInt("energy", be.movesType(dir, ModItems.ENERGY_UPGRADE.get()) ? rates.energy() : -1);
            data.putInt("insert_channel", be.getInsertChannel(dir));
            data.putInt("extract_channel", be.getExtractChannel(dir));
        }

        @Override
        public ResourceLocation getUid() {
            return PIPE;
        }
    }

    public enum PipeTooltipProvider implements IBlockComponentProvider {
        INSTANCE;

        // Only for block connections, the plain pipe and pipe-to-pipe arms show nothing extra.
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Direction dir = PipeBlock.clickedSide(accessor.getPosition(), accessor.getHitResult());
            PipeBlock.Side mode = accessor.getBlockState().getValue(PipeBlock.SIDES.get(dir));
            if (!PipeBlock.isPort(mode)) return;
            // Mode line, "Insert" and "Extract" take their channel's color once the server data for this side is in.
            CompoundTag data = accessor.getServerData();
            boolean ready = (data.contains("side") ? data.getInt("side") : -1) == dir.ordinal();
            MutableComponent insert = channelColored("jade.omni_pipes.mode.insert", ready ? data.getInt("insert_channel") : -1);
            MutableComponent extract = channelColored("jade.omni_pipes.mode.extract", ready ? data.getInt("extract_channel") : -1);
            Component modeName = switch (mode) {
                case INSERT -> insert;
                case EXTRACT -> extract;
                default -> Component.translatable("jade.omni_pipes.mode.both", insert, extract);
            };
            tooltip.add(Component.translatable("jade.omni_pipes.mode", modeName));
            if (!mode.extracts() || !ready) return;

            int speed = data.getInt("speed");
            if (data.getBoolean("creative")) {
                tooltip.add(Component.translatable("jade.omni_pipes.transferring", Component.translatable("jade.omni_pipes.everything")));
                tooltip.add(Component.translatable("jade.omni_pipes.stats", speed, Component.translatable("jade.omni_pipes.unlimited")));
                return;
            }
            List<Component> types = new ArrayList<>(List.of(Component.translatable("jade.omni_pipes.type.items")));
            List<Component> amounts = new ArrayList<>(List.of(Component.literal(String.valueOf(data.getInt("items")))));
            int fluid = (data.contains("fluid") ? data.getInt("fluid") : -1);
            int energy = (data.contains("energy") ? data.getInt("energy") : -1);
            if (fluid >= 0) {
                types.add(Component.translatable("jade.omni_pipes.type.fluids"));
                amounts.add(Component.translatable("jade.omni_pipes.fluid", ModFormat.decimal(fluid / 1000.0)));
            }
            if (energy >= 0) {
                types.add(Component.translatable("jade.omni_pipes.type.energy"));
                amounts.add(Component.translatable("jade.omni_pipes.energy", ModFormat.compact(energy)));
            }
            tooltip.add(Component.translatable("jade.omni_pipes.transferring", join(types)));
            tooltip.add(Component.translatable("jade.omni_pipes.stats", speed, join(amounts)));
        }

        // Lightened toward white so dark dyes like black and blue stay readable on Jade's dark box. -1 = no color yet.
        private static MutableComponent channelColored(String key, int channel) {
            MutableComponent text = Component.translatable(key);
            if (channel < 0) return text;
            return text.withColor(FastColor.ARGB32.lerp(0.35f, FastColor.ARGB32.opaque(DyeColor.byId(channel).getTextColor()), 0xFFFFFFFF));
        }

        private static Component join(List<Component> parts) {
            MutableComponent joined = Component.empty();
            for (int i = 0; i < parts.size(); i++) joined.append(i == 0 ? Component.empty() : Component.literal(", ")).append(parts.get(i));
            return joined;
        }

        @Override
        public ResourceLocation getUid() {
            return PIPE;
        }
    }
}
