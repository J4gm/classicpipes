package jagm.classicpipes.services;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.network.PayloadWrapper;
import jagm.classicpipes.network.SelfHandler;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.function.TriFunction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface LoaderService {

    <B extends BlockEntity> BlockEntityType<B> createBlockEntityType(BiFunction<BlockPos, BlockState, B> blockEntitySupplier, Block... validBlocks);

    <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(TriFunction<Integer, Inventory, D, M> menuSupplier, SelfHandler<D> handler);

    <M extends AbstractContainerMenu> MenuType<M> createSimpleMenuType(BiFunction<Integer, Inventory, M> menuSupplier);

    <D> void openMenu(ServerPlayer player, MenuProvider menuProvider, D payload, SelfHandler<D> handler);

    <T extends PayloadWrapper<T>> void sendToServer(T payload);

    <T extends PayloadWrapper<T>> void sendToClient(ServerPlayer player, T payload);

    boolean canAccessContainer(Level level, BlockPos containerPos, Direction face);

    boolean handleItemInsertion(ItemPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, ItemInPipe item);

    boolean handleItemExtraction(ItemPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<ItemStack> predicate);

    List<ItemStack> getContainerItems(ServerLevel level, BlockPos pos, Direction face);

    boolean extractSpecificItem(ItemPipeEntity pipe, ServerLevel level, BlockPos containerPos, Direction face, ItemStack stack);

    String getModName(String modId);

    boolean handleFluidInsertion(FluidPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, BlockEntity containerEntity, BlockPos containerPos, Fluid fluid, FluidInPipe fluidPacket);

    boolean canAccessFluidContainer(Level level, BlockPos containerPos, Direction face);

    boolean handleFluidExtraction(FluidPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<Fluid> predicate);

    FluidRenderInfo getFluidRenderInfo(FluidState fluidState, BlockAndTintGetter level, BlockPos pos);

    FluidRenderInfo getFluidRenderInfo(FluidState fluidState);

    Fluid getFluidFromStack(ItemStack stack);

    Component getFluidName(Fluid fluid);

    DispenseItemBehavior getDispenserBehaviour(ItemStack stack);

    default boolean quarkInstalled() {
        return false;
    }

    default boolean isQuarkCrafter(BlockEntity container) {
        return false;
    }

    default void setQuarkCrafterSlotState(BlockEntity crafter, int slot, boolean state) {

    }

    default void quarkCrafterCraft(ServerLevel level, BlockEntity crafter) {

    }

}
