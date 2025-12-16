package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ServerBoundTransferRecipePayload(List<ItemStack> recipe, List<Integer> slots) implements SelfHandler {

    public static final Type<ServerBoundTransferRecipePayload> TYPE = new Type<>(MiscUtil.identifier("transfer_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundTransferRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC),
            ServerBoundTransferRecipePayload::recipe,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT),
            ServerBoundTransferRecipePayload::slots,
            ServerBoundTransferRecipePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(Player player) {
        if (player != null && player.containerMenu instanceof RecipePipeMenu menu) {
            for (int i = 0; i < 10; i++) {
                menu.getSlot(i).set(ItemStack.EMPTY);
            }
            for (int i = 0; i < this.recipe().size(); i++) {
                menu.getSlot(this.slots().get(i)).set(this.recipe().get(i));
                menu.getSlot(this.slots().get(i)).setChanged();
            }
        }
    }

}
