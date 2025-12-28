package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

public class ObsidianPipeEntity extends ItemPipeEntity {

    public ObsidianPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.OBSIDIAN_PIPE_ENTITY, pos, state);
    }

    @Override
    public void routeItem(BlockState state, ItemInPipe item) {
        item.setEjecting(true);
    }

    @Override
    public void eject(ServerLevel level, BlockPos pos, ItemInPipe item) {
        level.playSound(null, pos, ClassicPipes.OBSIDIAN_PIPE_DESTROY_ITEM, SoundSource.BLOCKS, 0.25F, 4.0F);
        for (int i = 0; i < 4; i++) {
            level.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, item.getStack()),
                    pos.getX() + 0.5F,
                    pos.getY() + 0.5F,
                    pos.getZ() + 0.5F,
                    1,
                    level.random.nextGaussian() * 0.15,
                    level.random.nextDouble() * 0.2,
                    level.random.nextGaussian() * 0.15,
                    0.1F
            );
        }
    }

    @Override
    protected boolean canJoinNetwork() {
        return false;
    }

    @Override
    public short getTargetSpeed(BlockState state, Direction fromDirection, Direction targetDirection) {
        return ItemInPipe.DEFAULT_SPEED;
    }

    @Override
    public short getAcceleration(BlockState state, Direction fromDirection, Direction targetDirection) {
        return ItemInPipe.DEFAULT_ACCELERATION;
    }

}
