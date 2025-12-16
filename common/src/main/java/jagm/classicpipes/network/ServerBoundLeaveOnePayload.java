package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.ProviderPipeMenu;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundLeaveOnePayload(boolean leaveOne) implements SelfHandler {

    public static final Type<ServerBoundLeaveOnePayload> TYPE = new Type<>(MiscUtil.identifier("leave_one"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundLeaveOnePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundLeaveOnePayload::leaveOne,
            ServerBoundLeaveOnePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null) {
            if (player.containerMenu instanceof ProviderPipeMenu menu) {
                menu.setLeaveOne(this.leaveOne());
            } else if (player.containerMenu instanceof StoragePipeMenu menu) {
                menu.setLeaveOne(this.leaveOne());
            }
        }
    }

}
