package jagm.classicpipes.services;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.function.TriFunction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface LoaderService {

    <B extends BlockEntity> BlockEntityType<B> createBlockEntityType(BiFunction<BlockPos, BlockState, B> blockEntitySupplier, Block... validBlocks);

    <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(TriFunction<Integer, Inventory, D, M> menuSupplier, StreamCodec<RegistryFriendlyByteBuf, D> codec);

    <M extends AbstractContainerMenu> MenuType<M> createSimpleMenuType(BiFunction<Integer, Inventory, M> menuSupplier);

    <D> void openMenu(ServerPlayer player, MenuProvider menuProvider, D payload, StreamCodec<RegistryFriendlyByteBuf, D> codec);

    void sendToServer(CustomPacketPayload payload);

    void sendToClient(ServerPlayer player, CustomPacketPayload payload);

    boolean canAccessContainer(Level level, BlockPos containerPos, Direction face);

    boolean handleItemInsertion(ItemPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, ItemInPipe item);

    boolean handleItemExtraction(ItemPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<ItemStack> predicate);

    List<ItemStack> getExtractableContainerItems(ServerLevel level, BlockPos pos, Direction face);

    List<ItemStack> getAllContainerItems(ServerLevel level, BlockPos pos, Direction face);

    boolean extractSpecificItem(ItemPipeEntity pipe, ServerLevel level, BlockPos containerPos, Direction face, ItemStack stack);

    String getModName(String modId);

    boolean handleFluidInsertion(FluidPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, BlockEntity containerEntity, BlockPos containerPos, Fluid fluid, Object fluidData, FluidInPipe fluidPacket);

    boolean canAccessFluidContainer(Level level, BlockPos containerPos, Direction face);

    boolean handleFluidExtraction(FluidPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<Fluid> predicate);

    FluidRenderInfo getFluidRenderInfo(Fluid fluid, Object fluidData, BlockAndTintGetter level, BlockPos pos);

    FluidRenderInfo getFluidRenderInfo(Fluid fluid, Object fluidData);

    Fluid getFluidFromStack(ItemStack stack);

    Component getFluidName(Fluid fluid);

    default void loadFluidData(CompoundTag valueInput, HolderLookup.Provider registries, BiConsumer<Fluid, Object> setFluid) {
        Fluid fluid = BuiltInRegistries.FLUID.byNameCodec().parse(registries.createSerializationContext(NbtOps.INSTANCE), valueInput.get("fluid")).result().orElse(Fluids.WATER);
        DataComponentPatch components = DataComponentPatch.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), valueInput.get("fluid_data")).result().orElse(DataComponentPatch.EMPTY);
        setFluid.accept(fluid, components);
    }

    default void saveFluidData(CompoundTag valueOutput, Object fluidData) {
        if (fluidData instanceof DataComponentPatch components) {
            valueOutput.put("fluid_data", DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, components).getOrThrow());
        }
    }

}
