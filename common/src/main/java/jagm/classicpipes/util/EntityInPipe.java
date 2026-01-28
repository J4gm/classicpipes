package jagm.classicpipes.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public abstract class EntityInPipe {
    public static final short PIPE_LENGTH = 2048;
    public static final short HALFWAY = PIPE_LENGTH / 2;
    public static final short SPEED_LIMIT = HALFWAY;
    public static final short DEFAULT_SPEED = 64;
    public static final short DEFAULT_ACCELERATION = 1;
    public static final short DESPAWN_AGE = 24000;

    short speed;
    short progress;
    Direction fromDirection;
    Direction targetDirection;
    short age;


    public abstract EntityInPipe copyWithAmount(int amount);

    public abstract int getAmount();

    public abstract void setAmount(int amount);


    public Direction getTargetDirection() {
        return targetDirection;
    }

    public void setTargetDirection(Direction direction) {
        targetDirection = direction;
    }

    public Direction getFromDirection() {
        return fromDirection;
    }

    public short getAge() {
        return age;
    }

    public short getSpeed() {
        return speed;
    }

    public void resetProgress(Direction direction) {
        progress -= PIPE_LENGTH;
        fromDirection = direction;
        targetDirection = direction;
    }

    public short getProgress() {
        return progress;
    }

    public Vec3 getRenderPosition(float partialTicks) {
        float p = (float) progress / PIPE_LENGTH + partialTicks * (float) speed / PIPE_LENGTH;
        float q = 1.0F - p;
        boolean h = p < 0.5F;
        Direction d = h ? fromDirection : targetDirection;
        return new Vec3(
                d == Direction.WEST ? (h ? p : q) : (d == Direction.EAST ? (h ? q : p) : 0.5F),
                d == Direction.DOWN ? (h ? p : q) : (d == Direction.UP ? (h ? q : p) : 0.5F),
                d == Direction.NORTH ? (h ? p : q) : (d == Direction.SOUTH ? (h ? q : p) : 0.5F)
        );
    }

    public void move(short targetSpeed, short acceleration) {
        if (speed < targetSpeed) {
            speed = (short) Math.min(speed + acceleration, Math.min(targetSpeed, SPEED_LIMIT));
        } else if (speed > targetSpeed) {
            speed = (short) Math.max(speed - acceleration, Math.max(targetSpeed, 1));
        }
        progress += speed;
        age++;
    }
}
