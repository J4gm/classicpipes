package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.DirectionalFilterContainer;
import jagm.classicpipes.inventory.menu.DiamondFluidPipeMenu;
import jagm.classicpipes.util.FluidInPipe;
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

public class DiamondFluidPipeEntity extends FluidPipeEntity implements MenuProvider {

    private final DirectionalFilterContainer filter;

    public DiamondFluidPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY, pos, state);
        this.filter = new DirectionalFilterContainer(this, false);
    }

    @Override
    protected List<Direction> getValidDirections(BlockState state, FluidInPipe fluidPacket) {
        List<Direction> validDirections = new ArrayList<>();
        Direction direction = MiscUtil.nextDirection(fluidPacket.getFromDirection());
        ItemStack bucket = new ItemStack(this.getFluid().getBucket());
        for (int i = 0; i < 5; i++) {
            if (this.isPipeConnected(state, direction) && filter.directionMatches(bucket, direction)) {
                validDirections.add(direction);
            }
            direction = MiscUtil.nextDirection(direction);
        }
        if (validDirections.isEmpty() && filter.directionMatches(bucket, direction)) {
            validDirections.add(fluidPacket.getFromDirection());
        }
        if (validDirections.isEmpty()) {
            direction = MiscUtil.nextDirection(fluidPacket.getFromDirection());
            for (int i = 0; i < 5; i++) {
                if (this.isPipeConnected(state, direction) && filter.directionEmpty(direction)) {
                    validDirections.add(direction);
                }
                direction = MiscUtil.nextDirection(direction);
            }
        }
        if (validDirections.isEmpty() && filter.directionEmpty(fluidPacket.getFromDirection())) {
            validDirections.add(fluidPacket.getFromDirection());
        }
        return validDirections;
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
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".diamond_fluid_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new DiamondFluidPipeMenu(id, playerInventory, this.filter);
    }

}
