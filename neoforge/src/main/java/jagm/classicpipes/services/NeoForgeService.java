package jagm.classicpipes.services;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.renderer.FluidRenderInfo;
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
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
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
            return itemHandler.size() > 0;
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
                        ItemStack extracted = itemResource.toStack(itemHandler.extract(slot, itemResource, target.getCount(), transaction));
                        if (!extracted.isEmpty()) {
                            target.shrink(extracted.getCount());
                            pipe.setItem(face.getOpposite(), extracted);
                            if (target.isEmpty()) {
                                transaction.commit();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<ItemStack> getContainerItems(ServerLevel level, BlockPos pos, Direction face) {
        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, face);
        if (itemHandler != null) {
            List<ItemStack> stacks = new ArrayList<>();
            for (int slot = 0; slot < itemHandler.size(); slot++) {
                ItemStack slotStack = itemHandler.getResource(slot).toStack(itemHandler.getAmountAsInt(slot));
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
        ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, containerPos, state, containerEntity, face);
        if (fluidHandler != null) {
            try (Transaction transaction = Transaction.open(null)) {
                int amountFilled = fluidHandler.insert(FluidResource.of(fluid), fluidPacket.getAmount(), transaction);
                transaction.commit();
                if (amountFilled >= fluidPacket.getAmount()) {
                    return true;
                }
                fluidPacket.setAmount(fluidPacket.getAmount() - amountFilled);
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
        BlockState state = level.getBlockState(containerPos);
        ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, containerPos, state, blockEntity, face);
        if (fluidHandler != null) {
            return fluidHandler.size() > 0;
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
        ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, containerPos, state, blockEntity, face);
        if (fluidHandler != null) {
            int amountToDrain = Math.min(amount, pipe.remainingCapacity());
            Fluid fluid = pipe.getFluid() != null ? pipe.getFluid() : Fluids.WATER;
            if (pipe.isEmpty()) {
                for (int tank = 0; tank < fluidHandler.size(); tank++) {
                    FluidResource fluidResource = fluidHandler.getResource(tank);
                    if (!fluidResource.isEmpty() && predicate.test(fluidResource.getFluid())) {
                        fluid = fluidResource.getFluid();
                        break;
                    }
                }
            }
            try (Transaction transaction = Transaction.open(null)) {
                int amountExtracted = fluidHandler.extract(FluidResource.of(fluid), amountToDrain, transaction);
                if (amountExtracted > 0) {
                    pipe.setFluid(fluid);
                    pipe.insertFluidPacket(level, new FluidInPipe(amountExtracted, pipe.getTargetSpeed(state, face.getOpposite(), face.getOpposite()), (short) 0, face.getOpposite(), face.getOpposite(), (short) 0));
                    transaction.commit();
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

}
