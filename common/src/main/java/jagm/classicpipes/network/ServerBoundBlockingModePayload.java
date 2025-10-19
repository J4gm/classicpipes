package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record ServerBoundBlockingModePayload(boolean blockingMode) implements PayloadWrapper<ServerBoundBlockingModePayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("blocking_mode");
    public static final SelfHandler<ServerBoundBlockingModePayload> HANDLER = new ServerBoundBlockingModePayload.Handler();

    @Override
    public SelfHandler<ServerBoundBlockingModePayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundBlockingModePayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundBlockingModePayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.blockingMode());
            return buffer;
        }

        @Override
        public ServerBoundBlockingModePayload decode(FriendlyByteBuf buffer) {
            return new ServerBoundBlockingModePayload(buffer.readBoolean());
        }

        @Override
        public void handle(ServerBoundBlockingModePayload payload, Player player) {
            if (player != null) {
                if (player.containerMenu instanceof RecipePipeMenu menu) {
                    menu.setBlockingMode(payload.blockingMode());
                }
            }
        }

    }

}
