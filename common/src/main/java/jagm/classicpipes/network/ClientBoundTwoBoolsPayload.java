package jagm.classicpipes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ClientBoundTwoBoolsPayload(boolean first, boolean second) {

    public static final SelfHandler<ClientBoundTwoBoolsPayload> HANDLER = new Handler();

    private static class Handler extends SelfHandler<ClientBoundTwoBoolsPayload> {

        @Override
        public FriendlyByteBuf encode(ClientBoundTwoBoolsPayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.first());
            buffer.writeBoolean(payload.second());
            return buffer;
        }

        @Override
        public ClientBoundTwoBoolsPayload decode(FriendlyByteBuf buffer) {
            return new ClientBoundTwoBoolsPayload(buffer.readBoolean(), buffer.readBoolean());
        }

        @Override
        public void handle(ClientBoundTwoBoolsPayload payload, Player player) {}

    }

}
