package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;

public class FluidInPipe extends EntityInPipe {
    public static final Codec<FluidInPipe> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("amount").orElse(0).forGetter(FluidInPipe::getAmount),
                    Codec.SHORT.fieldOf("speed").orElse((short) 0).forGetter(FluidInPipe::getSpeed),
                    Codec.SHORT.fieldOf("progress").orElse((short) 0).forGetter(FluidInPipe::getProgress),
                    Codec.BYTE.fieldOf("from_direction").orElse((byte) 0).forGetter(fluidPacket -> (byte) fluidPacket.getFromDirection().get3DDataValue()),
                    Codec.BYTE.fieldOf("target_direction").orElse((byte) 0).forGetter(fluidPacket -> (byte) fluidPacket.getTargetDirection().get3DDataValue()),
                    Codec.SHORT.fieldOf("age").orElse((short) 0).forGetter(FluidInPipe::getAge)
            ).apply(instance, FluidInPipe::new)
    );

    private int amount;

    public FluidInPipe(int amount, short speed, short progress, Direction fromDirection, Direction targetDirection, short age) {
        this.amount = amount;
        this.speed = speed;
        this.progress = progress;
        this.fromDirection = fromDirection;
        this.targetDirection = targetDirection;
        this.age = age;
    }

    public FluidInPipe(int amount, short speed, short progress, byte fromDirection, byte targetDirection, short age) {
        this(amount, speed, progress, Direction.from3DDataValue(fromDirection), Direction.from3DDataValue(targetDirection), age);
    }


    public FluidInPipe copyWithAmount(int amount) {
        return new FluidInPipe(amount, this.speed, this.progress, this.fromDirection, this.targetDirection, this.age);
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

}
