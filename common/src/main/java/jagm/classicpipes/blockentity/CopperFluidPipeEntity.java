package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.CopperFluidPipeBlock;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.FacingOrNone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

public class CopperFluidPipeEntity extends FluidPipeEntity {

    private static final byte DEFAULT_COOLDOWN = 4;

    private byte cooldown;

    public CopperFluidPipeEntity(BlockPos pos, BlockState state) {
        this(ClassicPipes.COPPER_FLUID_PIPE_ENTITY, pos, state);
    }

    public CopperFluidPipeEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
        this.cooldown = DEFAULT_COOLDOWN;
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (state.getValue(CopperFluidPipeBlock.ENABLED) && state.getValue(CopperFluidPipeBlock.FACING) != FacingOrNone.NONE) {
            if (this.cooldown-- <= 0) {
                Direction direction = state.getValue(CopperFluidPipeBlock.FACING).getDirection();
                if (Services.LOADER_SERVICE.handleFluidExtraction(this, state, level, pos.relative(direction), direction.getOpposite(), this.extractAmount(), this.filterPredicate())) {
                    level.sendBlockUpdated(pos, state, state, 2);
                    this.setChanged();
                }
                this.cooldown = DEFAULT_COOLDOWN;
            }
        }
    }

    protected int extractAmount() {
        return 125;
    }

    protected Predicate<Fluid> filterPredicate() {
        return fluid -> true;
    }

    @Override
    protected void loadAdditional(CompoundTag valueInput, HolderLookup.Provider registries) {
        super.loadAdditional(valueInput, registries);
        this.cooldown = valueInput.getByteOr("cooldown", DEFAULT_COOLDOWN);
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput, HolderLookup.Provider registries) {
        super.saveAdditional(valueOutput, registries);
        valueOutput.putByte("cooldown", this.cooldown);
    }

}
