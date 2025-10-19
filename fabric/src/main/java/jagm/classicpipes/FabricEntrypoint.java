package jagm.classicpipes;

import jagm.classicpipes.blockentity.FabricFluidPipeWrapper;
import jagm.classicpipes.blockentity.FabricItemPipeWrapper;
import jagm.classicpipes.network.*;
import jagm.classicpipes.util.MiscUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@SuppressWarnings("unused")
public class FabricEntrypoint implements ModInitializer {

    public static final long FLUID_CONVERSION_RATE = FluidConstants.BUCKET / 1000;

    @Override
    public void onInitialize() {

        ClassicPipes.ITEMS.forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, MiscUtil.resourceLocation(name), item));
        ClassicPipes.BLOCKS.forEach((name, block) -> Registry.register(BuiltInRegistries.BLOCK, MiscUtil.resourceLocation(name), block));
        ClassicPipes.SOUNDS.forEach((name, soundEvent) -> Registry.register(BuiltInRegistries.SOUND_EVENT, MiscUtil.resourceLocation(name), soundEvent));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ClassicPipes.PIPES_TAB_KEY, ClassicPipes.PIPES_TAB);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ClassicPipes.LABEL_COMPONENT_KEY, ClassicPipes.LABEL_COMPONENT);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, MiscUtil.resourceLocation("request_item"), ClassicPipes.REQUEST_ITEM_TRIGGER);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, ClassicPipes.ITEMS_REQUESTED_STAT, ClassicPipes.ITEMS_REQUESTED_STAT);
        ClassicPipes.createStats();

        registerBlockEntity("basic_pipe", ClassicPipes.BASIC_PIPE_ENTITY);
        registerBlockEntity("golden_pipe", ClassicPipes.GOLDEN_PIPE_ENTITY);
        registerBlockEntity("copper_pipe", ClassicPipes.COPPER_PIPE_ENTITY);
        registerBlockEntity("iron_pipe", ClassicPipes.IRON_PIPE_ENTITY);
        registerBlockEntity("diamond_pipe", ClassicPipes.DIAMOND_PIPE_ENTITY);
        registerBlockEntity("flint_pipe", ClassicPipes.FLINT_PIPE_ENTITY);
        registerBlockEntity("lapis_pipe", ClassicPipes.LAPIS_PIPE_ENTITY);
        registerBlockEntity("obsidian_pipe", ClassicPipes.OBSIDIAN_PIPE_ENTITY);
        registerBlockEntity("bone_pipe", ClassicPipes.BONE_PIPE_ENTITY);
        registerBlockEntity("routing_pipe", ClassicPipes.ROUTING_PIPE_ENTITY);
        registerBlockEntity("provider_pipe", ClassicPipes.PROVIDER_PIPE_ENTITY);
        registerBlockEntity("request_pipe", ClassicPipes.REQUEST_PIPE_ENTITY);
        registerBlockEntity("stocking_pipe", ClassicPipes.STOCKING_PIPE_ENTITY);
        registerBlockEntity("matching_pipe", ClassicPipes.MATCHING_PIPE_ENTITY);
        registerBlockEntity("storage_pipe", ClassicPipes.STORAGE_PIPE_ENTITY);
        registerBlockEntity("recipe_pipe", ClassicPipes.RECIPE_PIPE_ENTITY);
        registerBlockEntity("fluid_pipe", ClassicPipes.FLUID_PIPE_ENTITY);
        registerBlockEntity("copper_fluid_pipe", ClassicPipes.COPPER_FLUID_PIPE_ENTITY);
        registerBlockEntity("iron_fluid_pipe", ClassicPipes.IRON_FLUID_PIPE_ENTITY);
        registerBlockEntity("lapis_fluid_pipe", ClassicPipes.LAPIS_FLUID_PIPE_ENTITY);
        registerBlockEntity("diamond_fluid_pipe", ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY);
        registerBlockEntity("obsidian_fluid_pipe", ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY);
        registerBlockEntity("advanced_copper_pipe", ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY);
        registerBlockEntity("advanced_copper_fluid_pipe", ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY);

        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.BASIC_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.GOLDEN_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.COPPER_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.IRON_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.DIAMOND_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.FLINT_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.LAPIS_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.OBSIDIAN_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.BONE_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.ROUTING_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.PROVIDER_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.REQUEST_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.STOCKING_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.MATCHING_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.STORAGE_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.RECIPE_PIPE_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(FabricItemPipeWrapper::new, ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.FLUID_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.COPPER_FLUID_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.IRON_FLUID_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.LAPIS_FLUID_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(FabricFluidPipeWrapper::new, ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY);

        registerMenu("diamond_pipe", ClassicPipes.DIAMOND_PIPE_MENU);
        registerMenu("routing_pipe", ClassicPipes.ROUTING_PIPE_MENU);
        registerMenu("provider_pipe", ClassicPipes.PROVIDER_PIPE_MENU);
        registerMenu("request", ClassicPipes.REQUEST_MENU);
        registerMenu("stocking_pipe", ClassicPipes.STOCKING_PIPE_MENU);
        registerMenu("matching_pipe", ClassicPipes.MATCHING_PIPE_MENU);
        registerMenu("storage_pipe", ClassicPipes.STORAGE_PIPE_MENU);
        registerMenu("recipe_pipe", ClassicPipes.RECIPE_PIPE_MENU);
        registerMenu("diamond_fluid_pipe", ClassicPipes.DIAMOND_FLUID_PIPE_MENU);
        registerMenu("advanced_copper_pipe", ClassicPipes.ADVANCED_COPPER_PIPE_MENU);
        registerMenu("advanced_copper_fluid_pipe", ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU);

        ItemGroupEvents.modifyEntriesEvent(ClassicPipes.PIPES_TAB_KEY).register(tab -> ClassicPipes.ITEMS.forEach((name, item) -> tab.accept(item)));

        registerServerPayload(ServerBoundMatchComponentsPayload.TYPE, ServerBoundMatchComponentsPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundDefaultRoutePayload.TYPE, ServerBoundDefaultRoutePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundLeaveOnePayload.TYPE, ServerBoundLeaveOnePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundSortingModePayload.TYPE, ServerBoundSortingModePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundRequestPayload.TYPE, ServerBoundRequestPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundActiveStockingPayload.TYPE, ServerBoundActiveStockingPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundSlotDirectionPayload.TYPE, ServerBoundSlotDirectionPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundTransferRecipePayload.TYPE, ServerBoundTransferRecipePayload.STREAM_CODEC);
        registerServerPayload(ServerBoundSetFilterPayload.TYPE, ServerBoundSetFilterPayload.STREAM_CODEC);
        registerServerPayload(ServerBoundBlockingModePayload.TYPE, ServerBoundBlockingModePayload.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(ClientBoundItemListPayload.TYPE, ClientBoundItemListPayload.STREAM_CODEC);

    }

    private static <T extends BlockEntity> void registerBlockEntity(String name, BlockEntityType<T> blockEntityType) {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MiscUtil.resourceLocation(name), blockEntityType);
    }

    private static <T extends AbstractContainerMenu> void registerMenu(String name, MenuType<T> menuType) {
        Registry.register(BuiltInRegistries.MENU, MiscUtil.resourceLocation(name), menuType);
    }

    private static <T extends SelfHandler> void registerServerPayload(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> payload.handle(context.player()));
    }

}
