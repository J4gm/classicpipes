package jagm.classicpipes.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public record ClientBoundRecipePipePayload(Direction[] slotDirections, List<Direction> availableDirections, BlockPos pos) {

    public static final SelfHandler<ClientBoundRecipePipePayload> HANDLER = new Handler();

    private byte[] getDirectionBytes() {
        byte[] directionBytes = new byte[this.slotDirections().length];
        for (int i = 0; i < this.slotDirections().length; i++) {
            directionBytes[i] = (byte) this.slotDirections()[i].get3DDataValue();
        }
        return directionBytes;
    }

    private static ClientBoundRecipePipePayload makePayload(byte[] directionBytes, List<Direction> availableDirections, BlockPos pos) {
        Direction[] directions = new Direction[directionBytes.length];
        for (int i = 0; i < directionBytes.length; i++) {
            directions[i] = Direction.from3DDataValue(directionBytes[i]);
        }
        return new ClientBoundRecipePipePayload(directions, availableDirections, pos);
    }

    private static class Handler extends SelfHandler<ClientBoundRecipePipePayload> {

        @Override
        public FriendlyByteBuf encode(ClientBoundRecipePipePayload payload, FriendlyByteBuf buffer) {
            buffer.writeByteArray(payload.getDirectionBytes());
            buffer.writeCollection(payload.availableDirections(), (buf, direction) -> buf.writeByte(direction.get3DDataValue()));
            buffer.writeBlockPos(payload.pos());
            return buffer;
        }

        @Override
        public ClientBoundRecipePipePayload decode(FriendlyByteBuf buffer) {
            byte[] directionBytes = buffer.readByteArray();
            List<Direction> availableDirections = buffer.readCollection(ArrayList::new, buf -> Direction.from3DDataValue(buf.readByte()));
            BlockPos pos = buffer.readBlockPos();
            return makePayload(directionBytes, availableDirections, pos);
        }

        @Override
        public void handle(ClientBoundRecipePipePayload payload, Player player) {}

    }

}
