package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.SortingMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ClientBoundItemListPayload(List<ItemStack> existingItems, List<ItemStack> craftableItems, SortingMode sortingMode, BlockPos networkPos, BlockPos requestPos) implements SelfHandler {

    public static final Type<ClientBoundItemListPayload> TYPE = new Type<>(MiscUtil.identifier("item_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundItemListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC),
            ClientBoundItemListPayload::existingItems,
            ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC),
            ClientBoundItemListPayload::craftableItems,
            ByteBufCodecs.BYTE,
            payload -> payload.sortingMode().getValue(),
            BlockPos.STREAM_CODEC,
            ClientBoundItemListPayload::networkPos,
            BlockPos.STREAM_CODEC,
            ClientBoundItemListPayload::requestPos,
            (existingItems, craftableItems, value, networkPos, requestPos) -> new ClientBoundItemListPayload(existingItems, craftableItems, SortingMode.fromByte(value), networkPos, requestPos)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null && player.containerMenu instanceof RequestMenu menu && menu.getNetworkPos().equals(this.networkPos())) {
            menu.update(this.existingItems(), this.craftableItems());
        }
    }

}
