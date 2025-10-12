package jagm.classicpipes.services;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.network.ForgeClientPacketHandler;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
import jagm.classicpipes.network.ForgeServerPacketHandler;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class ForgeService implements LoaderService {

    @Override
    public <B extends BlockEntity> BlockEntityType<B> createBlockEntityType(BiFunction<BlockPos, BlockState, B> blockEntitySupplier, Block... validBlocks) {
        return new BlockEntityType<>(blockEntitySupplier::apply, Set.of(validBlocks));
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> createMenuType(TriFunction<Integer, Inventory, D, M> menuSupplier, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        return IForgeMenuType.create((id, inventory, buffer) -> menuSupplier.apply(id, inventory, codec.decode(new RegistryFriendlyByteBuf(buffer, inventory.player.registryAccess()))));
    }

    @Override
    public <M extends AbstractContainerMenu> MenuType<M> createSimpleMenuType(BiFunction<Integer, Inventory, M> menuSupplier) {
        return new MenuType<>(menuSupplier::apply, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    public <D> void openMenu(ServerPlayer player, MenuProvider menuProvider, D payload, StreamCodec<RegistryFriendlyByteBuf, D> codec) {
        player.openMenu(menuProvider, buffer -> codec.encode(new RegistryFriendlyByteBuf(buffer, player.registryAccess()), payload));
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ForgeClientPacketHandler.sendToServer(payload);
    }

    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        ForgeServerPacketHandler.sendToClient(player, payload);
    }

    @Override
    public boolean canAccessContainer(Level level, BlockPos containerPos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof ItemPipeEntity) {
            return false;
        }
        if (blockEntity != null) {
            Optional<IItemHandler> itemHandlerOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve();
            if (itemHandlerOptional.isPresent()) {
                return itemHandlerOptional.get().getSlots() > 0;
            }
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
        if (blockEntity != null) {
            Optional<IItemHandler> itemHandlerOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve();
            if (itemHandlerOptional.isPresent()) {
                IItemHandler itemHandler = itemHandlerOptional.get();
                ItemStack stack = item.getStack();
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    stack = itemHandler.insertItem(slot, stack, false);
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
                item.setStack(stack);
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
        if (blockEntity != null) {
            Optional<IItemHandler> itemHandlerOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve();
            if (itemHandlerOptional.isPresent()) {
                IItemHandler itemHandler = itemHandlerOptional.get();
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
        }
        return false;
    }

    public List<ItemStack> getContainerItems(ServerLevel level, BlockPos pos, Direction face) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            Optional<IItemHandler> itemHandlerOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve();
            if (itemHandlerOptional.isPresent()) {
                IItemHandler itemHandler = itemHandlerOptional.get();
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
        }
        return List.of();
    }

    @Override
    public boolean extractSpecificItem(ItemPipeEntity pipe, ServerLevel level, BlockPos containerPos, Direction face, ItemStack stack) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity != null) {
            ItemStack target = stack.copy();
            Optional<IItemHandler> itemHandlerOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, face).resolve();
            if (itemHandlerOptional.isPresent()) {
                IItemHandler itemHandler = itemHandlerOptional.get();
                for (int slot = itemHandler.getSlots() - 1; slot >= 0; slot--) {
                    if (ItemStack.isSameItemSameComponents(stack, itemHandler.getStackInSlot(slot))) {
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
        }
        return false;
    }

    @Override
    public String getModName(String modId) {
        return ModList.get().getModContainerById(modId).map(ModContainer::getModInfo).map(IModInfo::getDisplayName).orElse(modId);
    }

    @Override
    public boolean handleFluidInsertion(FluidPipeEntity pipe, ServerLevel level, BlockPos pipePos, BlockState pipeState, BlockEntity containerEntity, BlockPos containerPos, Fluid fluid, FluidInPipe fluidPacket) {
        Direction face = fluidPacket.getTargetDirection().getOpposite();
        Optional<IFluidHandler> fluidHandlerOptional = containerEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, face).resolve();
        if (fluidHandlerOptional.isPresent()) {
            IFluidHandler fluidHandler = fluidHandlerOptional.get();
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
        } else if (blockEntity != null) {
            Optional<IFluidHandler> fluidHandlerOptional = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, face).resolve();
            if (fluidHandlerOptional.isPresent()) {
                IFluidHandler fluidHandler = fluidHandlerOptional.get();
                return fluidHandler.getTanks() > 0;
            }
        }
        return false;
    }

    @Override
    public boolean handleFluidExtraction(FluidPipeEntity pipe, BlockState pipeState, ServerLevel level, BlockPos containerPos, Direction face, int amount, Predicate<Fluid> predicate) {
        BlockEntity blockEntity = level.getBlockEntity(containerPos);
        if (blockEntity instanceof FluidPipeEntity || pipe.totalAmount() >= FluidPipeEntity.CAPACITY) {
            return false;
        } else if (blockEntity != null) {
            Optional<IFluidHandler> fluidHandlerOptional = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, face).resolve();
            if (fluidHandlerOptional.isPresent()) {
                IFluidHandler fluidHandler = fluidHandlerOptional.get();
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
        }
        return false;
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(FluidState fluidState, BlockAndTintGetter level, BlockPos pos) {
        IClientFluidTypeExtensions fluidInfo = IClientFluidTypeExtensions.of(fluidState);
        int tint = fluidInfo.getTintColor(fluidState, level, pos);
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (texture instanceof TextureAtlas blockAtlas) {
            return new FluidRenderInfo(tint, blockAtlas.getSprite(fluidInfo.getStillTexture(fluidState, level, pos)));
        }
        return new FluidRenderInfo(tint, null);
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(FluidState fluidState) {
        IClientFluidTypeExtensions fluidInfo = IClientFluidTypeExtensions.of(fluidState);
        int tint = fluidInfo.getTintColor();
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (texture instanceof TextureAtlas blockAtlas) {
            return new FluidRenderInfo(tint, blockAtlas.getSprite(fluidInfo.getStillTexture()));
        }
        return new FluidRenderInfo(tint, null);
    }

    @Override
    public Fluid getFluidFromStack(ItemStack stack) {
        Fluid fluid = null;
        Optional<IFluidHandler> fluidHandlerOptional = stack.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve();
        if (fluidHandlerOptional.isPresent()) {
            fluid = fluidHandlerOptional.get().getFluidInTank(0).getFluid();
        } else if (stack.getItem() instanceof BucketItem bucket) {
            fluid = bucket.getFluid();
        }
        return fluid != null && fluid.isSame(Fluids.EMPTY) ? null : fluid;
    }

    @Override
    public Component getFluidName(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }

}
