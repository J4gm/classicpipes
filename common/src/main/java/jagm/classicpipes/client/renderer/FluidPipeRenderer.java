package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jagm.classicpipes.block.FluidPipeBlock;
import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FluidInPipe;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeEntity> {

    private final BlockEntityRendererProvider.Context context;
    private final Map<FluidPipeEntity, Float> lastWidths;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
        this.lastWidths = new HashMap<>();
    }

    @Override
    public boolean shouldRender(FluidPipeEntity pipe, Vec3 cameraPos) {
        boolean ret = Vec3.atCenterOf(pipe.getBlockPos()).closerThan(cameraPos, this.getViewDistance());
        if (!ret) {
            this.lastWidths.remove(pipe);
        }
        return ret;
    }

    @Override
    public void render(FluidPipeEntity pipe, float partialTicks, PoseStack poses, MultiBufferSource bufferSource, int light, int overlay) {
        if (!this.lastWidths.containsKey(pipe)) {
            this.lastWidths.put(pipe, 0.0F);
        }
        poses.pushPose();
        Matrix4f matrix = poses.last().pose();
        FluidRenderInfo info = Services.LOADER_SERVICE.getFluidRenderInfo(pipe.getFluid().defaultFluidState(), pipe.getLevel(), pipe.getBlockPos());
        TextureAtlasSprite fluidSprite = info.sprite();
        if (fluidSprite == null) {
            fluidSprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.WATER.defaultBlockState());
        }
        VertexConsumer vertexBuffer = bufferSource.getBuffer(RenderType.text(fluidSprite.atlasLocation()));
        boolean[] middleSides = new boolean[6];
        Arrays.fill(middleSides, true);
        int totalAmount = 0;
        for (FluidInPipe fluidPacket : pipe.getContents()) {
            totalAmount += fluidPacket.getAmount();
            middleSides[fluidPacket.getFromDirection().get3DDataValue()] = false;
            middleSides[fluidPacket.getTargetDirection().get3DDataValue()] = false;
        }
        float targetWidth = Math.min(7.0F, totalAmount * 7.0F / FluidPipeEntity.CAPACITY) / 16.0F;
        float lastWidth = this.lastWidths.get(pipe);
        float width = lastWidth + (targetWidth - lastWidth) / 32.0F;
        this.lastWidths.put(pipe, width);
        if (width > 0.01F) {
            float start = 0.5F - width / 2;
            float end = 0.5F + width / 2;
            boolean renderMiddle = false;
            for (Direction direction : Direction.values()) {
                if (!middleSides[direction.get3DDataValue()] && pipe.getBlockState().getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
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
                this.renderFluidCuboid(vertexBuffer, matrix, start, start, start, end, end, end, fluidSprite, info.tint(), light, middleSides);
            }
        }
        poses.popPose();
        if (MiscUtil.DEBUG_MODE) {
            if (!pipe.isEmpty()){
                for (FluidInPipe fluidPacket : pipe.getContents()) {
                    Direction direction = fluidPacket.getProgress() < ItemInPipe.HALFWAY ? fluidPacket.getFromDirection() : fluidPacket.getTargetDirection();
                    poses.pushPose();
                    Vec3 vec = fluidPacket.getDebugRenderPosition(partialTicks);
                    poses.translate(vec.x, vec.y, vec.z);
                    poses.scale(0.4375F, 0.4375F, 0.4375F);
                    if (direction.equals(Direction.EAST) || direction.equals(Direction.WEST)) {
                        poses.mulPose(Axis.YP.rotationDegrees(90.0F));
                    } else if (direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) {
                        poses.mulPose(Axis.XP.rotationDegrees(90.0F));
                    }
                    this.context.getItemRenderer().renderStatic(new ItemStack(pipe.getFluid().getBucket()), ItemDisplayContext.FIXED, light, overlay, poses, bufferSource, pipe.getLevel(), 0);
                    poses.popPose();
                }
            }
        }
    }

    public void renderFluidCuboid(VertexConsumer vertexBuffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, TextureAtlasSprite fluidSprite, int tint, int light, boolean[] renderSide) {
        // y-axis
        float u1 = fluidSprite.getU(x1);
        float u2 = fluidSprite.getU(x2);
        float v1 = fluidSprite.getV(z1);
        float v2 = fluidSprite.getV(z2);
        if (renderSide[Direction.DOWN.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y1, z2).color(tint).uv(u1, v2);
            vertexBuffer.vertex(matrix, x1, y1, z1).color(tint).uv(u1, v1);
            vertexBuffer.vertex(matrix, x2, y1, z1).color(tint).uv(u2, v1);
            vertexBuffer.vertex(matrix, x2, y1, z2).color(tint).uv(u2, v2);
        }
        if (renderSide[Direction.UP.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y2, z1).color(tint).uv(u1, v2);
            vertexBuffer.vertex(matrix, x1, y2, z2).color(tint).uv(u1, v1);
            vertexBuffer.vertex(matrix, x2, y2, z2).color(tint).uv(u2, v1);
            vertexBuffer.vertex(matrix, x2, y2, z1).color(tint).uv(u2, v2);
        }
        // x-axis
        u1 = fluidSprite.getU(y1);
        u2 = fluidSprite.getU(y2);
        v1 = fluidSprite.getV(z1);
        v2 = fluidSprite.getV(z2);
        if (renderSide[Direction.WEST.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y2, z2).color(tint).uv(u1, v2);
            vertexBuffer.vertex(matrix, x1, y2, z1).color(tint).uv(u1, v1);
            vertexBuffer.vertex(matrix, x1, y1, z1).color(tint).uv(u2, v1);
            vertexBuffer.vertex(matrix, x1, y1, z2).color(tint).uv(u2, v2);
        }
        if (renderSide[Direction.EAST.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x2, y2, z1).color(tint).uv(u1, v2);
            vertexBuffer.vertex(matrix, x2, y2, z2).color(tint).uv(u1, v1);
            vertexBuffer.vertex(matrix, x2, y1, z2).color(tint).uv(u2, v1);
            vertexBuffer.vertex(matrix, x2, y1, z1).color(tint).uv(u2, v2);
        }
        // z-axis
        u1 = fluidSprite.getU(y1);
        u2 = fluidSprite.getU(y2);
        v1 = fluidSprite.getV(x1);
        v2 = fluidSprite.getV(x2);
        if (renderSide[Direction.NORTH.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x1, y2, z1).color(tint).uv(u1, v2);
            vertexBuffer.vertex(matrix, x2, y2, z1).color(tint).uv(u1, v1);
            vertexBuffer.vertex(matrix, x2, y1, z1).color(tint).uv(u2, v1);
            vertexBuffer.vertex(matrix, x1, y1, z1).color(tint).uv(u2, v2);
        }
        if (renderSide[Direction.SOUTH.get3DDataValue()]) {
            vertexBuffer.vertex(matrix, x2, y2, z2).color(tint).uv(u1, v2);
            vertexBuffer.vertex(matrix, x1, y2, z2).color(tint).uv(u1, v1);
            vertexBuffer.vertex(matrix, x1, y1, z2).color(tint).uv(u2, v1);
            vertexBuffer.vertex(matrix, x2, y1, z2).color(tint).uv(u2, v2);
        }
    }

}
