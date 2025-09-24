package jagm.classicpipes.network;

import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record ServerBoundRequestPayload(ItemStack stack, BlockPos requestPos) implements PayloadWrapper<ServerBoundRequestPayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("request");
    public static final SelfHandler<ServerBoundRequestPayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundRequestPayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundRequestPayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundRequestPayload payload, FriendlyByteBuf buffer) {
            MiscUtil.writeStackToBuffer(buffer, payload.stack());
            buffer.writeBlockPos(payload.requestPos());
            return buffer;
        }

        @Override
        public ServerBoundRequestPayload decode(FriendlyByteBuf buffer) {
            ItemStack stack = MiscUtil.readStackFromBuffer(buffer);
            BlockPos requestPos = buffer.readBlockPos();
            return new ServerBoundRequestPayload(stack, requestPos);
        }

        @Override
        public void handle(ServerBoundRequestPayload payload, Player player) {
            if (player.level() instanceof ServerLevel serverLevel && player.level().getBlockEntity(payload.requestPos()) instanceof NetworkedPipeEntity pipe && pipe.hasNetwork()) {
                pipe.getNetwork().request(serverLevel, payload.stack(), payload.requestPos(), player, false);
            }
        }

    }

}
