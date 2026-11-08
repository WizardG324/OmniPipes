package com.wizardg.omnipipes.networking;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.screen.PipeMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: an amount typed into a filter slot (middle click). Menu buttons can only carry an int, so this needs its own packet.
public record SetFilterAmountPayload(int containerId, int slot, int amount) implements CustomPacketPayload {
    public static final Type<SetFilterAmountPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "set_filter_amount"));
    public static final StreamCodec<ByteBuf, SetFilterAmountPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetFilterAmountPayload::containerId,
            ByteBufCodecs.VAR_INT, SetFilterAmountPayload::slot,
            ByteBufCodecs.VAR_INT, SetFilterAmountPayload::amount,
            SetFilterAmountPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, SetFilterAmountPayload::handle);
    }

    private static void handle(SetFilterAmountPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof PipeMenu menu && menu.containerId == payload.containerId())
            menu.setAmount(payload.slot(), payload.amount());
    }

    @Override
    public Type<SetFilterAmountPayload> type() {
        return TYPE;
    }
}
