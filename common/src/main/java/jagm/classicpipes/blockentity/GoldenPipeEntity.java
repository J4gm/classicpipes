package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.util.ItemInPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class GoldenPipeEntity extends RoundRobinPipeEntity {

    public GoldenPipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.GOLDEN_PIPE_ENTITY, pos, state);
    }

    @Override
    public short getTargetSpeed(BlockState state, Direction fromDirection, Direction targetDirection) {
        return ItemInPipe.SPEED_LIMIT;
    }

    @Override
    public short getAcceleration(BlockState state, Direction fromDirection, Direction targetDirection) {
        return ItemInPipe.DEFAULT_ACCELERATION * 16;
    }

}
