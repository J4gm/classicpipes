package jagm.classicpipes.network;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.ItemStackWithSlot;

import java.util.ArrayList;
import java.util.List;

public record ClientBoundBoolPayload(List<ItemStackWithSlot> items, boolean value) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundBoolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, MiscUtil.ITEM_STACK_WITH_SLOT_STREAM_CODEC),
            ClientBoundBoolPayload::items,
            ByteBufCodecs.BOOL,
            ClientBoundBoolPayload::value,
            ClientBoundBoolPayload::new
    );

}
