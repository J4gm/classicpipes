package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.ProviderPipeMenu;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record ServerBoundLeaveOnePayload(boolean leaveOne) implements PayloadWrapper<ServerBoundLeaveOnePayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("leave_one");
    public static final SelfHandler<ServerBoundLeaveOnePayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundLeaveOnePayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundLeaveOnePayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundLeaveOnePayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.leaveOne());
            return buffer;
        }

        @Override
        public ServerBoundLeaveOnePayload decode(FriendlyByteBuf buffer) {
            return new ServerBoundLeaveOnePayload(buffer.readBoolean());
        }

        @Override
        public void handle(ServerBoundLeaveOnePayload payload, Player player) {
            if (player != null) {
                if (player.containerMenu instanceof ProviderPipeMenu menu) {
                    menu.setLeaveOne(payload.leaveOne());
                } else if (player.containerMenu instanceof StoragePipeMenu menu) {
                    menu.setLeaveOne(payload.leaveOne());
                }
            }
        }

    }

}
