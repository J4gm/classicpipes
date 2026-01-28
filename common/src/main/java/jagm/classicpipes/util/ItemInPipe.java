package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ItemInPipe extends EntityInPipe {
    public static final Codec<ItemInPipe> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                ItemStack.CODEC.fieldOf("item").orElse(ItemStack.EMPTY).forGetter(ItemInPipe::getStack),
                Codec.SHORT.fieldOf("speed").orElse(DEFAULT_SPEED).forGetter(ItemInPipe::getSpeed),
                Codec.SHORT.fieldOf("progress").orElse((short) 0).forGetter(ItemInPipe::getProgress),
                Codec.BYTE.fieldOf("from_direction").orElse((byte) 0).forGetter(item -> (byte) item.getFromDirection().get3DDataValue()),
                Codec.BYTE.fieldOf("target_direction").orElse((byte) 0).forGetter(item -> (byte) item.getTargetDirection().get3DDataValue()),
                Codec.BOOL.fieldOf("ejecting").orElse(true).forGetter(ItemInPipe::isEjecting),
                Codec.SHORT.fieldOf("age").orElse((short) 0).forGetter(ItemInPipe::getAge)
        ).apply(instance, ItemInPipe::new)
    );

    private ItemStack stack;
    private boolean ejecting;

    public ItemInPipe(ItemStack stack, short speed, short progress, Direction fromDirection, Direction targetDirection, boolean ejecting, short age) {
        this.stack = stack;
        this.speed = (short) Math.min(speed, SPEED_LIMIT);
        this.progress = progress;
        this.fromDirection = fromDirection;
        this.targetDirection = targetDirection;
        this.ejecting = ejecting;
        this.age = age;
    }

    public ItemInPipe(ItemStack stack, Direction fromDirection, Direction toDirection) {
        this(stack, fromDirection, toDirection, true);
    }

    public ItemInPipe(ItemStack stack, Direction fromDirection, Direction toDirection, boolean ejecting) {
        this(stack, DEFAULT_SPEED, (short) 0, fromDirection, toDirection, ejecting, (short) 0);
    }

    public ItemInPipe(ItemStack stack, short speed, short progress, byte fromDirection, byte targetDirection, boolean ejecting, short age) {
        this(stack, speed, progress, Direction.from3DDataValue(fromDirection), Direction.from3DDataValue(targetDirection), ejecting, age);
    }

    public ItemInPipe copyWithAmount(int count) {
        return new ItemInPipe(this.getStack().copyWithCount(count), this.speed, this.progress, this.fromDirection, this.targetDirection, this.ejecting, this.age);
    }

    public void drop(ServerLevel level, BlockPos pos) {
        if (!this.stack.isEmpty()) {
            Vec3 offset = this.getRenderPosition(0.0F);
            ItemEntity droppedItem = new ItemEntity(level, pos.getX() + offset.x, pos.getY() + offset.y, pos.getZ() + offset.z, this.stack);
            droppedItem.setDefaultPickUpDelay();
            level.addFreshEntity(droppedItem);
        }
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public boolean isEjecting() {
        return this.ejecting;
    }

    public void setEjecting(boolean ejecting) {
        this.ejecting = ejecting;
    }

    public int getAmount() {
        return this.stack.getCount();
    }

    public void setAmount(int count) {
        this.stack.setCount(count);
    }

}
