package jagm.classicpipes.network;

import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.SortingMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public record ServerBoundSortingModePayload(SortingMode sortingMode) implements SelfHandler {

    public static final CustomPacketPayload.Type<ServerBoundSortingModePayload> TYPE = new CustomPacketPayload.Type<>(MiscUtil.identifier("sorting_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundSortingModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE,
            payload -> payload.sortingMode().getValue(),
            value -> new ServerBoundSortingModePayload(SortingMode.fromByte(value))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null && player.containerMenu instanceof RequestMenu menu) {
            if (player.level() instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(menu.getNetworkPos()) instanceof NetworkedPipeEntity pipe && pipe.hasNetwork()) {
                pipe.getNetwork().setSortingMode(this.sortingMode());
                pipe.setChanged();
            }
        }
    }

}
