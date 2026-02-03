package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.StockingPipeBlock;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.item.LabelItem;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.RequestedItem;
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
import java.util.List;

public class StockingPipeEntity extends NetworkedPipeEntity implements MenuProvider {

    private final FilterContainer filter;
    private boolean activeStocking;
    private final List<ItemStack> missingItemsCache;
    private boolean cacheInitialised = false;

    public StockingPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.STOCKING_PIPE_ENTITY, pos, state);
        this.filter = new FilterContainer(this, 9, true);
        this.activeStocking = false;
        this.missingItemsCache = new ArrayList<>();
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (!this.cacheInitialised && !state.getValue(StockingPipeBlock.FACING).equals(FacingOrNone.NONE)) {
            this.updateCache(level);
            this.cacheInitialised = true;
        }
    }

    public void updateCache(ServerLevel level) {
        this.missingItemsCache.clear();
        Direction facing = this.getBlockState().getValue(StockingPipeBlock.FACING).getDirection();
        if (facing != null) {
            List<ItemStack> filterItems = new ArrayList<>();
            for (ItemStack stack : this.filter) {
                if (stack.isEmpty()) {
                    continue;
                }
                MiscUtil.mergeStackIntoList(filterItems, stack.copy());
            }
            BlockPos containerPos = this.getBlockPos().relative(facing);
            if (!filterItems.isEmpty() && Services.LOADER_SERVICE.canAccessContainer(level, containerPos, facing.getOpposite())) {
                List<ItemStack> containerItems = Services.LOADER_SERVICE.getContainerItems(level, containerPos, facing.getOpposite());
                for (ItemStack filterStack : filterItems) {
                    int amountFound = 0;
                    boolean isLabel = filterStack.getItem() instanceof LabelItem;
                    for (ItemStack containerStack : containerItems) {
                        if (isLabel) {
                            if (((LabelItem) filterStack.getItem()).itemMatches(filterStack, containerStack)) {
                                amountFound += containerStack.getCount();
                            }
                        } else if (ItemStack.isSameItemSameComponents(filterStack, containerStack)) {
                            amountFound += containerStack.getCount();
                            break;
                        }
                    }
                    if (amountFound < filterStack.getCount()) {
                        this.missingItemsCache.add(filterStack.copyWithCount(filterStack.getCount() - amountFound));
                    }
                }
                if (this.activeStocking) {
                    this.tryRequests(level);
                }
            }
        }
    }

    public void updateCache() {
        this.cacheInitialised = false;
    }

    public void tryRequests(ServerLevel level) {
        if (this.hasNetwork()) {
            for (ItemStack stack : this.missingItemsCache) {
                int alreadyRequested = this.getAlreadyRequested(stack);
                if (alreadyRequested < stack.getCount()) {
                    this.getNetwork().request(level, stack.copyWithCount(stack.getCount() - alreadyRequested), this.getBlockPos(), null, true);
                }
            }
        }
    }

    public int getAlreadyRequested(ItemStack stack) {
        boolean isLabel = stack.getItem() instanceof LabelItem;
        int alreadyRequested = 0;
        for (ItemInPipe item : this.contents) {
            ItemStack pipeStack = item.getStack();
            if (ItemStack.isSameItemSameComponents(stack, pipeStack) || isLabel && ((LabelItem) stack.getItem()).itemMatches(stack, pipeStack)) {
                alreadyRequested += pipeStack.getCount();
            }
        }
        for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
            if (requestedItem.getDestination().equals(this.getBlockPos()) && (requestedItem.matches(stack) || isLabel && ((LabelItem) stack.getItem()).itemMatches(stack, requestedItem.getStack()))) {
                alreadyRequested += requestedItem.getAmountRemaining();
            }
        }
        return alreadyRequested;
    }

    public boolean isActiveStocking() {
        return this.activeStocking;
    }

    public void setActiveStocking(boolean activeStocking) {
        this.activeStocking = activeStocking;
        if (activeStocking && this.getLevel() instanceof ServerLevel serverLevel) {
            this.tryRequests(serverLevel);
        }
    }

    public boolean shouldMatchComponents() {
        return this.filter.shouldMatchComponents();
    }

    public List<ItemStack> getMissingItemsCache() {
        return this.missingItemsCache;
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        this.filter.clearContent();
        super.loadAdditional(valueInput);
        ValueInput.TypedInputList<ItemStackWithSlot> filterList = valueInput.listOrEmpty("filter", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : filterList) {
            this.filter.setItem(slotStack.slot(), slotStack.stack());
        }
        this.filter.setMatchComponents(valueInput.getBooleanOr("match_components", true));
        this.activeStocking = valueInput.getBooleanOr("active_stocking", false);
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
        valueOutput.putBoolean("active_stocking", this.activeStocking);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".stocking_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new StockingPipeMenu(id, playerInventory, this.filter, this.activeStocking);
    }

    public Filter getFilter() {
        return this.filter;
    }

}
