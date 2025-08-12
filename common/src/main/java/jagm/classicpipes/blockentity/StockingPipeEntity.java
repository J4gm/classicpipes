package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.StockingPipeBlock;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.menu.StockingPipeMenu;
import jagm.classicpipes.services.Services;
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

    private static final short DEFAULT_COOLDOWN = 40;

    private final FilterContainer filter;
    private boolean activeStocking;
    private final List<ItemStack> missingItemsCache;
    private boolean cacheInitialised = false;
    private short cooldown;

    public StockingPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.STOCKING_PIPE_ENTITY, pos, state);
        this.filter = new FilterContainer(this, 9, true);
        this.activeStocking = false;
        this.missingItemsCache = new ArrayList<>();
        this.cooldown = 0;
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.cacheInitialised) {
            this.updateCache(level);
            this.cacheInitialised = true;
        }
        if (this.cooldown == 1 && this.activeStocking) {
           this.tryRequests(level);
        }
        this.cooldown--;
        if (this.cooldown < 0) {
            this.cooldown = 0;
        }
        super.tickServer(level, pos, state);
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
                        missingItemsCache.add(filterStack);
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
        if (this.hasNetwork() && this.cooldown <= 1) {
            for (ItemStack stack : this.missingItemsCache) {
                boolean alreadyRequested = false;
                for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
                    if (requestedItem.matches(stack) && requestedItem.getDestination().equals(this.getBlockPos())) {
                        alreadyRequested = true;
                        this.cooldown = DEFAULT_COOLDOWN;
                    }
                }
                if (!alreadyRequested) {
                    this.getNetwork().request(level, stack, this.getBlockPos(), null, true);
                }
            }
        }
        this.cooldown = 0;
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

    public void resetCooldown() {
        this.cooldown = DEFAULT_COOLDOWN;
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

}
