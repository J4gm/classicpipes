package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.DirectionalFilterContainer;
import jagm.classicpipes.inventory.menu.DiamondPipeMenu;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class DiamondPipeEntity extends RoundRobinPipeEntity implements MenuProvider {

    private final DirectionalFilterContainer filter;

    public DiamondPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.DIAMOND_PIPE_ENTITY, pos, state);
        this.filter = new DirectionalFilterContainer(this, false);
    }

    @Override
    protected List<Direction> getValidDirections(BlockState state, ItemInPipe item) {
        List<Direction> validDirections = new ArrayList<>();
        Direction direction = MiscUtil.nextDirection(item.getFromDirection());
        for (int i = 0; i < 5; i++) {
            if (this.isPipeConnected(state, direction) && filter.directionMatches(item.getStack(), direction)) {
                validDirections.add(direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        if (validDirections.isEmpty() && filter.directionMatches(item.getStack(), direction)) {
            validDirections.add(item.getFromDirection());
        }
        if (validDirections.isEmpty()) {
            direction = MiscUtil.nextDirection(item.getFromDirection());
            for (int i = 0; i < 5; i++) {
                if (this.isPipeConnected(state, direction) && filter.directionEmpty(direction)) {
                    validDirections.add(direction);
                }
                direction = MiscUtil.nextDirection(direction);
            }
        }
        if (validDirections.isEmpty() && filter.directionEmpty(item.getFromDirection())) {
            validDirections.add(item.getFromDirection());
        }
        return validDirections;
    }

    @Override
    protected boolean canJoinNetwork() {
        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag valueInput, HolderLookup.Provider registries) {
        this.filter.clearContent();
        super.loadAdditional(valueInput, registries);
        ListTag filterList = valueInput.getListOrEmpty("filter");
        filterList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getIntOr("slot", 0);
                MiscUtil.loadFromTag(tag, ItemStack.CODEC, registries, stack -> this.filter.setItem(slot, stack));
            }
        });
        this.filter.setMatchComponents(valueInput.getBooleanOr("match_components", false));
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
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".diamond_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DiamondPipeMenu(id, playerInventory, this.filter);
    }

    public boolean shouldMatchComponents() {
        return this.filter.shouldMatchComponents();
    }

}
