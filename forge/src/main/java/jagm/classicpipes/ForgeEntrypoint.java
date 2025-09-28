package jagm.classicpipes;

import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.blockentity.ForgeFluidPipeWrapper;
import jagm.classicpipes.blockentity.ForgeItemPipeWrapper;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.client.renderer.FluidPipeRenderer;
import jagm.classicpipes.client.renderer.PipeRenderer;
import jagm.classicpipes.client.renderer.RecipePipeRenderer;
import jagm.classicpipes.client.screen.*;
import jagm.classicpipes.network.*;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

@Mod(ClassicPipes.MOD_ID)
@SuppressWarnings("unused")
public class ForgeEntrypoint {

    @Mod.EventBusSubscriber(modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventHandler {

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {

            event.register(ForgeRegistries.Keys.BLOCKS, helper -> ClassicPipes.BLOCKS.forEach(helper::register));
            event.register(ForgeRegistries.Keys.ITEMS, helper -> ClassicPipes.ITEMS.forEach(helper::register));
            event.register(ForgeRegistries.Keys.SOUND_EVENTS, helper -> ClassicPipes.SOUNDS.forEach(helper::register));
            event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ClassicPipes.PIPES_TAB_KEY, ClassicPipes.PIPES_TAB));
            event.register(Registries.CUSTOM_STAT, helper -> helper.register(ClassicPipes.ITEMS_REQUESTED_STAT, ClassicPipes.ITEMS_REQUESTED_STAT));

            event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, helper -> {
                helper.register("basic_pipe", ClassicPipes.BASIC_PIPE_ENTITY);
                helper.register("golden_pipe", ClassicPipes.GOLDEN_PIPE_ENTITY);
                helper.register("copper_pipe", ClassicPipes.COPPER_PIPE_ENTITY);
                helper.register("iron_pipe", ClassicPipes.IRON_PIPE_ENTITY);
                helper.register("diamond_pipe", ClassicPipes.DIAMOND_PIPE_ENTITY);
                helper.register("flint_pipe", ClassicPipes.FLINT_PIPE_ENTITY);
                helper.register("lapis_pipe", ClassicPipes.LAPIS_PIPE_ENTITY);
                helper.register("obsidian_pipe", ClassicPipes.OBSIDIAN_PIPE_ENTITY);
                helper.register("bone_pipe", ClassicPipes.BONE_PIPE_ENTITY);
                helper.register("routing_pipe", ClassicPipes.ROUTING_PIPE_ENTITY);
                helper.register("provider_pipe", ClassicPipes.PROVIDER_PIPE_ENTITY);
                helper.register("request_pipe", ClassicPipes.REQUEST_PIPE_ENTITY);
                helper.register("stocking_pipe", ClassicPipes.STOCKING_PIPE_ENTITY);
                helper.register("matching_pipe", ClassicPipes.MATCHING_PIPE_ENTITY);
                helper.register("storage_pipe", ClassicPipes.STORAGE_PIPE_ENTITY);
                helper.register("recipe_pipe", ClassicPipes.RECIPE_PIPE_ENTITY);
                helper.register("fluid_pipe", ClassicPipes.FLUID_PIPE_ENTITY);
                helper.register("copper_fluid_pipe", ClassicPipes.COPPER_FLUID_PIPE_ENTITY);
                helper.register("iron_fluid_pipe", ClassicPipes.IRON_FLUID_PIPE_ENTITY);
                helper.register("lapis_fluid_pipe", ClassicPipes.LAPIS_FLUID_PIPE_ENTITY);
                helper.register("diamond_fluid_pipe", ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY);
                helper.register("obsidian_fluid_pipe", ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY);
                helper.register("advanced_copper_pipe", ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY);
                helper.register("advanced_copper_fluid_pipe", ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY);
            });

            event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
                helper.register("diamond_pipe", ClassicPipes.DIAMOND_PIPE_MENU);
                helper.register("routing_pipe", ClassicPipes.ROUTING_PIPE_MENU);
                helper.register("provider_pipe", ClassicPipes.PROVIDER_PIPE_MENU);
                helper.register("request", ClassicPipes.REQUEST_MENU);
                helper.register("stocking_pipe", ClassicPipes.STOCKING_PIPE_MENU);
                helper.register("matching_pipe", ClassicPipes.MATCHING_PIPE_MENU);
                helper.register("storage_pipe", ClassicPipes.STORAGE_PIPE_MENU);
                helper.register("recipe_pipe", ClassicPipes.RECIPE_PIPE_MENU);
                helper.register("diamond_fluid_pipe", ClassicPipes.DIAMOND_FLUID_PIPE_MENU);
                helper.register("advanced_copper_pipe", ClassicPipes.ADVANCED_COPPER_PIPE_MENU);
                helper.register("advanced_copper_fluid_pipe", ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU);
            });

        }

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                CriteriaTriggers.register(ClassicPipes.REQUEST_ITEM_TRIGGER);
                ForgePacketHandler.registerServerPayload(ServerBoundMatchComponentsPayload.class, ServerBoundMatchComponentsPayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundDefaultRoutePayload.class, ServerBoundDefaultRoutePayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundLeaveOnePayload.class, ServerBoundLeaveOnePayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundSortingModePayload.class, ServerBoundSortingModePayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundRequestPayload.class, ServerBoundRequestPayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundActiveStockingPayload.class, ServerBoundActiveStockingPayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundSlotDirectionPayload.class, ServerBoundSlotDirectionPayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundTransferRecipePayload.class, ServerBoundTransferRecipePayload.HANDLER);
                ForgePacketHandler.registerServerPayload(ServerBoundSetFilterPayload.class, ServerBoundSetFilterPayload.HANDLER);
                ForgePacketHandler.registerClientPayload(ClientBoundItemListPayload.class, ClientBoundItemListPayload.HANDLER);
                ClassicPipes.createStats();
            });
        }

    }

    @Mod.EventBusSubscriber(modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEventHandler {

        @SubscribeEvent
        public static void onRegisterCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            if (event.getObject() instanceof ItemPipeEntity pipe) {
                Map<Direction, LazyOptional<IItemHandler>> wrapperForSide = new HashMap<>();
                for (Direction side : Direction.values()) {
                    wrapperForSide.put(side, LazyOptional.of(() -> new ForgeItemPipeWrapper(pipe, side)));
                }
                event.addCapability(MiscUtil.resourceLocation("item_pipe"), new ICapabilityProvider() {
                    @Override
                    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
                            return wrapperForSide.get(side).cast();
                        }
                        return LazyOptional.empty();
                    }
                });
                event.addListener(() -> wrapperForSide.forEach((direction, lazyOptional) -> lazyOptional.invalidate()));
            } else if (event.getObject() instanceof FluidPipeEntity pipe) {
                Map<Direction, LazyOptional<IFluidHandler>> wrapperForSide = new HashMap<>();
                for (Direction side : Direction.values()) {
                    wrapperForSide.put(side, LazyOptional.of(() -> new ForgeFluidPipeWrapper(pipe, side)));
                }
                event.addCapability(MiscUtil.resourceLocation("fluid_pipe"), new ICapabilityProvider() {
                    @Override
                    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
                            return wrapperForSide.get(side).cast();
                        }
                        return LazyOptional.empty();
                    }
                });
                event.addListener(() -> wrapperForSide.forEach((direction, lazyOptional) -> lazyOptional.invalidate()));
            }
        }

    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ClassicPipes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                //ClassicPipes.TRANSPARENT_BLOCKS.forEach(block -> ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout()));
                MenuScreens.register(ClassicPipes.DIAMOND_PIPE_MENU, DiamondPipeScreen::new);
                MenuScreens.register(ClassicPipes.ROUTING_PIPE_MENU, RoutingPipeScreen::new);
                MenuScreens.register(ClassicPipes.PROVIDER_PIPE_MENU, ProviderPipeScreen::new);
                MenuScreens.register(ClassicPipes.REQUEST_MENU, RequestScreen::new);
                MenuScreens.register(ClassicPipes.STOCKING_PIPE_MENU, StockingPipeScreen::new);
                MenuScreens.register(ClassicPipes.MATCHING_PIPE_MENU, MatchingPipeScreen::new);
                MenuScreens.register(ClassicPipes.STORAGE_PIPE_MENU, StoragePipeScreen::new);
                MenuScreens.register(ClassicPipes.RECIPE_PIPE_MENU, RecipePipeScreen::new);
                MenuScreens.register(ClassicPipes.DIAMOND_FLUID_PIPE_MENU, DiamondFluidPipeScreen::new);
                MenuScreens.register(ClassicPipes.ADVANCED_COPPER_PIPE_MENU, AdvancedCopperPipeScreen::new);
                MenuScreens.register(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_MENU, AdvancedCopperFluidPipeScreen::new);
            });
        }

    }

}
