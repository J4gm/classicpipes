package jagm.classicpipes.block;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.RoundRobinPipeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class NetherBrickPipeBlock extends BooleanDirectionsPipeBlock {

    public NetherBrickPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoundRobinPipeEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ClassicPipes.BASIC_PIPE_ENTITY ? RoundRobinPipeEntity::tick : null;
    }

    @Override
    protected boolean canConnectToPipe(PipeBlock pipeBlock){
        return pipeBlock instanceof WoodenPipeBlock || pipeBlock instanceof BrickPipeBlock || pipeBlock instanceof NetherBrickPipeBlock;
    }

}
