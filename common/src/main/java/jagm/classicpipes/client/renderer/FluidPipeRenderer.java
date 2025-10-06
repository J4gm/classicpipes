package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jagm.classicpipes.block.FluidPipeBlock;
import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.services.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

import java.util.Arrays;

public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeEntity> {

    private final BlockEntityRendererProvider.Context context;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(FluidPipeEntity pipe, float partialTicks, PoseStack poses, MultiBufferSource bufferSource, int light, int overlay) {
        poses.pushPose();
        Matrix4f matrix = poses.last().pose();
        FluidRenderInfo info = Services.LOADER_SERVICE.getFluidRenderInfo(pipe.getFluid().defaultFluidState(), pipe.getLevel(), pipe.getBlockPos());
        TextureAtlasSprite fluidSprite = info.sprite();
        if (fluidSprite == null) {
            fluidSprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.WATER.defaultBlockState());
        }
        VertexConsumer vertexBuffer = bufferSource.getBuffer(RenderType.text(fluidSprite.atlasLocation()));
        float width = pipe.lastRenderWidth + (pipe.targetRenderWidth - pipe.lastRenderWidth) * partialTicks;
        if (width > 0.01F) {
            float start = 0.5F - width / 2;
            float end = 0.5F + width / 2;
            boolean renderMiddle = false;
            for (Direction direction : Direction.values()) {
                if (!pipe.skipRenderingSide[direction.get3DDataValue()] && pipe.getBlockState().getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                    renderMiddle = true;
                    boolean[] renderSides = new boolean[6];
                    Arrays.fill(renderSides, true);
                    renderSides[direction.getOpposite().get3DDataValue()] = false;
                    switch (direction) {
                        case UP -> this.renderFluidCuboid(vertexBuffer, matrix, start, end, start, end, 1.0F, end, fluidSprite, info.tint(), light, renderSides);
                        case DOWN -> this.renderFluidCuboid(vertexBuffer, matrix, start, 0.0F, start, end, start, end, fluidSprite, info.tint(), light, renderSides);
                        case EAST -> this.renderFluidCuboid(vertexBuffer, matrix, end, start, start, 1.0F, end, end, fluidSprite, info.tint(), light, renderSides);
                        case WEST -> this.renderFluidCuboid(vertexBuffer, matrix, 0.0F, start, start, start, end, end, fluidSprite, info.tint(), light, renderSides);
                        case SOUTH -> this.renderFluidCuboid(vertexBuffer, matrix, start, start, end, end, end, 1.0F, fluidSprite, info.tint(), light, renderSides);
                        case NORTH -> this.renderFluidCuboid(vertexBuffer, matrix, start, start, 0.0F, end, end, start, fluidSprite, info.tint(), light, renderSides);
                    }
                }
            }
            if (renderMiddle) {
                this.renderFluidCuboid(vertexBuffer, matrix, start, start, start, end, end, end, fluidSprite, info.tint(), light, pipe.skipRenderingSide);
            }
        }
        poses.popPose();
    }

    public void renderFluidCuboid(VertexConsumer vertexBuffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, TextureAtlasSprite fluidSprite, int tint, int light, boolean[] renderSide) {
        // y-axis
        float u1 = fluidSprite.getU(x1);
        float u2 = fluidSprite.getU(x2);
        float v1 = fluidSprite.getV(z1);
        float v2 = fluidSprite.getV(z2);
        if (renderSide[Direction.DOWN.get3DDataValue()]) {
            vertexBuffer.addVertex(matrix, x1, y1, z2).setColor(tint).setUv(u1, v2).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y1, z1).setColor(tint).setUv(u1, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y1, z1).setColor(tint).setUv(u2, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y1, z2).setColor(tint).setUv(u2, v2).setLight(light);
        }
        if (renderSide[Direction.UP.get3DDataValue()]) {
            vertexBuffer.addVertex(matrix, x1, y2, z1).setColor(tint).setUv(u1, v2).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y2, z2).setColor(tint).setUv(u1, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y2, z2).setColor(tint).setUv(u2, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y2, z1).setColor(tint).setUv(u2, v2).setLight(light);
        }
        // x-axis
        u1 = fluidSprite.getU(y1);
        u2 = fluidSprite.getU(y2);
        v1 = fluidSprite.getV(z1);
        v2 = fluidSprite.getV(z2);
        if (renderSide[Direction.WEST.get3DDataValue()]) {
            vertexBuffer.addVertex(matrix, x1, y2, z2).setColor(tint).setUv(u1, v2).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y2, z1).setColor(tint).setUv(u1, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y1, z1).setColor(tint).setUv(u2, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y1, z2).setColor(tint).setUv(u2, v2).setLight(light);
        }
        if (renderSide[Direction.EAST.get3DDataValue()]) {
            vertexBuffer.addVertex(matrix, x2, y2, z1).setColor(tint).setUv(u1, v2).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y2, z2).setColor(tint).setUv(u1, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y1, z2).setColor(tint).setUv(u2, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y1, z1).setColor(tint).setUv(u2, v2).setLight(light);
        }
        // z-axis
        u1 = fluidSprite.getU(y1);
        u2 = fluidSprite.getU(y2);
        v1 = fluidSprite.getV(x1);
        v2 = fluidSprite.getV(x2);
        if (renderSide[Direction.NORTH.get3DDataValue()]) {
            vertexBuffer.addVertex(matrix, x1, y2, z1).setColor(tint).setUv(u1, v2).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y2, z1).setColor(tint).setUv(u1, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y1, z1).setColor(tint).setUv(u2, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y1, z1).setColor(tint).setUv(u2, v2).setLight(light);
        }
        if (renderSide[Direction.SOUTH.get3DDataValue()]) {
            vertexBuffer.addVertex(matrix, x2, y2, z2).setColor(tint).setUv(u1, v2).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y2, z2).setColor(tint).setUv(u1, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x1, y1, z2).setColor(tint).setUv(u2, v1).setLight(light);
            vertexBuffer.addVertex(matrix, x2, y1, z2).setColor(tint).setUv(u2, v2).setLight(light);
        }
    }

}
