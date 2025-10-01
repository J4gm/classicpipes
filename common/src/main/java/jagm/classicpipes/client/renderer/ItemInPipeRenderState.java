package jagm.classicpipes.client.renderer;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record ItemInPipeRenderState(ItemStackRenderState stackState, Vec3 renderPos, Direction direction) {
}
