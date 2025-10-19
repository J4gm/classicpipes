package jagm.classicpipes.network;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.ItemStackWithSlot;

import java.util.ArrayList;
import java.util.List;

public record ClientBoundRecipePipePayload(List<ItemStackWithSlot> items, Direction[] slotDirections, List<Direction> availableDirections, BlockPos pos, boolean blockingMode) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundRecipePipePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, MiscUtil.ITEM_STACK_WITH_SLOT_STREAM_CODEC),
            ClientBoundRecipePipePayload::items,
            ByteBufCodecs.byteArray(10),
            ClientBoundRecipePipePayload::getDirectionBytes,
            ByteBufCodecs.collection(ArrayList::new, Direction.STREAM_CODEC),
            ClientBoundRecipePipePayload::availableDirections,
            BlockPos.STREAM_CODEC,
            ClientBoundRecipePipePayload::pos,
            ByteBufCodecs.BOOL,
            ClientBoundRecipePipePayload::blockingMode,
            ClientBoundRecipePipePayload::makePayload
    );

    private byte[] getDirectionBytes() {
        byte[] directionBytes = new byte[this.slotDirections().length];
        for (int i = 0; i < this.slotDirections().length; i++) {
            directionBytes[i] = (byte) this.slotDirections()[i].get3DDataValue();
        }
        return directionBytes;
    }

    private static ClientBoundRecipePipePayload makePayload(List<ItemStackWithSlot> items, byte[] directionBytes, List<Direction> availableDirections, BlockPos pos, boolean blockingMode) {
        Direction[] directions = new Direction[directionBytes.length];
        for (int i = 0; i < directionBytes.length; i++) {
            directions[i] = Direction.from3DDataValue(directionBytes[i]);
        }
        return new ClientBoundRecipePipePayload(items, directions, availableDirections, pos, blockingMode);
    }

}
