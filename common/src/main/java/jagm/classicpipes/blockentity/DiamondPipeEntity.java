package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.DirectionalFilterContainer;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.menu.DiamondPipeMenu;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<Filter.MatchingResult, List<Direction>> matchPriority = new HashMap<>();
        matchPriority.put(Filter.MatchingResult.ITEM, new ArrayList<>());
        matchPriority.put(Filter.MatchingResult.TAG, new ArrayList<>());
        matchPriority.put(Filter.MatchingResult.MOD, new ArrayList<>());
        for (int i = 0; i < 5; i++) {
            Filter.MatchingResult result = filter.directionMatches(item.getStack(), direction);
            if (this.isPipeConnected(state, direction) && result.matches) {
                matchPriority.get(result).add(direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        if (!matchPriority.get(Filter.MatchingResult.ITEM).isEmpty()) {
            validDirections.addAll(matchPriority.get(Filter.MatchingResult.ITEM));
        } else if (!matchPriority.get(Filter.MatchingResult.TAG).isEmpty()) {
            validDirections.addAll(matchPriority.get(Filter.MatchingResult.TAG));
        } else if (!matchPriority.get(Filter.MatchingResult.MOD).isEmpty()) {
            validDirections.addAll(matchPriority.get(Filter.MatchingResult.MOD));
        }
        if (validDirections.isEmpty() && filter.directionMatches(item.getStack(), direction).matches) {
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
    protected void loadAdditional(ValueInput valueInput) {
        this.filter.clearContent();
        super.loadAdditional(valueInput);
        ValueInput.TypedInputList<ItemStackWithSlot> filterList = valueInput.listOrEmpty("filter", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : filterList) {
            this.filter.setItem(slotStack.slot(), slotStack.stack());
        }
        this.filter.setMatchComponents(valueInput.getBooleanOr("match_components", false));
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
