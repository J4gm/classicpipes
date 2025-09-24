package jagm.classicpipes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ClientBoundThreeBoolsPayload(boolean first, boolean second, boolean third) {

    public static final SelfHandler<ClientBoundThreeBoolsPayload> HANDLER = new Handler();

    private static class Handler extends SelfHandler<ClientBoundThreeBoolsPayload> {

        @Override
        public FriendlyByteBuf encode(ClientBoundThreeBoolsPayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.first());
            buffer.writeBoolean(payload.second());
            buffer.writeBoolean(payload.third());
            return buffer;
        }

        @Override
        public ClientBoundThreeBoolsPayload decode(FriendlyByteBuf buffer) {
            return new ClientBoundThreeBoolsPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
        }

        @Override
        public void handle(ClientBoundThreeBoolsPayload payload, Player player) {}

    }

}
