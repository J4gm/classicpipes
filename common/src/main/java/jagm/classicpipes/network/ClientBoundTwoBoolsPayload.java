package jagm.classicpipes.network;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.ItemStackWithSlot;

import java.util.ArrayList;
import java.util.List;

public record ClientBoundTwoBoolsPayload(List<ItemStackWithSlot> items, boolean first, boolean second) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundTwoBoolsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, MiscUtil.ITEM_STACK_WITH_SLOT_STREAM_CODEC),
            ClientBoundTwoBoolsPayload::items,
            ByteBufCodecs.BOOL,
            ClientBoundTwoBoolsPayload::first,
            ByteBufCodecs.BOOL,
            ClientBoundTwoBoolsPayload::second,
            ClientBoundTwoBoolsPayload::new
    );

}
