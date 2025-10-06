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
        float u1 = fluidSprite.getU(x1 * 16.0F);
        float u2 = fluidSprite.getU(x2 * 16.0F);
        float v1 = fluidSprite.getV(z1 * 16.0F);
        float v2 = fluidSprite.getV(z2 * 16.0F);
        if (renderSide[Direction.DOWN.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y1, z2).color(tint).uv(u1, v2).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y1, z1).color(tint).uv(u1, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y1, z1).color(tint).uv(u2, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y1, z2).color(tint).uv(u2, v2).uv2(light).endVertex();
        }
        if (renderSide[Direction.UP.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y2, z1).color(tint).uv(u1, v2).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y2, z2).color(tint).uv(u1, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y2, z2).color(tint).uv(u2, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y2, z1).color(tint).uv(u2, v2).uv2(light).endVertex();
        }
        // x-axis
        u1 = fluidSprite.getU(y1 * 16.0F);
        u2 = fluidSprite.getU(y2 * 16.0F);
        v1 = fluidSprite.getV(z1 * 16.0F);
        v2 = fluidSprite.getV(z2 * 16.0F);
        if (renderSide[Direction.WEST.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y2, z2).color(tint).uv(u1, v2).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y2, z1).color(tint).uv(u1, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y1, z1).color(tint).uv(u2, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y1, z2).color(tint).uv(u2, v2).uv2(light).endVertex();
        }
        if (renderSide[Direction.EAST.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x2, y2, z1).color(tint).uv(u1, v2).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y2, z2).color(tint).uv(u1, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y1, z2).color(tint).uv(u2, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y1, z1).color(tint).uv(u2, v2).uv2(light).endVertex();
        }
        // z-axis
        u1 = fluidSprite.getU(y1 * 16.0F);
        u2 = fluidSprite.getU(y2 * 16.0F);
        v1 = fluidSprite.getV(x1 * 16.0F);
        v2 = fluidSprite.getV(x2 * 16.0F);
        if (renderSide[Direction.NORTH.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y2, z1).color(tint).uv(u1, v2).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y2, z1).color(tint).uv(u1, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y1, z1).color(tint).uv(u2, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y1, z1).color(tint).uv(u2, v2).uv2(light).endVertex();
        }
        if (renderSide[Direction.SOUTH.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x2, y2, z2).color(tint).uv(u1, v2).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y2, z2).color(tint).uv(u1, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x1, y1, z2).color(tint).uv(u2, v1).uv2(light).endVertex();
            vertexBuffer.vertex(matrix, x2, y1, z2).color(tint).uv(u2, v2).uv2(light).endVertex();
        }
    }

}
