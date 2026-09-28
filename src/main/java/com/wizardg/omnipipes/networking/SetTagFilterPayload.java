package com.wizardg.omnipipes.networking;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.item.ModDataComponents;
import com.wizardg.omnipipes.item.TagFilterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

// Client -> server: the edited tag list of the Tag Filter in the player's hand.
public record SetTagFilterPayload(boolean offhand, List<ResourceLocation> tags) implements CustomPacketPayload {
    public static final Type<SetTagFilterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "set_tag_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetTagFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetTagFilterPayload::offhand,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(TagFilterItem.MAX_TAGS)), SetTagFilterPayload::tags,
            SetTagFilterPayload::new);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, SetTagFilterPayload::handle);
    }

    private static void handle(SetTagFilterPayload payload, IPayloadContext context) {
        ItemStack stack = context.player().getItemInHand(payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof TagFilterItem)) return;
        List<ResourceLocation> tags = payload.tags().stream().distinct().limit(TagFilterItem.MAX_TAGS).toList();
        if (tags.isEmpty()) stack.remove(ModDataComponents.TAGS);
        else stack.set(ModDataComponents.TAGS, tags);
    }

    @Override
    public Type<SetTagFilterPayload> type() {
        return TYPE;
    }
}
