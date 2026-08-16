package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.NetworkedPipeBlock;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.RoutingPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutingPipeEntity extends NetworkedPipeEntity implements MenuProvider, OverflowHandlingPipe {

    private final SingleItemFilterContainer filter;
    private boolean defaultRoute;
    private final Map<Direction, List<ItemStack>> cannotFit;
    private int overflowCheck;

    public RoutingPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.ROUTING_PIPE_ENTITY, pos, state);
        this.filter = new SingleItemFilterContainer(this, 9, false);
        this.defaultRoute = false;
        this.cannotFit = new HashMap<>();
        this.overflowCheck = 0;
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (this.overflowCheck++ > 100) {
            this.overflowCheck = 0;
            this.cannotFit.clear();
        }
    }

    @Override
    public void load(CompoundTag valueInput) {
        this.filter.clearContent();
        super.load(valueInput);
        ListTag filterList = valueInput.getList("filter", ListTag.TAG_COMPOUND);
        filterList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getInt("slot");
                MiscUtil.loadFromTag(tag, ItemStack.CODEC, stack -> this.filter.setItem(slot, stack));
            }
        });
        this.filter.setMatchComponents(valueInput.getBoolean("match_components"));
        this.defaultRoute = valueInput.getBoolean("default_route");
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        ListTag filterList = new ListTag();
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("slot", slot);
                MiscUtil.saveToTag(tag, stack, ItemStack.CODEC, filterList::add);
            }
        }
        valueOutput.put("filter", filterList);
        valueOutput.putBoolean("match_components", this.filter.shouldMatchComponents());
        valueOutput.putBoolean("default_route", this.defaultRoute);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".routing_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RoutingPipeMenu(id, playerInventory, this.filter, this.defaultRoute);
    }

    public boolean shouldMatchComponents() {
        return this.filter.shouldMatchComponents();
    }

    @Override
    public boolean isDefaultRoute() {
        return this.defaultRoute;
    }

    public void setDefaultRoute(boolean defaultRoute) {
        this.defaultRoute = defaultRoute;
        if (defaultRoute) {
            this.getNetwork().addPipe(this);
        } else {
            this.getNetwork().getDefaultRoutes().remove(this);
        }
    }

    public FilterContainer.MatchingResult canRouteItemHere(ItemStack stack) {
        return this.filter.matches(stack);
    }

    @Override
    public void markCannotFit(ItemStack stack, Direction direction) {
        if (this.cannotFit.containsKey(direction)) {
            MiscUtil.mergeStackIntoList(this.cannotFit.get(direction), stack);
        } else {
            List<ItemStack> stacks = new ArrayList<>();
            stacks.add(stack);
            this.cannotFit.put(direction, stacks);
        }
    }

    @Override
    public boolean itemCanFit(ItemStack stack) {
        for (Direction direction : Direction.values()) {
            if (this.getBlockState().getValue(NetworkedPipeBlock.PROPERTY_BY_DIRECTION.get(direction)).equals(NetworkedPipeBlock.ConnectionState.UNLINKED)) {
                if (!this.cannotFit.containsKey(direction) || this.cannotFit.get(direction).stream().noneMatch(cannotFitStack -> ItemStack.isSameItemSameTags(cannotFitStack, stack))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean itemCanFit(ItemStack stack, Direction direction) {
        if (this.getBlockState().getValue(NetworkedPipeBlock.PROPERTY_BY_DIRECTION.get(direction)).equals(NetworkedPipeBlock.ConnectionState.UNLINKED)) {
            return !this.cannotFit.containsKey(direction) || this.cannotFit.get(direction).stream().noneMatch(cannotFitStack -> ItemStack.isSameItemSameTags(cannotFitStack, stack));
        }
        return false;
    }

}
