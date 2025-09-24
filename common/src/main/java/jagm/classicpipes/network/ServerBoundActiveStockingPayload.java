package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record ServerBoundActiveStockingPayload(boolean activeStocking) implements PayloadWrapper<ServerBoundActiveStockingPayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("active_stocking");

    public static final Handler HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundActiveStockingPayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    public static class Handler extends SelfHandler<ServerBoundActiveStockingPayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundActiveStockingPayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.activeStocking());
            return buffer;
        }

        @Override
        public ServerBoundActiveStockingPayload decode(FriendlyByteBuf buffer) {
            return new ServerBoundActiveStockingPayload(buffer.readBoolean());
        }

        @Override
        public void handle(ServerBoundActiveStockingPayload payload, Player player) {
            if (player != null && player.containerMenu instanceof StockingPipeMenu menu) {
                menu.setActiveStocking(payload.activeStocking());
            }
        }

    }

}
