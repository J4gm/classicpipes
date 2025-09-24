package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.SortingMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ClientBoundItemListPayload(List<ItemStack> existingItems, List<ItemStack> craftableItems, BlockPos networkPos, BlockPos requestPos, SortingMode sortingMode) implements PayloadWrapper<ClientBoundItemListPayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("item_list");
    public static final SelfHandler<ClientBoundItemListPayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ClientBoundItemListPayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ClientBoundItemListPayload> {

        @Override
        public FriendlyByteBuf encode(ClientBoundItemListPayload payload, FriendlyByteBuf buffer) {
            buffer.writeCollection(payload.existingItems(), MiscUtil::writeStackToBuffer);
            buffer.writeCollection(payload.craftableItems(), MiscUtil::writeStackToBuffer);
            buffer.writeBlockPos(payload.networkPos());
            buffer.writeBlockPos(payload.requestPos());
            buffer.writeByte(payload.sortingMode().getValue());
            return buffer;
        }

        @Override
        public ClientBoundItemListPayload decode(FriendlyByteBuf buffer) {
            List<ItemStack> existingItems = buffer.readCollection(ArrayList::new, MiscUtil::readStackFromBuffer);
            List<ItemStack> craftableItems = buffer.readCollection(ArrayList::new, MiscUtil::readStackFromBuffer);
            BlockPos networkPos = buffer.readBlockPos();
            BlockPos requestPos = buffer.readBlockPos();
            SortingMode sortingMode = SortingMode.fromByte(buffer.readByte());
            return new ClientBoundItemListPayload(existingItems, craftableItems, networkPos, requestPos, sortingMode);
        }

        @Override
        public void handle(ClientBoundItemListPayload payload, Player player) {
            if (player != null && player.containerMenu instanceof RequestMenu menu && menu.getNetworkPos().equals(payload.networkPos())) {
                menu.update(payload.existingItems(), payload.craftableItems());
            }
        }

    }

}
