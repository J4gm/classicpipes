package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Objects;

public class FluidWithData {

    public static final Codec<FluidWithData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").orElse(Fluids.EMPTY).forGetter(FluidWithData::getFluid),
                    CompoundTag.CODEC.fieldOf("data").orElse(new CompoundTag()).forGetter(FluidWithData::getCompoundTag)
            ).apply(instance, FluidWithData::new)
    );
    public static final FluidWithData EMPTY = new FluidWithData(Fluids.EMPTY, new CompoundTag());

    private final Fluid fluid;
    private final CompoundTag fluidData;

    public FluidWithData(Fluid fluid, CompoundTag tag) {
        this.fluid = fluid;
        this.fluidData = tag != null ? tag : new CompoundTag();
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public CompoundTag getCompoundTag() {
        return this.fluidData.copy();
    }

    public boolean isBlank() {
        return this.fluid == null || this.fluid.isSame(Fluids.EMPTY);
    }

    public ItemStack getBucketStack() {
        ItemStack bucketStack = new ItemStack(this.fluid.getBucket() != Items.AIR ? this.fluid.getBucket() : Items.BUCKET);
        bucketStack.getOrCreateTag().put("classic_pipes_fluid_data", CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElse(new CompoundTag()));
        return bucketStack;
    }

    @Override
    public boolean equals (Object other) {
        if (this == other) {
            return true;
        } else {
            if (other instanceof FluidWithData otherFluid) {
                return this.fluid.equals(otherFluid.fluid) && this.fluidData.equals(otherFluid.fluidData);
            }
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fluid, this.fluidData);
    }

}
