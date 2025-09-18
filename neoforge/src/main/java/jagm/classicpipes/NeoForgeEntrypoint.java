package jagm.classicpipes;

import jagm.classicpipes.blockentity.NeoForgeFluidPipeWrapper;
import jagm.classicpipes.blockentity.NeoForgeItemPipeWrapper;
import jagm.classicpipes.client.renderer.FluidPipeRenderer;
import jagm.classicpipes.client.renderer.PipeRenderer;
import jagm.classicpipes.client.renderer.RecipePipeRenderer;
import jagm.classicpipes.client.screen.*;
import jagm.classicpipes.network.*;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(ClassicPipes.MOD_ID)
@SuppressWarnings("unused")
public class NeoForgeEntrypoint {

    @EventBusSubscriber(modid = ClassicPipes.MOD_ID)
    public static class ModEventHandler {

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {

            event.register(Registries.BLOCK, helper -> ClassicPipes.BLOCKS.forEach((name, block) -> helper.register(MiscUtil.resourceLocation(name), block)));
            event.register(Registries.ITEM, helper -> ClassicPipes.ITEMS.forEach((name, item) -> helper.register(MiscUtil.resourceLocation(name), item)));
            event.register(Registries.SOUND_EVENT, helper -> ClassicPipes.SOUNDS.forEach((name, soundEvent) -> helper.register(MiscUtil.resourceLocation(name), soundEvent)));
            event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ClassicPipes.PIPES_TAB_KEY, ClassicPipes.PIPES_TAB));
            event.register(Registries.DATA_COMPONENT_TYPE, helper -> helper.register(ClassicPipes.LABEL_COMPONENT_KEY, ClassicPipes.LABEL_COMPONENT));
            event.register(Registries.TRIGGER_TYPE, helper -> helper.register(MiscUtil.resourceLocation("request_item"), ClassicPipes.REQUEST_ITEM_TRIGGER));

            event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
                helper.register(MiscUtil.resourceLocation("basic_pipe"), ClassicPipes.BASIC_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("golden_pipe"), ClassicPipes.GOLDEN_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("copper_pipe"), ClassicPipes.COPPER_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("iron_pipe"), ClassicPipes.IRON_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("diamond_pipe"), ClassicPipes.DIAMOND_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("flint_pipe"), ClassicPipes.FLINT_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("lapis_pipe"), ClassicPipes.LAPIS_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("obsidian_pipe"), ClassicPipes.OBSIDIAN_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("bone_pipe"), ClassicPipes.BONE_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("routing_pipe"), ClassicPipes.ROUTING_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("provider_pipe"), ClassicPipes.PROVIDER_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("request_pipe"), ClassicPipes.REQUEST_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("stocking_pipe"), ClassicPipes.STOCKING_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("matching_pipe"), ClassicPipes.MATCHING_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("storage_pipe"), ClassicPipes.STORAGE_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("recipe_pipe"), ClassicPipes.RECIPE_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("fluid_pipe"), ClassicPipes.FLUID_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("copper_fluid_pipe"), ClassicPipes.COPPER_FLUID_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("iron_fluid_pipe"), ClassicPipes.IRON_FLUID_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("lapis_fluid_pipe"), ClassicPipes.LAPIS_FLUID_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("diamond_fluid_pipe"), ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("obsidian_fluid_pipe"), ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("advanced_copper_pipe"), ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY);
                helper.register(MiscUtil.resourceLocation("advanced_copper_fluid_pipe"), ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY);
            });

            event.register(Registries.MENU, helper -> {
                helper.register(MiscUtil.resourceLocation("diamond_pipe"), ClassicPipes.DIAMOND_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("routing_pipe"), ClassicPipes.ROUTING_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("provider_pipe"), ClassicPipes.PROVIDER_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("request"), ClassicPipes.REQUEST_MENU);
                helper.register(MiscUtil.resourceLocation("stocking_pipe"), ClassicPipes.STOCKING_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("matching_pipe"), ClassicPipes.MATCHING_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("storage_pipe"), ClassicPipes.STORAGE_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("recipe_pipe"), ClassicPipes.RECIPE_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("diamond_fluid_pipe"), ClassicPipes.DIAMOND_FLUID_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("advanced_copper_pipe"), ClassicPipes.ADVANCED_COPPER_PIPE_MENU);
                helper.register(MiscUtil.resourceLocation("advanced_copper_fluid_pipe"), ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU);
            });

        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.BASIC_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.GOLDEN_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.COPPER_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.IRON_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.DIAMOND_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.FLINT_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.LAPIS_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.OBSIDIAN_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.BONE_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.ROUTING_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.PROVIDER_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.REQUEST_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.STOCKING_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.MATCHING_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.STORAGE_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.RECIPE_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY, NeoForgeItemPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.COPPER_FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.IRON_FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.LAPIS_FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY, NeoForgeFluidPipeWrapper::new);
        }

        @SubscribeEvent
        public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registerServerPayload(registrar, ServerBoundMatchComponentsPayload.TYPE, ServerBoundMatchComponentsPayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundDefaultRoutePayload.TYPE, ServerBoundDefaultRoutePayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundLeaveOnePayload.TYPE, ServerBoundLeaveOnePayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundSortingModePayload.TYPE, ServerBoundSortingModePayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundRequestPayload.TYPE, ServerBoundRequestPayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundActiveStockingPayload.TYPE, ServerBoundActiveStockingPayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundSlotDirectionPayload.TYPE, ServerBoundSlotDirectionPayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundTransferRecipePayload.TYPE, ServerBoundTransferRecipePayload.STREAM_CODEC);
            registerServerPayload(registrar, ServerBoundSetFilterPayload.TYPE, ServerBoundSetFilterPayload.STREAM_CODEC);
            registerClientPayload(registrar, ClientBoundItemListPayload.TYPE, ClientBoundItemListPayload.STREAM_CODEC);
        }

        private static <T extends SelfHandler> void registerServerPayload(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            registrar.playToServer(type, codec, (payload, context) -> context.enqueueWork(() -> payload.handle(context.player())));
        }

        private static <T extends SelfHandler> void registerClientPayload(PayloadRegistrar registrar, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            registrar.playToClient(type, codec, (payload, context) -> context.enqueueWork(() -> payload.handle(context.player())));
        }

    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = ClassicPipes.MOD_ID)
    public static class ClientModEventHandler {

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ClassicPipes.BASIC_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.GOLDEN_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.COPPER_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.IRON_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.DIAMOND_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.FLINT_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.LAPIS_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.OBSIDIAN_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.BONE_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.ROUTING_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.PROVIDER_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.REQUEST_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.STOCKING_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.MATCHING_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.STORAGE_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.RECIPE_PIPE_ENTITY, RecipePipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.COPPER_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.IRON_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.LAPIS_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY, PipeRenderer::new);
            event.registerBlockEntityRenderer(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        }

        @SubscribeEvent
        public static void onFillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == ClassicPipes.PIPES_TAB_KEY) {
                ClassicPipes.ITEMS.forEach((name, item) -> event.accept(item));
            }
        }

        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ClassicPipes.DIAMOND_PIPE_MENU, DiamondPipeScreen::new);
            event.register(ClassicPipes.ROUTING_PIPE_MENU, RoutingPipeScreen::new);
            event.register(ClassicPipes.PROVIDER_PIPE_MENU, ProviderPipeScreen::new);
            event.register(ClassicPipes.REQUEST_MENU, RequestScreen::new);
            event.register(ClassicPipes.STOCKING_PIPE_MENU, StockingPipeScreen::new);
            event.register(ClassicPipes.MATCHING_PIPE_MENU, MatchingPipeScreen::new);
            event.register(ClassicPipes.STORAGE_PIPE_MENU, StoragePipeScreen::new);
            event.register(ClassicPipes.RECIPE_PIPE_MENU, RecipePipeScreen::new);
            event.register(ClassicPipes.DIAMOND_FLUID_PIPE_MENU, DiamondFluidPipeScreen::new);
            event.register(ClassicPipes.ADVANCED_COPPER_PIPE_MENU, AdvancedCopperPipeScreen::new);
            event.register(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU, AdvancedCopperFluidPipeScreen::new);
        }

    }

}
