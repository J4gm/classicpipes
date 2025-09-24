package jagm.classicpipes.network;

import jagm.classicpipes.blockentity.RecipePipeEntity;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record ServerBoundSlotDirectionPayload(BlockPos pos, int slot, Direction direction) implements PayloadWrapper<ServerBoundSlotDirectionPayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("slot_direction");
    public static final SelfHandler<ServerBoundSlotDirectionPayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundSlotDirectionPayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundSlotDirectionPayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundSlotDirectionPayload payload, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeInt(payload.slot());
            buffer.writeByte(payload.direction().get3DDataValue());
            return buffer;
        }

        @Override
        public ServerBoundSlotDirectionPayload decode(FriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            int slot = buffer.readInt();
            Direction direction = Direction.from3DDataValue(buffer.readByte());
            return new ServerBoundSlotDirectionPayload(pos, slot, direction);
        }

        @Override
        public void handle(ServerBoundSlotDirectionPayload payload, Player player) {
            if (player.level().getBlockEntity(payload.pos()) instanceof RecipePipeEntity craftingPipe) {
                craftingPipe.setSlotDirection(payload.slot(), payload.direction());
            }
        }

    }

}
