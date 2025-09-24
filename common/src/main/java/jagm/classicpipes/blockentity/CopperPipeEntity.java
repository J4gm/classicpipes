package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.CopperPipeBlock;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class CopperPipeEntity extends RoundRobinPipeEntity {

    private static final byte DEFAULT_COOLDOWN = 8;

    private byte cooldown;

    public CopperPipeEntity(BlockPos pos, BlockState state) {
        this(ClassicPipes.COPPER_PIPE_ENTITY, pos, state);
    }

    public CopperPipeEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
        this.cooldown = DEFAULT_COOLDOWN;
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (state.getValue(CopperPipeBlock.ENABLED) && state.getValue(CopperPipeBlock.FACING) != FacingOrNone.NONE) {
            if (this.cooldown-- <= 0) {
                Direction direction = state.getValue(CopperPipeBlock.FACING).getDirection();
                if (Services.LOADER_SERVICE.handleItemExtraction(this, state, level, pos.relative(direction), direction.getOpposite(), this.extractAmount(), this.filterPredicate())) {
                    level.sendBlockUpdated(pos, state, state, 2);
                    this.setChanged();
                }
                this.cooldown = DEFAULT_COOLDOWN;
            }
        }
    }

    protected int extractAmount() {
        return 1;
    }

    protected Predicate<ItemStack> filterPredicate() {
        return stack -> true;
    }

    @Override
    public void load(CompoundTag valueInput) {
        super.load(valueInput);
        this.cooldown = valueInput.getByte("cooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putByte("cooldown", this.cooldown);
    }

}
