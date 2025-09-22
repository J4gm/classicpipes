package jagm.classicpipes.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.ItemPipeEntity;
import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PipeRenderer implements BlockEntityRenderer<ItemPipeEntity> {

    private final BlockEntityRendererProvider.Context context;

    public PipeRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(ItemPipeEntity pipe, float partialTicks, PoseStack poses, MultiBufferSource bufferSource, int light, int overlay, Vec3 cameraPos) {
        renderPipeItems(this.context, pipe, partialTicks, poses, bufferSource, light, overlay);
    }

    public static void renderPipeItems(BlockEntityRendererProvider.Context context, ItemPipeEntity pipe, float partialTicks, PoseStack poses, MultiBufferSource bufferSource, int light, int overlay) {
        if (!pipe.isEmpty()){
            ClassicPipes.LOGGER.info("Rendering pipe items");
            for (ItemInPipe item : pipe.getContents()) {
                Direction direction = item.getProgress() < ItemInPipe.HALFWAY ? item.getFromDirection() : item.getTargetDirection();
                poses.pushPose();
                poses.translate(item.getRenderPosition(partialTicks));
                poses.scale(0.4375F, 0.4375F, 0.4375F);
                if (direction.equals(Direction.EAST) || direction.equals(Direction.WEST)) {
                    poses.mulPose(Axis.YP.rotationDegrees(90.0F));
                } else if (direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) {
                    poses.mulPose(Axis.XP.rotationDegrees(90.0F));
                }
                context.getItemRenderer().renderStatic(item.getStack(), ItemDisplayContext.FIXED, light, overlay, poses, bufferSource, pipe.getLevel(), 0);
                poses.popPose();
            }
        }
        if (MiscUtil.DEBUG_MODE) {
            for (Direction direction : pipe.networkDistances.keySet()) {
                Component component = Component.literal(String.valueOf(pipe.networkDistances.get(direction).b()));
                poses.pushPose();
                poses.translate(
                        0.5F + (direction.equals(Direction.EAST) ? 0.375F : (direction.equals(Direction.WEST) ? -0.375F : 0.0F)),
                        0.5F + (direction.equals(Direction.UP) ? 0.375F : (direction.equals(Direction.DOWN) ? -0.375F : 0.0F)),
                        0.5F + (direction.equals(Direction.SOUTH) ? 0.375F : (direction.equals(Direction.NORTH) ? -0.375F : 0.0F))
                );
                poses.mulPose(context.getEntityRenderer().cameraOrientation());
                poses.scale(0.0125F, -0.0125F, 0.0125F);
                Matrix4f matrix4f = poses.last().pose();
                Font font = context.getFont();
                float f = (float)(-font.width(component)) / 2.0F;
                int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
                font.drawInBatch(component, f, 0, -2130706433, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, j, light);
                poses.popPose();
            }
            if (pipe instanceof NetworkedPipeEntity networkedPipe) {
                Component component = Component.literal(networkedPipe.isController() ? "CONTROLLER" : networkedPipe.syncedNetworkPos.toShortString());
                poses.pushPose();
                poses.translate(0.5F, 0.5F, 0.5F);
                poses.mulPose(context.getEntityRenderer().cameraOrientation());
                poses.scale(0.025F, -0.025F, 0.025F);
                Matrix4f matrix4f = poses.last().pose();
                Font font = context.getFont();
                float f = (float)(-font.width(component)) / 2.0F;
                int j = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
                font.drawInBatch(component, f, 0, -2130706433, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, j, light);
                poses.popPose();
            }
        }
    }

}
