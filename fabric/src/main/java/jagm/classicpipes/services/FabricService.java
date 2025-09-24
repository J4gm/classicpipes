package jagm.classicpipes.services;

import jagm.classicpipes.FabricEntrypoint;
import jagm.classicpipes.block.FluidPipeBlock;
import jagm.classicpipes.block.PipeBlock;
import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.network.PayloadWrapper;
import jagm.classicpipes.network.SelfHandler;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class FabricService implements LoaderService {

    @Override
    public <B extends BlockEntity> BlockEntityType<B> createBlockEntityType(BiFunction<BlockPos, BlockState, B> blockEntitySupplier, Block... validBlocks) {
        return BlockEntityType.Builder.of(blockEntitySupplier::apply, validBlocks).build(null);
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(TriFunction<Integer, Inventory, D, M> menuSupplier, SelfHandler<D> handler) {
        return new ExtendedScreenHandlerType<>((id, inventory, buffer) -> menuSupplier.apply(id, inventory, handler.decode(buffer)));
    }

    @Override
    public <M extends AbstractContainerMenu> MenuType<M> createSimpleMenuType(BiFunction<Integer, Inventory, M> menuSupplier) {
        return new MenuType<>(menuSupplier::apply, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    public <D> void openMenu(ServerPlayer player, MenuProvider menuProvider, D payload, SelfHandler<D> handler) {
        player.openMenu(new ExtendedScreenHandlerFactory() {

            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buffer) {
                handler.encode(payload, buffer);
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return menuProvider.createMenu(id, inventory, player);
            }

            @Override
            public Component getDisplayName() {
                return menuProvider.getDisplayName();
            }

        });
    }

    @Override
    public boolean canAccessContainer(Level level, BlockPos containerPos, Direction face) {
        BlockState state = level.getBlockState(containerPos);
        if (state.getBlock() instanceof PipeBlock) {
            return false;
        }
        Storage<ItemVariant> itemHandler = ItemStorage.SIDED.find(level, containerPos, face);
        if (itemHandler != null) {
            return itemHandler.supportsExtraction() || itemHandler.supportsInsertion();
        }
        return false;
    }

    @Override
    public <T extends PayloadWrapper<T>> void sendToServer(T payload) {
        ClientPlayNetworking.send(payload.getType(), payload.getHandler().encode(payload, PacketByteBufs.create()));
    }

    @Override
    public <T extends PayloadWrapper<T>> void sendToClient(ServerPlayer player, T payload) {
        ServerPlayNetworking.send(player, payload.getType(), payload.getHandler().encode(payload, PacketByteBufs.create()));
    }

    @Override
    public boolean handleItemInsertion(ItemPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, ItemInPipe item) {
        BlockPos containerPos = pipePos.relative(item.getTargetDirection());
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof ItemPipeEntity nextPipe) {
            item.resetProgress(item.getTargetDirection().getOpposite());
            nextPipe.insertPipeItem(level, item);
            level.sendBlockUpdated(containerPos, nextPipe.getBlockState(), nextPipe.getBlockState(), 2);
            return true;
        }
        Direction face = item.getTargetDirection().getOpposite();
        Storage<ItemVariant> itemHandler = ItemStorage.SIDED.find(level, containerPos, face);
        if (itemHandler != null) {
            int count = item.getStack().getCount();
            int inserted;
            try (Transaction transaction = Transaction.openOuter()) {
                inserted = (int) itemHandler.insert(ItemVariant.of(item.getStack()), count, transaction);
                transaction.commit();
            }
            if (inserted >= count) {
                return true;
            }
            item.setStack(item.getStack().copyWithCount(count - inserted));
        }
        item.resetProgress(item.getTargetDirection());
        pipe.routeItem(pipeState, item);
        return false;
    }

    @Override
    public boolean handleItemExtraction(ItemPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<ItemStack> predicate) {
        BlockState state = level.getBlockState(containerPos);
        if (state.getBlock() instanceof PipeBlock) {
            return false;
        }
        Storage<ItemVariant> itemHandler = ItemStorage.SIDED.find(level, containerPos, face);
        if (itemHandler != null) {
            List<StorageView<ItemVariant>> itemViewList = new ArrayList<>();
            itemHandler.nonEmptyIterator().forEachRemaining(itemViewList::add);
            ItemStack stack = ItemStack.EMPTY;
            try (Transaction transaction = Transaction.openOuter()) {
                for (int i = itemViewList.size() - 1; i >= 0; i--) {
                    StorageView<ItemVariant> itemView = itemViewList.get(i);
                    ItemVariant resource = itemView.getResource();
                    if (predicate.test(resource.toStack())) {
                        int extracted = (int) itemView.extract(resource, amount, transaction);
                        if (extracted > 0) {
                            stack = resource.toStack(extracted);
                            transaction.commit();
                            break;
                        }
                    }
                }
            }
            if (!stack.isEmpty()) {
                pipe.setItem(face.getOpposite(), stack);
                return true;
            }
        }
        return false;
    }

    public List<ItemStack> getContainerItems(ServerLevel level, BlockPos pos, Direction face) {
        Storage<ItemVariant> itemHandler = ItemStorage.SIDED.find(level, pos, face);
        if (itemHandler != null) {
            List<ItemStack> stacks = new ArrayList<>();
            Iterator<StorageView<ItemVariant>> iterator = itemHandler.nonEmptyIterator();
            while (iterator.hasNext()) {
                StorageView<ItemVariant> itemView = iterator.next();
                boolean matched = false;
                for (ItemStack stack : stacks) {
                    if (itemView.getResource().matches(stack)) {
                        stack.setCount(stack.getCount() + (int) itemView.getAmount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    stacks.add(itemView.getResource().toStack((int) itemView.getAmount()));
                }
            }
            return stacks;
        }
        return List.of();
    }

    @Override
    public boolean extractSpecificItem(ItemPipeEntity pipe, ServerLevel level, BlockPos containerPos, Direction face, ItemStack stack) {
        Storage<ItemVariant> itemHandler = ItemStorage.SIDED.find(level, containerPos, face);
        boolean success = false;
        if (itemHandler != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                int extracted = (int) itemHandler.extract(ItemVariant.of(stack), stack.getCount(), transaction);
                if (extracted == stack.getCount()) {
                    success = true;
                    transaction.commit();
                    int itemsLeft = extracted;
                    while (itemsLeft > 0) {
                        int removed = Math.min(stack.getMaxStackSize(), itemsLeft);
                        pipe.setItem(face.getOpposite(), stack.copyWithCount(removed));
                        itemsLeft -= removed;
                    }
                } else {
                    transaction.abort();
                }
            }
        }
        return success;
    }

    @Override
    public String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(ModContainer::getMetadata).map(ModMetadata::getName).orElse(modId);
    }

    @Override
    public boolean handleFluidInsertion(FluidPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, BlockEntity containerEntity, BlockPos containerPos, Fluid fluid, FluidInPipe fluidPacket) {
        Direction face = fluidPacket.getTargetDirection().getOpposite();
        Storage<FluidVariant> fluidHandler = FluidStorage.SIDED.find(level, containerPos, containerEntity.getBlockState(), containerEntity, face);
        if (fluidHandler != null && fluidHandler.supportsInsertion()) {
            long inserted;
            long amount = fluidPacket.getAmount() * FabricEntrypoint.FLUID_CONVERSION_RATE;
            try (Transaction transaction = Transaction.openOuter()) {
                inserted = fluidHandler.insert(FluidVariant.of(fluid), amount, transaction);
                transaction.commit();
            }
            if (inserted >= amount) {
                return true;
            }
            fluidPacket.setAmount((int) ((amount - inserted) / FabricEntrypoint.FLUID_CONVERSION_RATE));
        }
        return false;
    }

    @Override
    public boolean canAccessFluidContainer(Level level, BlockPos containerPos, Direction face) {
        BlockState state = level.getBlockState(containerPos);
        if (state.getBlock() instanceof FluidPipeBlock) {
            return false;
        }
        Storage<FluidVariant> fluidHandler = FluidStorage.SIDED.find(level, containerPos, face);
        if (fluidHandler != null) {
            return fluidHandler.supportsExtraction() || fluidHandler.supportsInsertion();
        }
        return false;
    }

    @Override
    public boolean handleFluidExtraction(FluidPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<Fluid> predicate) {
        BlockState state = level.getBlockState(containerPos);
        if (state.getBlock() instanceof FluidPipeBlock || pipe.totalAmount() >= FluidPipeEntity.CAPACITY) {
            return false;
        }
        Storage<FluidVariant> fluidHandler = FluidStorage.SIDED.find(level, containerPos, face);
        if (fluidHandler != null && fluidHandler.supportsExtraction()) {
            long extracted = 0;
            try (Transaction transaction = Transaction.openOuter()) {
                long amountToExtract = Math.min(amount, pipe.remainingCapacity()) * FabricEntrypoint.FLUID_CONVERSION_RATE;
                if (predicate.test(pipe.getFluid())) {
                    extracted = fluidHandler.extract(FluidVariant.of(pipe.getFluid()), amountToExtract, transaction);
                }
                if (extracted <= 0 && pipe.isEmpty()) {
                    Iterator<StorageView<FluidVariant>> iterator = fluidHandler.nonEmptyIterator();
                    while (iterator.hasNext()) {
                        StorageView<FluidVariant> fluidStorage = iterator.next();
                        if (predicate.test(fluidStorage.getResource().getFluid())) {
                            extracted = fluidHandler.extract(fluidStorage.getResource(), amountToExtract, transaction);
                            if (extracted > 0) {
                                pipe.setFluid(fluidStorage.getResource().getFluid());
                                break;
                            }
                        }
                    }
                }
                if (extracted > 0) {
                    transaction.commit();
                }
            }
            if (extracted > 0) {
                pipe.insertFluidPacket(level, new FluidInPipe((int) (extracted / FabricEntrypoint.FLUID_CONVERSION_RATE), pipe.getTargetSpeed(), (short) 0, face.getOpposite(), face.getOpposite(), (short) 0));
                return true;
            }
        }
        return false;
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(FluidState fluidState, BlockAndTintGetter level, BlockPos pos) {
        FluidVariant fluidVariant = FluidVariant.of(fluidState.getType());
        int tint = FluidVariantRendering.getColor(fluidVariant, level, pos);
        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(fluidVariant);
        return new FluidRenderInfo(tint, sprite);
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(FluidState fluidState) {
        FluidVariant fluidVariant = FluidVariant.of(fluidState.getType());
        int tint = FluidVariantRendering.getColor(fluidVariant);
        TextureAtlasSprite sprite = FluidVariantRendering.getSprite(fluidVariant);
        return new FluidRenderInfo(tint, sprite);
    }

    @Override
    public Fluid getFluidFromStack(ItemStack stack) {
        Fluid fluid = null;
        Storage<FluidVariant> fluidHandler = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        if (fluidHandler != null) {
            Iterator<StorageView<FluidVariant>> iterator = fluidHandler.nonEmptyIterator();
            if (iterator.hasNext()) {
                fluid = iterator.next().getResource().getFluid();
            }
        } else if (stack.getItem() instanceof BucketItem bucket) {
            fluid = bucket.content;
        }
        return fluid != null && fluid.isSame(Fluids.EMPTY) ? null : fluid;
    }

    @Override
    public Component getFluidName(Fluid fluid) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }

    @Override
    public DispenseItemBehavior getDispenserBehaviour(ItemStack stack) {
        return DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem());
    }

}
