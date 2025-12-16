package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundBlockingModePayload(boolean blockingMode) implements SelfHandler {

    public static final Type<ServerBoundBlockingModePayload> TYPE = new Type<>(MiscUtil.identifier("blocking_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundBlockingModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundBlockingModePayload::blockingMode,
            ServerBoundBlockingModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null) {
            if (player.containerMenu instanceof RecipePipeMenu menu) {
                menu.setBlockingMode(this.blockingMode());
            }
        }
    }

}
