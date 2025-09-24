package jagm.classicpipes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrafterBlock.class)
public class CrafterInsertionMixin {

    @Inject(at = @At("HEAD"), method = "dispenseItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/CrafterBlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/crafting/RecipeHolder;)V")
    public void injectInserter(ServerLevel level, BlockPos pos, CrafterBlockEntity crafter, ItemStack stack, BlockState state, RecipeHolder<CraftingRecipe> recipeHolder, CallbackInfo info) {
        Direction facing = state.getValue(BlockStateProperties.ORIENTATION).front();
        BlockPos nextPos = pos.relative(facing);
        BlockEntity blockEntity = level.getBlockEntity(nextPos);
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).ifPresent(itemHandler -> {
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    if (itemHandler.isItemValid(slot, stack)) {
                        ItemStack remainder = itemHandler.insertItem(slot, stack.copy(), false);
                        if (remainder.getCount() < stack.getCount()) {
                            stack.shrink(stack.getCount() - remainder.getCount());
                            break;
                        }
                    }
                }
            });
        }
    }

}
