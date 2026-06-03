package jagm.classicpipes.services;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class NeoForgeService implements LoaderService {

    @Override
    public <B extends BlockEntity> BlockEntityType<B> createBlockEntityType(BiFunction<BlockPos, BlockState, B> blockEntitySupplier, Block... validBlocks) {
        return new BlockEntityType<>(blockEntitySupplier::apply, Set.of(validBlocks));
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(TriFunction<Integer, Inventory, D, M> menuSupplier, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        return IMenuTypeExtension.create((id, inventory, buffer) -> menuSupplier.apply(id, inventory, codec.decode(buffer)));
    }

    @Override
    public <M extends AbstractContainerMenu> MenuType<M> createSimpleMenuType(BiFunction<Integer, Inventory, M> menuSupplier) {
        return new MenuType<>(menuSupplier::apply, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    public <D> void openMenu(ServerPlayer player, MenuProvider menuProvider, D payload, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        player.openMenu(menuProvider, buffer -> codec.encode(buffer, payload));
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public boolean canAccessContainer(Level level, BlockPos containerPos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof ItemPipeEntity) {
            return false;
        }
        BlockState state = level.getBlockState(containerPos);
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, containerPos, state, blockEntity, face);
        if (itemHandler != null) {
            return true;
        } else {
            return MiscUtil.canAccessVanillaContainer(level, blockEntity, state, containerPos, face);
        }
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
        BlockState state = level.getBlockState(containerPos);
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, containerPos, state, blockEntity, face);
        if (itemHandler != null) {
            ItemStack stack = item.getStack();
            try (Transaction transaction = Transaction.open(null)) {
                for (int slot = 0; slot < itemHandler.size(); slot++) {
                    stack.shrink(itemHandler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction));
                    if (stack.isEmpty()) {
                        transaction.commit();
                        return true;
                    }
                }
                transaction.commit();
            }
            item.setStack(stack);
        } else {
            Container container = MiscUtil.getVanillaContainer(level, blockEntity, level.getBlockState(containerPos), containerPos);
            if (container != null) {
                ItemStack remaining = HopperBlockEntity.addItem(null, container, item.getStack(), face);
                if (remaining.isEmpty()) {
                    return true;
                }
                item.setStack(remaining);
            }
        }
        item.resetProgress(item.getTargetDirection());
        pipe.routeItem(pipeState, item);
        return false;
    }

    @Override
    public boolean handleItemExtraction(ItemPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<ItemStack> predicate) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof ItemPipeEntity) {
            return false;
        }
        BlockState state = level.getBlockState(containerPos);
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, containerPos, state, blockEntity, face);
        if (itemHandler != null) {
            try (Transaction transaction = Transaction.open(null)) {
                for (int slot = itemHandler.size() - 1; slot >= 0; slot--) {
                    ItemResource itemResource = itemHandler.getResource(slot);
                    if (!itemResource.isEmpty()) {
                        int amountToExtract = Math.min(amount, itemResource.getMaxStackSize());
                        if (predicate.test(itemResource.toStack(amountToExtract))) {
                            ItemStack extracted = itemResource.toStack(itemHandler.extract(slot, itemResource, amountToExtract, transaction));
                            if (!extracted.isEmpty()) {
                                pipe.setItem(face.getOpposite(), extracted);
                                transaction.commit();
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            Container container = MiscUtil.getVanillaContainer(level, blockEntity, level.getBlockState(containerPos), containerPos);
            if (container != null) {
                int[] slots = container instanceof WorldlyContainer worldlyContainer ? worldlyContainer.getSlotsForFace(face) : IntStream.range(0, container.getContainerSize()).toArray();
                for (int i = slots.length - 1; i >= 0; i--) {
                    int slot = slots[i];
                    ItemStack slotStack = container.getItem(slot);
                    int amountToTake = Math.min(slotStack.getCount(), amount);
                    ItemStack extracted = slotStack.copyWithCount(amountToTake);
                    if (predicate.test(slotStack) && !extracted.isEmpty() && MiscUtil.canTakeItemFromVanillaContainer(container, slot, extracted, face)) {
                        container.removeItem(slot, amountToTake);
                        container.setChanged();
                        pipe.setItem(face.getOpposite(), slotStack.copyWithCount(amountToTake));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean extractSpecificItem(ItemPipeEntity pipe, ServerLevel level, BlockPos containerPos, Direction face, ItemStack stack) {
        ItemStack target = stack.copy();
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, containerPos, face);
        if (itemHandler != null) {
            try (Transaction transaction = Transaction.open(null)) {
                for (int slot = itemHandler.size() - 1; slot >= 0; slot--) {
                    ItemResource itemResource = itemHandler.getResource(slot);
                    if (!itemResource.isEmpty() && itemResource.matches(target)) {
                        while (itemHandler.getAmountAsInt(slot) > 0) {
                            ItemStack extracted = itemResource.toStack(itemHandler.extract(slot, itemResource, target.getCount(), transaction));
                            if (!extracted.isEmpty()) {
                                target.shrink(extracted.getCount());
                                pipe.setItem(face.getOpposite(), extracted);
                                if (target.isEmpty()) {
                                    transaction.commit();
                                    return true;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            Container container = MiscUtil.getVanillaContainer(level, level.getBlockState(containerPos), containerPos);
            if (container != null) {
                int[] slots = container instanceof WorldlyContainer worldlyContainer ? worldlyContainer.getSlotsForFace(face) : IntStream.range(0, container.getContainerSize()).toArray();
                for (int i = slots.length - 1; i >= 0; i--) {
                    int slot = slots[i];
                    ItemStack slotStack = container.getItem(slot);
                    if (ItemStack.isSameItemSameComponents(target, slotStack)) {
                        int amountToTake = Math.min(slotStack.getCount(), target.getCount());
                        ItemStack extracted = slotStack.copyWithCount(amountToTake);
                        if (!extracted.isEmpty() && MiscUtil.canTakeItemFromVanillaContainer(container, slot, extracted, face)) {
                            container.removeItem(slot, amountToTake);
                            container.setChanged();
                            target.shrink(amountToTake);
                            pipe.setItem(face.getOpposite(), extracted);
                            if (target.isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<ItemStack> getExtractableContainerItems(ServerLevel level, BlockPos pos, Direction face) {
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, face);
        List<ItemStack> stacks = new ArrayList<>();
        if (itemHandler != null) {
            try (Transaction transaction = Transaction.open(null)) {
                for (int slot = itemHandler.size() - 1; slot >= 0; slot--) {
                    ItemResource itemResource = itemHandler.getResource(slot);
                    if (!itemResource.isEmpty()) {
                        int prevAmount = itemHandler.getAmountAsInt(slot);
                        int extractable = itemHandler.extract(slot, itemResource, prevAmount, transaction);
                        if (extractable >= itemResource.getMaxStackSize()) {
                            extractable = prevAmount;
                        }
                        MiscUtil.mergeStackIntoList(stacks, itemResource.toStack(extractable));
                    }
                }
            }
        } else {
            Container container = MiscUtil.getVanillaContainer(level, level.getBlockState(pos), pos);
            if (container != null) {
                int[] slots = container instanceof WorldlyContainer worldlyContainer ? worldlyContainer.getSlotsForFace(face) : IntStream.range(0, container.getContainerSize()).toArray();
                for (int i = slots.length - 1; i >= 0; i--) {
                    int slot = slots[i];
                    ItemStack extractable = container.getItem(slot);
                    if (MiscUtil.canTakeItemFromVanillaContainer(container, slot, extractable, face)) {
                        MiscUtil.mergeStackIntoList(stacks, extractable);
                    }
                }
            }
        }
        return stacks;
    }

    public List<ItemStack> getAllContainerItems(ServerLevel level, BlockPos pos, Direction face) {
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, face);
        List<ItemStack> stacks = new ArrayList<>();
        if (itemHandler != null) {
            for (int slot = itemHandler.size() - 1; slot >= 0; slot--) {
                MiscUtil.mergeStackIntoList(stacks, itemHandler.getResource(slot).toStack(itemHandler.getAmountAsInt(slot)));
            }
        } else {
            Container container = MiscUtil.getVanillaContainer(level, level.getBlockState(pos), pos);
            if (container != null) {
                int[] slots = container instanceof WorldlyContainer worldlyContainer ? worldlyContainer.getSlotsForFace(face) : IntStream.range(0, container.getContainerSize()).toArray();
                for (int i = slots.length - 1; i >= 0; i--) {
                    MiscUtil.mergeStackIntoList(stacks, container.getItem(slots[i]));
                }
            }
        }
        return stacks;
    }

    @Override
    public String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modId);
    }

    @Override
    public boolean handleFluidInsertion(FluidPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, BlockEntity containerEntity, BlockPos containerPos, Fluid fluid, Object fluidData, FluidInPipe fluidPacket) {
        Direction face = fluidPacket.getTargetDirection().getOpposite();
        BlockState state = level.getBlockState(containerPos);
        ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, containerPos, state, containerEntity, face);
        if (fluidHandler != null) {
            try (Transaction transaction = Transaction.open(null)) {
                FluidResource fluidResource = fluidData instanceof DataComponentPatch components ? FluidResource.of(fluid, components) : FluidResource.of(fluid);
                if (!fluidResource.isEmpty()) {
                    int amountFilled = fluidHandler.insert(fluidResource, fluidPacket.getAmount(), transaction);
                    transaction.commit();
                    if (amountFilled >= fluidPacket.getAmount()) {
                        return true;
                    }
                    fluidPacket.setAmount(fluidPacket.getAmount() - amountFilled);
                }
            }
        }
        return false;
    }

    @Override
    public boolean canAccessFluidContainer(Level level, BlockPos containerPos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof FluidPipeEntity) {
            return false;
        }
        return level.getCapability(Capabilities.Fluid.BLOCK, containerPos, level.getBlockState(containerPos), blockEntity, face) != null;
    }

    @Override
    public boolean handleFluidExtraction(FluidPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<Fluid> predicate) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof FluidPipeEntity || pipe.totalAmount() >= FluidPipeEntity.CAPACITY) {
            return false;
        }
        BlockState state = level.getBlockState(containerPos);
        ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, containerPos, state, blockEntity, face);
        if (fluidHandler != null) {
            int amountToDrain = Math.min(amount, pipe.remainingCapacity());
            FluidResource fluidResource = pipe.getFluidData() instanceof DataComponentPatch components ? FluidResource.of(pipe.getFluid(), components) : FluidResource.of(pipe.getFluid());
            boolean satisfiesPredicate = predicate.test(pipe.getFluid());
            if (pipe.isEmpty()) {
                for (int tank = 0; tank < fluidHandler.size(); tank++) {
                    FluidResource tankResource = fluidHandler.getResource(tank);
                    if (!tankResource.isEmpty() && predicate.test(tankResource.getFluid())) {
                        fluidResource = tankResource;
                        satisfiesPredicate = true;
                        break;
                    }
                }
            }
            if (satisfiesPredicate) {
                try (Transaction transaction = Transaction.open(null)) {
                    int amountExtracted = fluidHandler.extract(fluidResource, amountToDrain, transaction);
                    if (amountExtracted > 0) {
                        pipe.setFluid(fluidResource.getFluid(), fluidResource.getComponentsPatch());
                        pipe.insertFluidPacket(level, new FluidInPipe(amountExtracted, pipe.getTargetSpeed(), (short) 0, face.getOpposite(), face.getOpposite(), (short) 0));
                        transaction.commit();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Fluid getFluidFromStack(ItemStack stack) {
        Fluid fluid = null;
        ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
        if (fluidHandler != null) {
            fluid = fluidHandler.getResource(0).getFluid();
        } else if (stack.getItem() instanceof BucketItem bucket) {
            fluid = bucket.content;
        }
        return fluid != null && fluid.isSame(Fluids.EMPTY) ? null : fluid;
    }

    @Override
    public Component getFluidName(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }

    @Override
    public int getFluidTint(FluidModel fluidModel, Fluid fluid, Object fluidData, BlockAndTintGetter level, BlockPos pos) {
        if (fluidModel.fluidTintSource() != null) {
            if (fluidData instanceof DataComponentPatch components && !components.isEmpty()) {
                return fluidModel.fluidTintSource().colorAsStack(new FluidStack(fluid, 1, components));
            } else {
                return fluidModel.fluidTintSource().colorInWorld(Blocks.AIR.defaultBlockState(), level, pos);
            }
        } else {
            return -1;
        }
    }

}
