package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RoutingPipeMenu;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundDefaultRoutePayload(boolean defaultRoute) implements SelfHandler {

    public static final Type<ServerBoundDefaultRoutePayload> TYPE = new Type<>(MiscUtil.identifier("default_route"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundDefaultRoutePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundDefaultRoutePayload::defaultRoute,
            ServerBoundDefaultRoutePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null) {
            if (player.containerMenu instanceof RoutingPipeMenu menu) {
                menu.setDefaultRoute(this.defaultRoute());
            } else if (player.containerMenu instanceof StoragePipeMenu menu) {
                menu.setDefaultRoute(this.defaultRoute());
            }
        }
    }

}
