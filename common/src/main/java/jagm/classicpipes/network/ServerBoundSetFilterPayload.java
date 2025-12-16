package jagm.classicpipes.network;

import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.inventory.menu.FluidFilterMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public record ServerBoundSetFilterPayload(int slot, ItemStack stack) implements SelfHandler {

    public static final Type<ServerBoundSetFilterPayload> TYPE = new Type<>(MiscUtil.identifier("set_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundSetFilterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ServerBoundSetFilterPayload::slot,
            ItemStack.STREAM_CODEC,
            ServerBoundSetFilterPayload::stack,
            ServerBoundSetFilterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null && player.containerMenu instanceof FilterMenu menu) {
            Slot slot = menu.getSlot(this.slot());
            if (slot.container instanceof Filter && (!(menu instanceof FluidFilterMenu) || Services.LOADER_SERVICE.getFluidFromStack(this.stack()) != null)) {
                slot.set(this.stack());
                slot.setChanged();
            }
        }
    }

}
