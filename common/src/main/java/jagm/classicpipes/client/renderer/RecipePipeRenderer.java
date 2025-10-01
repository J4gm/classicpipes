package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jagm.classicpipes.blockentity.RecipePipeEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RecipePipeRenderer extends PipeRenderer<RecipePipeEntity> {

    public RecipePipeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(RecipePipeEntity pipe, PipeRenderState pipeState, float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(pipe, pipeState, partialTicks, cameraPos, breakProgress);
        List<ItemStackRenderState> heldItems = new ArrayList<>();
        List<Float> angles = new ArrayList<>();
        int i = 0;
        for (ItemStack stack : pipe.getHeldItems()) {
            if (!stack.isEmpty() && pipe.getLevel() != null) {
                ItemStackRenderState stackState = new ItemStackRenderState();
                this.context.itemModelResolver().updateForTopItem(stackState, stack, ItemDisplayContext.FIXED, pipe.getLevel(), null, 0);
                heldItems.add(stackState);
                angles.add(((pipe.getLevel().getGameTime() + i * 8) % 80 + partialTicks) * 2 * (float) Math.PI / 80);
            } else {
                heldItems.add(null);
                angles.add(0.0F);
            }
            i++;
        }
        pipeState.setHeldItems(heldItems, angles);
    }

    @Override
    public void submit(PipeRenderState pipeState, PoseStack poses, SubmitNodeCollector queue, CameraRenderState cameraState) {
        super.submit(pipeState, poses, queue, cameraState);
        for (int i = 0; i < pipeState.heldItems().size(); i++) {
            ItemStackRenderState stackState = pipeState.heldItems().get(i);
            if (stackState != null) {
                int a = i;
                float xOff = a % 2 == 0 ? 0.05F : -0.05F;
                a /= 2;
                float zOff = a % 2 == 0 ? 0.05F : -0.05F;
                a /= 2;
                float yOff = a % 2 == 0 ? 0.05F : -0.05F;
                poses.pushPose();
                poses.translate(0.5F + xOff, 0.5F + yOff, 0.5F + zOff);
                poses.scale(0.4375F, 0.4375F, 0.4375F);
                poses.mulPose(Axis.YP.rotation(pipeState.angles().get(i)));
                stackState.submit(poses, queue, pipeState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poses.popPose();
            }
        }
    }

}
