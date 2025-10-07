package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.container.SingleItemFilterContainer;
import jagm.classicpipes.inventory.menu.RoutingPipeMenu;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.BlockPos;
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

public class RoutingPipeEntity extends NetworkedPipeEntity implements MenuProvider {

    private final SingleItemFilterContainer filter;
    private boolean defaultRoute;

    public RoutingPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.ROUTING_PIPE_ENTITY, pos, state);
        this.filter = new SingleItemFilterContainer(this, 9, false);
        this.defaultRoute = false;
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
        this.defaultRoute = valueInput.getBoolean("default_route");
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

}
