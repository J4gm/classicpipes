package jagm.classicpipes.network;

import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.SortingMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public record ServerBoundSortingModePayload(SortingMode sortingMode) implements PayloadWrapper<ServerBoundSortingModePayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("sorting_mode");
    public static final SelfHandler<ServerBoundSortingModePayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundSortingModePayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundSortingModePayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundSortingModePayload payload, FriendlyByteBuf buffer) {
            buffer.writeByte(payload.sortingMode().getValue());
            return buffer;
        }

        @Override
        public ServerBoundSortingModePayload decode(FriendlyByteBuf buffer) {
            return new ServerBoundSortingModePayload(SortingMode.fromByte(buffer.readByte()));
        }

        @Override
        public void handle(ServerBoundSortingModePayload payload, Player player) {
            if (player != null && player.containerMenu instanceof RequestMenu menu) {
                if (player.level() instanceof ServerLevel serverLevel && serverLevel.getBlockEntity(menu.getNetworkPos()) instanceof NetworkedPipeEntity pipe && pipe.hasNetwork()) {
                    pipe.getNetwork().setSortingMode(payload.sortingMode());
                    pipe.setChanged();
                }
            }
        }

    }

}
