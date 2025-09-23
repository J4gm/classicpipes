package jagm.classicpipes.services;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class NeoForgeService implements LoaderService {

    @Override
    public <B extends BlockEntity> BlockEntityType<B> createBlockEntityType(BiFunction<BlockPos, BlockState, B> blockEntitySupplier, Block... validBlocks) {
        return BlockEntityType.Builder.of(blockEntitySupplier::apply, validBlocks).build(null);
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
        PacketDistributor.sendToServer(payload);
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
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, state, blockEntity, face);
        if (itemHandler != null) {
            return itemHandler.getSlots() > 0;
        }
        return false;
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
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, state, blockEntity, face);
        if (itemHandler != null) {
            ItemStack stack = item.getStack();
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                stack = itemHandler.insertItem(slot, stack, false);
                if (stack.isEmpty()) {
                    return true;
                }
            }
            item.setStack(stack);
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
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, state, blockEntity, face);
        if (itemHandler != null) {
            for (int slot = itemHandler.getSlots() - 1; slot >= 0; slot--) {
                if (predicate.test(itemHandler.getStackInSlot(slot))) {
                    ItemStack stack = itemHandler.extractItem(slot, amount, false);
                    if (!stack.isEmpty()) {
                        pipe.setItem(face.getOpposite(), stack);
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
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, containerPos, face);
        if (itemHandler != null) {
            for (int slot = itemHandler.getSlots() - 1; slot >= 0; slot--) {
                if (ItemStack.isSameItemSameComponents(target, itemHandler.getStackInSlot(slot))) {
                    ItemStack extracted = itemHandler.extractItem(slot, target.getCount(), false);
                    if (!extracted.isEmpty()) {
                        target.shrink(extracted.getCount());
                        pipe.setItem(face.getOpposite(), extracted);
                        if (target.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<ItemStack> getContainerItems(ServerLevel level, BlockPos pos, Direction face) {
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face);
        if (itemHandler != null) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                ItemStack slotStack = itemHandler.getStackInSlot(slot);
                if (slotStack.isEmpty()) {
                    continue;
                }
                boolean matched = false;
                for (ItemStack stack : stacks) {
                    if (ItemStack.isSameItemSameComponents(stack, slotStack)) {
                        stack.setCount(stack.getCount() + slotStack.getCount());
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    stacks.add(slotStack.copy());
                }
            }
            return stacks;
        }
        return List.of();
    }

    @Override
    public String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modId);
    }

    @Override
    public boolean handleFluidInsertion(FluidPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, BlockEntity containerEntity, BlockPos containerPos, Fluid fluid, FluidInPipe fluidPacket) {
        Direction face = fluidPacket.getTargetDirection().getOpposite();
        BlockState state = level.getBlockState(containerPos);
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, containerPos, state, containerEntity, face);
        if (fluidHandler != null) {
            int amountFilled = fluidHandler.fill(new FluidStack(fluid, fluidPacket.getAmount()), IFluidHandler.FluidAction.EXECUTE);
            if (amountFilled >= fluidPacket.getAmount()) {
                return true;
            }
            fluidPacket.setAmount(fluidPacket.getAmount() - amountFilled);
        }
        return false;
    }

    @Override
    public boolean canAccessFluidContainer(Level level, BlockPos containerPos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof FluidPipeEntity) {
            return false;
        }
        BlockState state = level.getBlockState(containerPos);
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, containerPos, state, blockEntity, face);
        if (fluidHandler != null) {
            return fluidHandler.getTanks() > 0;
        }
        return false;
    }

    @Override
    public boolean handleFluidExtraction(FluidPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<Fluid> predicate) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof FluidPipeEntity || pipe.totalAmount() >= FluidPipeEntity.CAPACITY) {
            return false;
        }
        BlockState state = level.getBlockState(containerPos);
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, containerPos, state, blockEntity, face);
        if (fluidHandler != null) {
            int amountToDrain = Math.min(amount, pipe.remainingCapacity());
            Fluid fluid = pipe.isEmpty() ? fluidHandler.drain(amountToDrain, IFluidHandler.FluidAction.SIMULATE).getFluid() : pipe.getFluid();
            if (predicate.test(fluid)) {
                FluidStack drainedStack = pipe.isEmpty() ? fluidHandler.drain(amountToDrain, IFluidHandler.FluidAction.EXECUTE) : fluidHandler.drain(new FluidStack(pipe.getFluid(), amountToDrain), IFluidHandler.FluidAction.EXECUTE);
                if (!drainedStack.isEmpty()) {
                    pipe.setFluid(drainedStack.getFluid());
                    pipe.insertFluidPacket(level, new FluidInPipe(drainedStack.getAmount(), pipe.getTargetSpeed(), (short) 0, face.getOpposite(), face.getOpposite(), (short) 0));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(FluidState fluidState, BlockAndTintGetter level, BlockPos pos) {
        IClientFluidTypeExtensions fluidInfo = IClientFluidTypeExtensions.of(fluidState);
        int tint = fluidInfo.getTintColor(fluidState, level, pos);
        ResourceLocation texture = fluidInfo.getStillTexture(fluidState, level, pos);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        return new FluidRenderInfo(tint, sprite);
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(FluidState fluidState) {
        IClientFluidTypeExtensions fluidInfo = IClientFluidTypeExtensions.of(fluidState);
        int tint = fluidInfo.getTintColor();
        ResourceLocation texture = fluidInfo.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        return new FluidRenderInfo(tint, sprite);
    }

    @Override
    public Fluid getFluidFromStack(ItemStack stack) {
        Fluid fluid = null;
        IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler != null) {
            fluid = fluidHandler.getFluidInTank(0).getFluid();
        } else if (stack.getItem() instanceof BucketItem bucket) {
            fluid = bucket.content;
        }
        return fluid != null && fluid.isSame(Fluids.EMPTY) ? null : fluid;
    }

    @Override
    public Component getFluidName(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }

}
