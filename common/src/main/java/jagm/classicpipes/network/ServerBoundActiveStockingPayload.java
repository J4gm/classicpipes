package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundActiveStockingPayload(boolean activeStocking) implements SelfHandler {

    public static final Type<ServerBoundActiveStockingPayload> TYPE = new Type<>(MiscUtil.identifier("active_stocking"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundActiveStockingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundActiveStockingPayload::activeStocking,
            ServerBoundActiveStockingPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null && player.containerMenu instanceof StockingPipeMenu menu) {
            menu.setActiveStocking(this.activeStocking());
        }
    }

}
