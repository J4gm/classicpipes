package jagm.classicpipes.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.EitherCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.services.Services;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidWithData {

    public static final Codec<Either<DataComponentPatch, CompoundTag>> EITHER_DATA_CODEC = new EitherCodec<>(DataComponentPatch.CODEC, CompoundTag.CODEC);
    public static final Codec<FluidWithData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").orElse(Fluids.EMPTY).forGetter(FluidWithData::getFluid),
                EITHER_DATA_CODEC.fieldOf("data").orElse(Services.LOADER_SERVICE.emptyFluidData()).forGetter(fluid -> fluid.fluidData)
        ).apply(instance, FluidWithData::new)
    );

    private final Fluid fluid;
    private final Either<DataComponentPatch, CompoundTag> fluidData;

    public FluidWithData(Fluid fluid, Either<DataComponentPatch, CompoundTag> fluidData) {
        this.fluid = fluid;
        this.fluidData = fluidData;
    }

    public FluidWithData(Fluid fluid, DataComponentPatch components) {
        this(fluid, Either.left(components));
    }

    public FluidWithData(Fluid fluid, CompoundTag tag) {
        this(fluid, Either.right(tag));
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public DataComponentPatch getComponents() {
        return this.fluidData.left().orElse(DataComponentPatch.EMPTY);
    }

    public CompoundTag getCompoundTag() {
        return this.fluidData.right().orElse(new CompoundTag());
    }

    public boolean isBlank() {
        return this.fluid == null || this.fluid.isSame(Fluids.EMPTY);
    }

    public ItemStack getBucketStack() {
        if (this.fluid.getBucket() == null) {
            return ItemStack.EMPTY;
        } else {
            ItemStack bucketStack = new ItemStack(this.fluid.getBucket());
            bucketStack.set(ClassicPipes.FLUID_DATA_COMPONENT, this.fluidData);
            return bucketStack;
        }
    }

    @Override
    public boolean equals (Object other) {
        if (this == other) {
            return true;
        } else {
            if (other instanceof FluidWithData otherFluid) {
                if (this.fluid.equals(otherFluid.fluid)) {
                    if (this.fluidData.left().isPresent() && otherFluid.fluidData.left().isPresent()) {
                        return this.getComponents().equals(otherFluid.getComponents());
                    } else if (this.fluidData.right().isPresent() && otherFluid.fluidData.right().isPresent()) {
                        return this.getCompoundTag().equals(otherFluid.getCompoundTag());
                    }
                }
            }
            return false;
        }
    }

}
