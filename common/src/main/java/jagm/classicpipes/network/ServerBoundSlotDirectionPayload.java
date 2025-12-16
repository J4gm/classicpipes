package jagm.classicpipes.network;

import jagm.classicpipes.blockentity.RecipePipeEntity;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundSlotDirectionPayload(BlockPos pos, int slot, Direction direction) implements SelfHandler {

    public static final CustomPacketPayload.Type<ServerBoundSlotDirectionPayload> TYPE = new CustomPacketPayload.Type<>(MiscUtil.identifier("slot_direction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundSlotDirectionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ServerBoundSlotDirectionPayload::pos,
            ByteBufCodecs.INT,
            ServerBoundSlotDirectionPayload::slot,
            ByteBufCodecs.BYTE,
            payload -> (byte) payload.direction().get3DDataValue(),
            (pos, slot, directionByte) -> new ServerBoundSlotDirectionPayload(pos, slot, Direction.from3DDataValue(directionByte))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player.level().getBlockEntity(this.pos()) instanceof RecipePipeEntity craftingPipe) {
            craftingPipe.setSlotDirection(this.slot(), this.direction());
        }
    }

}
