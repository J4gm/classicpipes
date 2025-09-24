package jagm.classicpipes.network;

import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.inventory.menu.FluidFilterMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public record ServerBoundSetFilterPayload(int slot, ItemStack stack) implements PayloadWrapper<ServerBoundSetFilterPayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("set_filter");
    public static final SelfHandler<ServerBoundSetFilterPayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundSetFilterPayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundSetFilterPayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundSetFilterPayload payload, FriendlyByteBuf buffer) {
            buffer.writeInt(payload.slot());
            MiscUtil.writeStackToBuffer(buffer, payload.stack());
            return buffer;
        }

        @Override
        public ServerBoundSetFilterPayload decode(FriendlyByteBuf buffer) {
            int slot = buffer.readInt();
            ItemStack stack = MiscUtil.readStackFromBuffer(buffer);
            return new ServerBoundSetFilterPayload(slot, stack);
        }

        @Override
        public void handle(ServerBoundSetFilterPayload payload, Player player) {
            if (player != null && player.containerMenu instanceof FilterMenu menu) {
                Slot slot = menu.getSlot(payload.slot());
                if (slot.container instanceof Filter && (!(menu instanceof FluidFilterMenu) || Services.LOADER_SERVICE.getFluidFromStack(payload.stack()) != null)) {
                    slot.set(payload.stack());
                    slot.setChanged();
                }
            }
        }

    }

}
