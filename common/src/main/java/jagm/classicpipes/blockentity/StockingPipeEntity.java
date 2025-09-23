package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.StockingPipeBlock;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.RequestedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
        if (!this.cacheInitialised) {
            this.updateCache(level);
            this.cacheInitialised = true;
        }
    }

    public void updateCache(ServerLevel level) {
        this.missingItemsCache.clear();
        Direction facing = this.getBlockState().getValue(StockingPipeBlock.FACING).getDirection();
        if (facing != null) {
            List<ItemStack> filterItems = new ArrayList<>();
            for (int i = 0; i < this.filter.getContainerSize(); i++) {
                ItemStack stack = this.filter.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                boolean matched = false;
                for (ItemStack filterStack : filterItems) {
                    if (ItemStack.isSameItemSameComponents(stack, filterStack)) {
                        filterStack.grow(stack.getCount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    filterItems.add(stack.copy());
                }
            }
            if (!filterItems.isEmpty()) {
                List<ItemStack> containerItems = Services.LOADER_SERVICE.getContainerItems(level, this.getBlockPos().relative(facing), facing.getOpposite());
                for (ItemStack filterStack : filterItems) {
                    boolean matched = false;
                    for (ItemStack containerStack : containerItems) {
                        if (ItemStack.isSameItemSameComponents(filterStack, containerStack)) {
                            matched = true;
                            int missing = filterStack.getCount() - containerStack.getCount();
                            if (missing > 0) {
                                this.missingItemsCache.add(containerStack.copyWithCount(missing));
                            }
                            break;
                        }
                    }
                    if (!matched) {
                        this.missingItemsCache.add(filterStack);
                    }
                }
            }
        }
        if (this.activeStocking) {
            this.tryRequests(level);
        }
    }

    public void updateCache() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            this.updateCache(serverLevel);
        }
    }

    public void tryRequests(ServerLevel level) {
        if (this.hasNetwork()) {
            for (ItemStack stack : this.missingItemsCache) {
                int alreadyRequested = 0;
                for (ItemInPipe item : this.contents) {
                    if (ItemStack.isSameItemSameComponents(stack, item.getStack())) {
                        alreadyRequested += item.getStack().getCount();
                    }
                }
                for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
                    if (requestedItem.matches(stack) && requestedItem.getDestination().equals(this.getBlockPos())) {
                        alreadyRequested += requestedItem.getAmountRemaining();
                    }
                }
                if (alreadyRequested < stack.getCount()) {
                    this.getNetwork().request(level, stack.copyWithCount(stack.getCount() - alreadyRequested), this.getBlockPos(), null, true);
                }
            }
        }
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
    protected void loadAdditional(CompoundTag valueInput, HolderLookup.Provider registries) {
        this.filter.clearContent();
        super.loadAdditional(valueInput, registries);
        ListTag filterList = valueInput.getList("filter", ListTag.TAG_COMPOUND);
        filterList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getInt("slot");
                MiscUtil.loadFromTag(tag, ItemStack.CODEC, registries, stack -> this.filter.setItem(slot, stack));
            }
        });
        this.filter.setMatchComponents(valueInput.getBoolean("match_components"));
        this.activeStocking = valueInput.getBoolean("active_stocking");
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput, HolderLookup.Provider registries) {
        super.saveAdditional(valueOutput, registries);
        ListTag filterList = new ListTag();
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("slot", slot);
                MiscUtil.saveToTag(tag, stack, ItemStack.CODEC, registries, filterList::add);
            }
        }
        valueOutput.put("filter", filterList);
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

}
