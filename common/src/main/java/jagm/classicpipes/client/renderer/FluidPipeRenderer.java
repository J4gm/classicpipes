package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jagm.classicpipes.block.FluidPipeBlock;
import jagm.classicpipes.blockentity.FluidPipeEntity;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FluidInPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeEntity, FluidPipeRenderer.FluidPipeRenderState> {

    private final BlockEntityRendererProvider.Context context;
    private final Map<FluidPipeEntity, Float> lastWidths;

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
        this.lastWidths = new HashMap<>();
    }

    @Override
    public FluidPipeRenderState createRenderState() {
        return new FluidPipeRenderState();
    }

    @Override
    public void extractRenderState(FluidPipeEntity pipe, FluidPipeRenderState pipeState, float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pipe, pipeState, partialTicks, cameraPos, breakProgress);
        FluidRenderInfo fluidInfo = Services.LOADER_SERVICE.getFluidRenderInfo(pipe.getFluid().defaultFluidState(), pipe.getLevel(), pipe.getBlockPos());
        boolean[] middleSides = new boolean[6];
        Arrays.fill(middleSides, true);
        boolean[] pipeDirections = new boolean[6];
        for (int i = 0; i < 6; i++) {
            pipeDirections[i] = pipe.getBlockState().getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.from3DDataValue(i)));
        }
        int totalAmount = 0;
        for (FluidInPipe fluidPacket : pipe.getContents()) {
            totalAmount += fluidPacket.getAmount();
            middleSides[fluidPacket.getFromDirection().get3DDataValue()] = false;
            middleSides[fluidPacket.getTargetDirection().get3DDataValue()] = false;
        }
        float targetWidth = Math.min(7.0F, totalAmount * 7.0F / FluidPipeEntity.CAPACITY) / 16.0F;
        float lastWidth = this.lastWidths.getOrDefault(pipe, 0.0F);
        float width = lastWidth + (targetWidth - lastWidth) / 32.0F;
        this.lastWidths.put(pipe, width);
        pipeState.initialise(fluidInfo, width, middleSides, pipeDirections);
    }

    @Override
    public boolean shouldRender(FluidPipeEntity pipe, Vec3 cameraPos) {
        boolean shouldRender = BlockEntityRenderer.super.shouldRender(pipe, cameraPos);
        if (!shouldRender) {
            this.lastWidths.remove(pipe);
        }
        return shouldRender;
    }

    @Override
    public void submit(FluidPipeRenderState pipeState, PoseStack poses, SubmitNodeCollector queue, CameraRenderState cameraState) {
        poses.pushPose();
        if (pipeState.width() > 0.01F) {
            TextureAtlasSprite fluidSprite = pipeState.fluidInfo().sprite() == null ? Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.WATER.defaultBlockState()) : pipeState.fluidInfo().sprite();
            queue.submitCustomGeometry(poses, RenderType.text(fluidSprite.atlasLocation()), ((pose, vertexBuffer) -> {
                Matrix4f matrix = pose.pose();
                float start = 0.5F - pipeState.width() / 2;
                float end = 0.5F + pipeState.width() / 2;
                boolean renderMiddle = false;
                for (Direction direction : Direction.values()) {
                    if (!pipeState.middleSides()[direction.get3DDataValue()] && pipeState.pipeDirections()[direction.get3DDataValue()]) {
                        renderMiddle = true;
                        boolean[] renderSides = new boolean[6];
                        Arrays.fill(renderSides, true);
                        renderSides[direction.getOpposite().get3DDataValue()] = false;
                        switch (direction) {
                            case UP -> this.renderFluidCuboid(vertexBuffer, matrix, start, end, start, end, 1.0F, end, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, renderSides);
                            case DOWN -> this.renderFluidCuboid(vertexBuffer, matrix, start, 0.0F, start, end, start, end, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, renderSides);
                            case EAST -> this.renderFluidCuboid(vertexBuffer, matrix, end, start, start, 1.0F, end, end, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, renderSides);
                            case WEST -> this.renderFluidCuboid(vertexBuffer, matrix, 0.0F, start, start, start, end, end, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, renderSides);
                            case SOUTH -> this.renderFluidCuboid(vertexBuffer, matrix, start, start, end, end, end, 1.0F, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, renderSides);
                            case NORTH -> this.renderFluidCuboid(vertexBuffer, matrix, start, start, 0.0F, end, end, start, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, renderSides);
                        }
                    }
                }
                if (renderMiddle) {
                    this.renderFluidCuboid(vertexBuffer, matrix, start, start, start, end, end, end, fluidSprite, pipeState.fluidInfo().tint(), pipeState.lightCoords, pipeState.middleSides());
                }
            }));
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

    public static class FluidPipeRenderState extends BlockEntityRenderState {

        private FluidRenderInfo fluidInfo;
        private float width;
        private boolean[] middleSides;
        private boolean[] pipeDirections;

        public void initialise(FluidRenderInfo fluidInfo, float width, boolean[] middleSides, boolean[] pipeDirections) {
            this.fluidInfo = fluidInfo;
            this.width = width;
            this.middleSides = middleSides;
            this.pipeDirections = pipeDirections;
        }

        public FluidRenderInfo fluidInfo() {
            return this.fluidInfo;
        }

        public float width() {
            return this.width;
        }

        public boolean[] middleSides() {
            return this.middleSides;
        }

        public boolean[] pipeDirections() {
            return this.pipeDirections;
        }

    }

}
