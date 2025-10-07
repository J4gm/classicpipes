package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.ProviderPipeBlock;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.ProviderPipeMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import jagm.classicpipes.util.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProviderPipeEntity extends NetworkedPipeEntity implements MenuProvider, ProviderPipe {

    private final SingleItemFilterContainer filter;
    private boolean leaveOne;
    private final List<ItemStack> cache;
    private boolean cacheInitialised = false;

    public ProviderPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.PROVIDER_PIPE_ENTITY, pos, state);
        this.filter = new SingleItemFilterContainer(this, 9, false);
        this.leaveOne = false;
        this.cache = new ArrayList<>();
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.cacheInitialised && !state.getValue(ProviderPipeBlock.FACING).equals(FacingOrNone.NONE)) {
            this.updateCache(level, pos, state.getValue(ProviderPipeBlock.FACING).getDirection());
            this.cacheInitialised = true;
        }
        super.tickServer(level, pos, state);
    }

    @Override
    public void disconnect(ServerLevel level) {
        PipeNetwork network = this.getNetwork();
        super.disconnect(level);
        if (network != null) {
            network.cacheUpdated();
        }
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        this.filter.clearContent();
        this.cacheInitialised = false;
        super.loadAdditional(valueInput);
        ValueInput.TypedInputList<ItemStackWithSlot> filterList = valueInput.listOrEmpty("filter", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : filterList) {
            this.filter.setItem(slotStack.slot(), slotStack.stack());
        }
        this.filter.setMatchComponents(valueInput.getBooleanOr("match_components", false));
        this.leaveOne = valueInput.getBooleanOr("leave_one", false);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ValueOutput.TypedOutputList<ItemStackWithSlot> filterList = valueOutput.list("filter", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                filterList.add(new ItemStackWithSlot(slot, stack));
            }
        }
        valueOutput.putBoolean("match_components", this.filter.shouldMatchComponents());
        valueOutput.putBoolean("leave_one", this.leaveOne);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".provider_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ProviderPipeMenu(id, playerInventory, this.filter, this.leaveOne);
    }

    public boolean shouldMatchComponents() {
        return this.filter.shouldMatchComponents();
    }

    public void setLeaveOne(boolean leaveOne) {
        this.leaveOne = leaveOne;
        Direction facing = this.getBlockState().getValue(ProviderPipeBlock.FACING).getDirection();
        if (this.getLevel() instanceof ServerLevel && facing != null) {
            this.updateCache();
        }
    }

    public boolean shouldLeaveOne() {
        return this.leaveOne;
    }

    private void updateCache(ServerLevel level, BlockPos pos, Direction facing) {
        this.cache.clear();
        List<ItemStack> stacks = Services.LOADER_SERVICE.getContainerItems(level, pos.relative(facing), facing.getOpposite());
        Iterator<ItemStack> iterator = stacks.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (!this.filter.isEmpty() && !this.filter.matches(stack).matches) {
                iterator.remove();
            } else if (this.shouldLeaveOne()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        this.cache.addAll(stacks);
        if (this.hasNetwork()) {
            this.getNetwork().cacheUpdated();
        }
    }

    @Override
    public void updateCache() {
        this.cacheInitialised = false;
    }

    @Override
    public List<ItemStack> getCache() {
        return this.cache;
    }

    @Override
    public boolean extractItem(ServerLevel level, ItemStack stack) {
        Direction facing = this.getBlockState().getValue(ProviderPipeBlock.FACING).getDirection();
        if (facing != null) {
            boolean extracted = Services.LOADER_SERVICE.extractSpecificItem(this, level, this.getBlockPos().relative(facing), facing.getOpposite(), stack.copy());
            this.updateCache();
            return extracted;
        }
        return false;
    }

    @Override
    public BlockPos getProviderPipePos() {
        return this.getBlockPos();
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(ProviderPipeBlock.FACING).getDirection();
    }

    public Filter getFilter() {
        return this.filter;
    }

}
