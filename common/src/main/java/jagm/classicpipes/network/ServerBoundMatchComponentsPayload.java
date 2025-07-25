package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record ServerBoundMatchComponentsPayload(boolean matchComponents) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerBoundMatchComponentsPayload> TYPE = new CustomPacketPayload.Type<>(MiscUtil.resourceLocation("match_components"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundMatchComponentsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerBoundMatchComponentsPayload::matchComponents,
            ServerBoundMatchComponentsPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(Player player) {
        if (player != null && player.containerMenu instanceof FilterMenu menu) {
            menu.getFilter().setMatchComponents(this.matchComponents());
        }
    }

}
