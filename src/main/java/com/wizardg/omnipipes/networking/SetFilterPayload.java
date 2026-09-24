package com.wizardg.omnipipes.networking;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.screen.PipeMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Client -> server: an item dragged from JEI/REI onto a filter slot. Menu buttons can only carry an int, so this needs its own packet.
public record SetFilterPayload(int containerId, int slot, ItemStack stack) implements CustomPacketPayload {
    public static final Type<SetFilterPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OmniPipes.MODID, "set_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetFilterPayload::containerId,
            ByteBufCodecs.VAR_INT, SetFilterPayload::slot,
            ItemStack.STREAM_CODEC, SetFilterPayload::stack,
            SetFilterPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, SetFilterPayload::handle);
    }

    private static void handle(SetFilterPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof PipeMenu menu && menu.containerId == payload.containerId())
            menu.setFilterSlot(payload.slot(), payload.stack());
    }

    @Override
    public Type<SetFilterPayload> type() {
        return TYPE;
    }
}
