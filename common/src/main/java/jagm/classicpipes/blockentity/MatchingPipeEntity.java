package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.MatchingPipeBlock;
import jagm.classicpipes.inventory.menu.MatchingPipeMenu;
import jagm.classicpipes.item.LabelItem;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class MatchingPipeEntity extends NetworkedPipeEntity implements MenuProvider, MatchingPipe {

    private boolean matchComponents;
    private final List<ItemStack> cache;
    private boolean cacheInitialised;
    private final List<ItemStack> cannotFit;

    public MatchingPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.MATCHING_PIPE_ENTITY, pos, state);
        this.matchComponents = false;
        this.cache = new ArrayList<>();
        this.cacheInitialised = false;
        this.cannotFit = new ArrayList<>();
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.cacheInitialised && !state.getValue(MatchingPipeBlock.FACING).equals(FacingOrNone.NONE)) {
            this.updateCache(level, pos, state.getValue(MatchingPipeBlock.FACING).getDirection());
            this.cacheInitialised = true;
        }
        super.tickServer(level, pos, state);
    }

    private void updateCache(ServerLevel level, BlockPos pos, Direction facing) {
        this.cache.clear();
        this.cannotFit.clear();
        this.cache.addAll(Services.LOADER_SERVICE.getContainerItems(level, pos.relative(facing), facing.getOpposite()));
    }

    @Override
    public void updateCache() {
        this.cacheInitialised = false;
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (this.itemCanFit(stack)) {
            for (ItemStack containerStack : this.cache) {
                if (containerStack.getItem() instanceof LabelItem labelItem && labelItem.itemMatches(containerStack, stack) || stack.is(containerStack.getItem()) && (!this.shouldMatchComponents() || ItemStack.isSameItemSameTags(stack, containerStack))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean itemCanFit(ItemStack stack) {
        for (ItemStack cannotFitStack : this.cannotFit) {
            if (ItemStack.isSameItemSameTags(cannotFitStack, stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public NetworkedPipeEntity getAsPipe() {
        return this;
    }

    @Override
    public void markCannotFit(ItemStack stack) {
        this.cannotFit.add(stack);
    }

    @Override
    public void load(CompoundTag valueInput) {
        this.cacheInitialised = false;
        super.load(valueInput);
        this.matchComponents = valueInput.getBoolean("match_components");
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putBoolean("match_components", this.shouldMatchComponents());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".matching_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MatchingPipeMenu(id, playerInventory, this.shouldMatchComponents(), this);
    }

    public boolean shouldMatchComponents() {
        return this.matchComponents;
    }

    public void setMatchComponents(boolean matchComponents) {
        this.matchComponents = matchComponents;
    }

}
