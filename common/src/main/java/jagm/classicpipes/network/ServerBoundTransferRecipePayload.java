package jagm.classicpipes.network;

import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ServerBoundTransferRecipePayload(List<ItemStack> recipe, List<Integer> slots) implements PayloadWrapper<ServerBoundTransferRecipePayload> {

    public static final ResourceLocation TYPE = MiscUtil.resourceLocation("transfer_recipe");
    public static final SelfHandler<ServerBoundTransferRecipePayload> HANDLER = new Handler();

    @Override
    public SelfHandler<ServerBoundTransferRecipePayload> getHandler() {
        return HANDLER;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private static class Handler extends SelfHandler<ServerBoundTransferRecipePayload> {

        @Override
        public FriendlyByteBuf encode(ServerBoundTransferRecipePayload payload, FriendlyByteBuf buffer) {
            buffer.writeCollection(payload.recipe(), MiscUtil::writeStackToBuffer);
            buffer.writeCollection(payload.slots(), FriendlyByteBuf::writeInt);
            return buffer;
        }

        @Override
        public ServerBoundTransferRecipePayload decode(FriendlyByteBuf buffer) {
            List<ItemStack> recipe = buffer.readCollection(ArrayList::new, MiscUtil::readStackFromBuffer);
            List<Integer> slots = buffer.readCollection(ArrayList::new, FriendlyByteBuf::readInt);
            return new ServerBoundTransferRecipePayload(recipe, slots);
        }

        @Override
        public void handle(ServerBoundTransferRecipePayload payload, Player player) {
            if (player != null && player.containerMenu instanceof RecipePipeMenu menu) {
                for (int i = 0; i < 10; i++) {
                    menu.getSlot(i).set(ItemStack.EMPTY);
                }
                for (int i = 0; i < payload.recipe().size(); i++) {
                    menu.getSlot(payload.slots().get(i)).set(payload.recipe().get(i));
                    menu.getSlot(payload.slots().get(i)).setChanged();
                }
            }
        }

    }

}
