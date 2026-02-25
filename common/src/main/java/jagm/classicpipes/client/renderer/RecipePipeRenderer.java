package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jagm.classicpipes.blockentity.RecipePipeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RecipePipeRenderer implements BlockEntityRenderer<RecipePipeEntity> {

    private final BlockEntityRendererProvider.Context context;

    public RecipePipeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(RecipePipeEntity pipe, float partialTicks, PoseStack poses, MultiBufferSource bufferSource, int light, int overlay) {
        PipeRenderer.renderPipeItems(this.context, pipe, partialTicks, poses, bufferSource, light, overlay);
        List<List<ItemStack>> heldItems = pipe.getHeldItems();
        for (int i = 0; i < heldItems.size(); i++) {
            if (!heldItems.get(i).isEmpty()) {
                ItemStack stack = heldItems.get(i).get(0);
                if (!stack.isEmpty() && pipe.getLevel() != null) {
                    int a = i;
                    float xOff = a % 2 == 0 ? 0.05F : -0.05F;
                    a /= 2;
                    float zOff = a % 2 == 0 ? 0.05F : -0.05F;
                    a /= 2;
                    float yOff = a % 2 == 0 ? 0.05F : -0.05F;
                    poses.pushPose();
                    poses.translate(0.5F + xOff, 0.5F + yOff, 0.5F + zOff);
                    poses.scale(0.4375F, 0.4375F, 0.4375F);
                    poses.mulPose(Axis.YP.rotation(((pipe.getLevel().getGameTime() + i * 8) % 80 + partialTicks) * 2 * (float) Math.PI / 80)); // 1 rotation per 80 ticks
                    this.context.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poses, bufferSource, pipe.getLevel(), 0);
                    poses.popPose();
                }
            }
        }
    }

}
