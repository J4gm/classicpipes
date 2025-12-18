package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RequestPipeEntity extends NetworkedPipeEntity implements MenuProvider {

    public static final Component TITLE = Component.translatable("container." + ClassicPipes.MOD_ID + ".request");

    public RequestPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.REQUEST_PIPE_ENTITY, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (this.hasNetwork()) {
            return new RequestMenu(id, inventory, this.getNetwork().requestItemList(this.getBlockPos()));
        }
        return null;
    }

    @Override
    public void eject(ServerLevel level, BlockPos pos, ItemInPipe item) {
        if (!item.getStack().isEmpty()) {
            Vec3 offset = new Vec3(
                    item.getTargetDirection() == Direction.WEST ? 0.125F : (item.getTargetDirection() == Direction.EAST ? 0.875F : 0.5F),
                    item.getTargetDirection() == Direction.DOWN ? 0.0F : (item.getTargetDirection() == Direction.UP ? 0.75F : 0.375F),
                    item.getTargetDirection() == Direction.NORTH ? 0.125F : (item.getTargetDirection() == Direction.SOUTH ? 0.875F : 0.5F)
            );
            ItemEntity ejectedItem = new ItemEntity(level, pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z, item.getStack());
            float v = 0.078125F;
            ejectedItem.setDeltaMovement(
                    item.getTargetDirection() == Direction.WEST ? -v : (item.getTargetDirection() == Direction.EAST ? v : 0.0F),
                    item.getTargetDirection() == Direction.DOWN ? -v : (item.getTargetDirection() == Direction.UP ? v : 0.0F),
                    item.getTargetDirection() == Direction.NORTH ? -v : (item.getTargetDirection() == Direction.SOUTH ? v : 0.0F)
            );
            level.addFreshEntity(ejectedItem);
            level.playSound(null, pos, ClassicPipes.PIPE_EJECT_SOUND, SoundSource.BLOCKS);
        }
    }

}
