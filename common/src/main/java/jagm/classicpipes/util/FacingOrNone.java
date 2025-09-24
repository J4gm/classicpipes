package jagm.classicpipes.util;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum FacingOrNone implements StringRepresentable {

    NORTH("north", Direction.NORTH),
    EAST("east", Direction.EAST),
    SOUTH("south", Direction.SOUTH),
    WEST("west", Direction.WEST),
    UP("up", Direction.UP),
    DOWN("down", Direction.DOWN),
    NONE("none", null);

    public static final EnumProperty<FacingOrNone> BLOCK_PROPERTY = EnumProperty.create("facing", FacingOrNone.class, FacingOrNone.values());

    private final String name;
    private final Direction direction;

    FacingOrNone(String name, Direction direction) {
        this.name = name;
        this.direction = direction;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Direction getDirection() {
        return direction;
    }

    public static FacingOrNone with(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

}
