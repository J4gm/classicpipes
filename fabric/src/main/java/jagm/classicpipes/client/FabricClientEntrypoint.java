package jagm.classicpipes.client;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.client.renderer.FluidPipeRenderer;
import jagm.classicpipes.client.renderer.PipeRenderer;
import jagm.classicpipes.client.renderer.RecipePipeRenderer;
import jagm.classicpipes.client.screen.*;
import jagm.classicpipes.network.ClientBoundItemListPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

@SuppressWarnings("unused")
public class FabricClientEntrypoint implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ClassicPipes.TRANSPARENT_BLOCKS.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout()));

        BlockEntityRenderers.register(ClassicPipes.BASIC_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.GOLDEN_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.COPPER_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.IRON_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.DIAMOND_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.FLINT_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.LAPIS_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.OBSIDIAN_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.BONE_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.ROUTING_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.PROVIDER_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.REQUEST_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.STOCKING_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.MATCHING_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.STORAGE_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.RECIPE_PIPE_ENTITY, RecipePipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.ADVANCED_COPPER_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.COPPER_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.IRON_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.LAPIS_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.DIAMOND_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.OBSIDIAN_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.ADVANCED_COPPER_FLUID_PIPE_ENTITY, FluidPipeRenderer::new);

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

        ClientPlayNetworking.registerGlobalReceiver(ClientBoundItemListPayload.TYPE, (payload, context) -> payload.handle(context.player()));

    }

}
