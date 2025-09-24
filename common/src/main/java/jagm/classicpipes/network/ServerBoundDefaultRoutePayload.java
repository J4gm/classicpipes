package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RoutingPipeMenu;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record ServerBoundDefaultRoutePayload(boolean defaultRoute) implements PayloadWrapper<ServerBoundDefaultRoutePayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("default_route");
    public static final SelfHandler<ServerBoundDefaultRoutePayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundDefaultRoutePayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundDefaultRoutePayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundDefaultRoutePayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.defaultRoute());
            return buffer;
        }

        @Override
        public ServerBoundDefaultRoutePayload decode(FriendlyByteBuf buffer) {
            return new ServerBoundDefaultRoutePayload(buffer.readBoolean());
        }

        @Override
        public void handle(ServerBoundDefaultRoutePayload payload, Player player) {
            if (player != null) {
                if (player.containerMenu instanceof RoutingPipeMenu menu) {
                    menu.setDefaultRoute(payload.defaultRoute());
                } else if (player.containerMenu instanceof StoragePipeMenu menu) {
                    menu.setDefaultRoute(payload.defaultRoute());
                }
            }
        }

    }

}
