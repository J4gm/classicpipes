package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.MatchingPipeBlock;
import jagm.classicpipes.block.ProviderPipeBlock;
import jagm.classicpipes.inventory.menu.StoragePipeMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

import static jagm.classicpipes.block.NetworkedPipeBlock.ENABLED;
public class StoragePipeEntity extends NetworkedPipeEntity implements MenuProvider, ProviderPipe, MatchingPipe {

    private boolean defaultRoute;
    private boolean matchComponents;
    private boolean leaveOne;
    private final List<ItemStack> providerCache;
    private final List<ItemStack> matchingCache;
    private boolean cacheInitialised;
    private final List<ItemStack> cannotFit;

    public StoragePipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.STORAGE_PIPE_ENTITY, pos, state);
        this.defaultRoute = false;
        this.matchComponents = false;
        this.leaveOne = false;
        this.providerCache = new ArrayList<>();
        this.matchingCache = new ArrayList<>();
        this.cacheInitialised = false;
        this.cannotFit = new ArrayList<>();
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        if (!this.cacheInitialised && !state.getValue(MatchingPipeBlock.FACING).equals(FacingOrNone.NONE)) {
            this.updateCache(level, pos, state);
            this.cacheInitialised = true;
        }
        super.tickServer(level, pos, state);
    }

    private void updateCache(ServerLevel level, BlockPos pos, BlockState state) {
        this.providerCache.clear();
        this.matchingCache.clear();
        if (state.getValue(ENABLED)) {
            Direction facing = state.getValue(MatchingPipeBlock.FACING).getDirection();
            this.cannotFit.clear();
            List<ItemStack> stacks = Services.LOADER_SERVICE.getContainerItems(level, pos.relative(facing), facing.getOpposite());
            for (ItemStack stack : stacks) {
                this.matchingCache.add(stack.copy());
                if (this.shouldLeaveOne()) {
                    stack.shrink(1);
                }
                if (!stack.isEmpty()) {
                    this.providerCache.add(stack);
                }
            }
        }
        if (this.hasNetwork()) {
            this.getNetwork().cacheUpdated();
        }
    }

    @Override
    public void updateCache() {
        this.cacheInitialised = false;
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(ProviderPipeBlock.FACING).getDirection();
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (this.itemCanFit(stack)) {
            for (ItemStack containerStack : this.matchingCache) {
                if (stack.is(containerStack.getItem()) && (!this.shouldMatchComponents() || ItemStack.isSameItemSameComponents(stack, containerStack))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean itemCanFit(ItemStack stack) {
        for (ItemStack cannotFitStack : this.cannotFit) {
            if (ItemStack.isSameItemSameComponents(cannotFitStack, stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public NetworkedPipeEntity getAsPipe() {
        return this;
    }

    @Override
    public void markCannotFit(ItemStack stack) {
        this.cannotFit.add(stack);
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

    public boolean shouldMatchComponents() {
        return this.matchComponents;
    }

    public void setMatchComponents(boolean matchComponents) {
        this.matchComponents = matchComponents;
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

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        this.cacheInitialised = false;
        super.loadAdditional(valueInput);
        this.defaultRoute = valueInput.getBooleanOr("default_route", false);
        this.matchComponents = valueInput.getBooleanOr("match_components", false);
        this.leaveOne = valueInput.getBooleanOr("leave_one", false);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putBoolean("default_route", this.isDefaultRoute());
        valueOutput.putBoolean("match_components", this.shouldMatchComponents());
        valueOutput.putBoolean("leave_one", this.shouldLeaveOne());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".storage_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StoragePipeMenu(id, inventory, this.isDefaultRoute(), this.shouldMatchComponents(), this.shouldLeaveOne(), this);
    }

    @Override
    public List<ItemStack> getCache() {
        return this.providerCache;
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
}
