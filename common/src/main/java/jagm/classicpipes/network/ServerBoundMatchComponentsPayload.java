package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.inventory.menu.MatchingPipeMenu;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record ServerBoundMatchComponentsPayload(boolean matchComponents) implements PayloadWrapper<ServerBoundMatchComponentsPayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("match_components");
    public static final SelfHandler<ServerBoundMatchComponentsPayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundMatchComponentsPayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundMatchComponentsPayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundMatchComponentsPayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.matchComponents());
            return buffer;
        }

        @Override
        public ServerBoundMatchComponentsPayload decode(FriendlyByteBuf buffer) {
            return new ServerBoundMatchComponentsPayload(buffer.readBoolean());
        }

        @Override
        public void handle(ServerBoundMatchComponentsPayload payload, Player player) {
            if (player != null) {
                if (player.containerMenu instanceof FilterMenu menu) {
                    menu.getFilter().setMatchComponents(payload.matchComponents());
                } else if (player.containerMenu instanceof MatchingPipeMenu menu) {
                    menu.setMatchComponents(payload.matchComponents());
                } else if (player.containerMenu instanceof StoragePipeMenu menu) {
                    menu.setMatchComponents(payload.matchComponents());
                }
            }
        }

    }

}
