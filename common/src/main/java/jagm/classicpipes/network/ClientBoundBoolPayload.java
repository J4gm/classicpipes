package jagm.classicpipes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record ClientBoundBoolPayload(boolean value) {

    public static final SelfHandler<ClientBoundBoolPayload> HANDLER = new Handler();

    private static class Handler extends SelfHandler<ClientBoundBoolPayload> {

        @Override
        public FriendlyByteBuf encode(ClientBoundBoolPayload payload, FriendlyByteBuf buffer) {
            buffer.writeBoolean(payload.value());
            return buffer;
        }

        @Override
        public ClientBoundBoolPayload decode(FriendlyByteBuf buffer) {
            return new ClientBoundBoolPayload(buffer.readBoolean());
        }

        @Override
        public void handle(ClientBoundBoolPayload payload, Player player) {}

    }

}
