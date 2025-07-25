package jagm.classicpipes.client;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.client.screen.DiamondPipeScreen;
import jagm.classicpipes.client.screen.ProviderPipeScreen;
import jagm.classicpipes.client.screen.RequestScreen;
import jagm.classicpipes.client.screen.RoutingPipeScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@SuppressWarnings("unused")
public class FabricClientEntrypoint implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClassicPipes.TRANSPARENT_BLOCKS.forEach(block -> BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.CUTOUT));
        BlockEntityRenderers.register(ClassicPipes.BASIC_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.GOLDEN_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.COPPER_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.IRON_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.DIAMOND_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.FLINT_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.LAPIS_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.OBSIDIAN_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.ROUTING_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.PROVIDER_PIPE_ENTITY, PipeRenderer::new);
        BlockEntityRenderers.register(ClassicPipes.REQUEST_PIPE_ENTITY, PipeRenderer::new);
        MenuScreens.register(ClassicPipes.DIAMOND_PIPE_MENU, DiamondPipeScreen::new);
        MenuScreens.register(ClassicPipes.ROUTING_PIPE_MENU, RoutingPipeScreen::new);
        MenuScreens.register(ClassicPipes.PROVIDER_PIPE_MENU, ProviderPipeScreen::new);
        MenuScreens.register(ClassicPipes.REQUEST_MENU, RequestScreen::new);
    }

}
