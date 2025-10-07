package jagm.classicpipes.inventory.container;

import jagm.classicpipes.blockentity.PipeEntity;
import jagm.classicpipes.item.LabelItem;
import jagm.classicpipes.item.TagLabelItem;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DirectionalFilterContainer implements Filter {

    private final Map<Direction, NonNullList<ItemStack>> filterMap;
    private final PipeEntity pipe;
    private boolean matchComponents;

    public DirectionalFilterContainer(PipeEntity pipe, boolean matchComponents) {
        this.pipe = pipe;
        this.filterMap = new HashMap<>();
        this.matchComponents = matchComponents;
        this.clearContent();
    }

    public MatchingResult directionMatches(ItemStack stack, Direction direction) {
        for (ItemStack filterStack : filterMap.get(direction)) {
            if (filterStack.getItem() instanceof LabelItem labelItem && labelItem.itemMatches(filterStack, stack)) {
                return labelItem instanceof TagLabelItem ? MatchingResult.TAG : MatchingResult.MOD;
            } else if (filterStack.is(stack.getItem()) && (!this.shouldMatchComponents() || ItemStack.isSameItemSameComponents(stack, filterStack))) {
                return MatchingResult.ITEM;
            }
        }
        return MatchingResult.FALSE;
    }

    public boolean directionEmpty(Direction direction) {
        return filterMap.get(direction).stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    public boolean isEmpty() {
        for (Direction direction : Direction.values()) {
            if (!this.directionEmpty(direction)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return filterMap.get(Direction.from3DDataValue(row)).get(col);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        int row = slot / 9;
        int col = slot % 9;
        filterMap.get(Direction.from3DDataValue(row)).set(col, ItemStack.EMPTY);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        int row = slot / 9;
        int col = slot % 9;
        filterMap.get(Direction.from3DDataValue(row)).set(col, stack);
    }

    @Override
    public void setChanged() {
        if (pipe != null && pipe.getLevel() != null) {
            pipe.getLevel().sendBlockUpdated(pipe.getBlockPos(), pipe.getBlockState(), pipe.getBlockState(), 2);
            pipe.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (pipe != null) {
            return Container.stillValidBlockEntity(pipe, player);
        } else {
            return true;
        }
    }

    @Override
    public void clearContent() {
        for (Direction direction : Direction.values()) {
            filterMap.put(direction, NonNullList.withSize(9, ItemStack.EMPTY));
        }
    }

    @Override
    public void setMatchComponents(boolean matchComponents) {
        this.matchComponents = matchComponents;
        this.setChanged();
    }

    @Override
    public boolean shouldMatchComponents() {
        return this.matchComponents;
    }

    @Override
    public PipeEntity getPipe() {
        return this.pipe;
    }

}
