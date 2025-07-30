package jagm.classicpipes.network;

import jagm.classicpipes.blockentity.LogisticalPipeEntity;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record ServerBoundRequestPayload(ItemStack stack, BlockPos requestPos) implements SelfHandler {

    public static final CustomPacketPayload.Type<ServerBoundRequestPayload> TYPE = new CustomPacketPayload.Type<>(MiscUtil.resourceLocation("request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ServerBoundRequestPayload::stack,
            BlockPos.STREAM_CODEC,
            ServerBoundRequestPayload::requestPos,
            ServerBoundRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player.level() instanceof ServerLevel serverLevel && player.level().getBlockEntity(this.requestPos()) instanceof LogisticalPipeEntity pipe && pipe.hasLogisticalNetwork()) {
            pipe.getLogisticalNetwork().request(serverLevel, this.stack(), this.requestPos(), player);
        }
    }

}
