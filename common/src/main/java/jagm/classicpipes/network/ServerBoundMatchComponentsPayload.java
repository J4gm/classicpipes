package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.inventory.menu.MatchingPipeMenu;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundMatchComponentsPayload(boolean matchComponents) implements SelfHandler {

    public static final CustomPacketPayload.Type<ServerBoundMatchComponentsPayload> TYPE = new CustomPacketPayload.Type<>(MiscUtil.identifier("match_components"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundMatchComponentsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundMatchComponentsPayload::matchComponents,
            ServerBoundMatchComponentsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null) {
            switch (player.containerMenu) {
                case FilterMenu menu -> menu.getFilter().setMatchComponents(this.matchComponents());
                case MatchingPipeMenu menu -> menu.setMatchComponents(this.matchComponents());
                case StoragePipeMenu menu -> menu.setMatchComponents(this.matchComponents());
                default -> {}
            }
        }
    }

}
