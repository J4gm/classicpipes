package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.Tuple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PipeRenderer<T extends ItemPipeEntity> implements BlockEntityRenderer<T, PipeRenderer.PipeRenderState> {

    protected final BlockEntityRendererProvider.Context context;

    public PipeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public PipeRenderState createRenderState() {
        return new PipeRenderState();
    }

    @Override
    public void extractRenderState(T pipe, PipeRenderState pipeState, float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pipe, pipeState, partialTicks, cameraPos, breakProgress);
        List<ItemInPipeRenderState> itemRenderStates = new ArrayList<>();
        for (ItemInPipe item : pipe.getContents()) {
            ItemStackRenderState stackState = new ItemStackRenderState();
            this.context.itemModelResolver().updateForTopItem(stackState, item.getStack(), ItemDisplayContext.FIXED, pipe.getLevel(), null, 0);
            itemRenderStates.add(new ItemInPipeRenderState(stackState, item.getRenderPosition(partialTicks), item.getProgress() < ItemInPipe.HALFWAY ? item.getFromDirection() : item.getTargetDirection()));
        }
        if (pipe instanceof NetworkedPipeEntity networkedPipe) {
            pipeState.initialise(itemRenderStates, pipe.networkDistances, this.context.font(), networkedPipe.isController(), networkedPipe.syncedNetworkPos);
        } else {
            pipeState.initialise(itemRenderStates, pipe.networkDistances, this.context.font());
        }
    }

    @Override
    public void submit(PipeRenderState pipeState, PoseStack poses, SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (!pipeState.contents().isEmpty()) {
            for (ItemInPipeRenderState itemState : pipeState.contents()) {
                poses.pushPose();
                poses.translate(itemState.renderPos());
                poses.scale(0.4375F, 0.4375F, 0.4375F);
                if (itemState.direction().equals(Direction.EAST) || itemState.direction().equals(Direction.WEST)) {
                    poses.mulPose(Axis.YP.rotationDegrees(90.0F));
                } else if (itemState.direction().equals(Direction.UP) || itemState.direction().equals(Direction.DOWN)) {
                    poses.mulPose(Axis.XP.rotationDegrees(90.0F));
                }
                itemState.stackState().submit(poses, queue, pipeState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poses.popPose();
            }
        }
        if (MiscUtil.DEBUG_MODE) {
            Font font = pipeState.font();
            int background = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            for (Direction direction : pipeState.networkDistances().keySet()) {
                Component component = Component.literal(String.valueOf(pipeState.networkDistances().get(direction).b()));
                poses.pushPose();
                poses.translate(
                        0.5F + (direction.equals(Direction.EAST) ? 0.375F : (direction.equals(Direction.WEST) ? -0.375F : 0.0F)),
                        0.5F + (direction.equals(Direction.UP) ? 0.375F : (direction.equals(Direction.DOWN) ? -0.375F : 0.0F)),
                        0.5F + (direction.equals(Direction.SOUTH) ? 0.375F : (direction.equals(Direction.NORTH) ? -0.375F : 0.0F))
                );
                poses.mulPose(cameraState.orientation);
                poses.scale(0.0125F, -0.0125F, 0.0125F);
                float xOffset = (float)(-font.width(component)) / 2.0F;
                queue.submitText(poses, xOffset, 0.0F, component.getVisualOrderText(), false, Font.DisplayMode.NORMAL, pipeState.lightCoords, -2130706433, background, 0);
                poses.popPose();
            }
            if (pipeState.syncedNetworkPos() != null) {
                Component component = Component.literal(pipeState.controller() ? "CONTROLLER" : pipeState.syncedNetworkPos().toShortString());
                poses.pushPose();
                poses.translate(0.5F, 0.5F, 0.5F);
                poses.mulPose(cameraState.orientation);
                poses.scale(0.025F, -0.025F, 0.025F);
                float xOffset = (float)(-font.width(component)) / 2.0F;
                queue.submitText(poses, xOffset, 0.0F, component.getVisualOrderText(), false, Font.DisplayMode.NORMAL, pipeState.lightCoords, -2130706433, background, 0);
                poses.popPose();
            }
        }
    }

    public static class PipeRenderState extends BlockEntityRenderState {

        private List<ItemInPipeRenderState> contents;
        private Map<Direction, Tuple<BlockPos, Integer>> networkDistances;
        private Font font;
        private boolean controller;
        private BlockPos syncedNetworkPos;
        private List<ItemStackRenderState> heldItems;
        private List<Float> angles;

        public void initialise(List<ItemInPipeRenderState> contents, Map<Direction, Tuple<BlockPos, Integer>> networkDistances, Font font, boolean controller, BlockPos syncedNetworkPos) {
            this.contents = contents;
            this.networkDistances = networkDistances;
            this.font = font;
            this.controller = controller;
            this.syncedNetworkPos = syncedNetworkPos;
        }

        public void initialise(List<ItemInPipeRenderState> contents, Map<Direction, Tuple<BlockPos, Integer>> networkDistances, Font font) {
            this.initialise(contents, networkDistances, font, false, null);
        }

        public void setHeldItems(List<ItemStackRenderState> heldItems, List<Float> angles) {
            this.heldItems = heldItems;
            this.angles = angles;
        }

        public List<ItemInPipeRenderState> contents() {
            return this.contents;
        }

        public Map<Direction, Tuple<BlockPos, Integer>> networkDistances() {
            return networkDistances;
        }

        public Font font() {
            return this.font;
        }

        public boolean controller() {
            return this.controller;
        }

        public BlockPos syncedNetworkPos() {
            return this.syncedNetworkPos;
        }

        public List<ItemStackRenderState> heldItems() {
            return this.heldItems;
        }

        public List<Float> angles() {
            return this.angles;
        }

    }

}
