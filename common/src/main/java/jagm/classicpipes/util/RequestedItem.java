package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class RequestedItem {

    private static final short TIMEOUT = 24000;
    public static final Codec<RequestedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item").orElse(ItemStack.EMPTY).forGetter(RequestedItem::getStack),
            BlockPos.CODEC.fieldOf("destination").orElse(BlockPos.ZERO).forGetter(RequestedItem::getDestination),
            Codec.STRING.fieldOf("player").orElse("").forGetter(RequestedItem::getPlayerName)
    ).apply(instance, RequestedItem::new));

    private final ItemStack stack;
    private final BlockPos destination;
    private final String playerName;
    private short age;

    public RequestedItem(ItemStack stack, BlockPos destination, String playerName) {
        this.stack = stack;
        this.destination = destination;
        this.playerName = playerName;
        this.age = 0;
    }

    public void sendMessage(ServerLevel level, Component component) {
        List<ServerPlayer> players = level.getPlayers(player -> player.getName().getString().equals(this.playerName));
        if (!players.isEmpty()) {
            players.get(0).displayClientMessage(component, false);
        }
    }

    public boolean matches(ItemInPipe pipeItem) {
        return this.matches(pipeItem.getStack());
    }

    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameTags(stack, this.stack);
    }

    public boolean matches(RequestedItem anotherItem) {
        return this.playerName.equals(anotherItem.playerName) && this.destination.equals(anotherItem.destination) && this.matches(anotherItem.stack);
    }

    public NetworkedPipeEntity getTarget(Level level) {
        BlockEntity target = level.getBlockEntity(this.destination);
        if (target instanceof NetworkedPipeEntity) {
            return (NetworkedPipeEntity) target;
        }
        return null;
    }

    public int getAmountRemaining() {
        return this.stack.getCount();
    }

    public void arrived(int amount) {
        this.age = 0;
        this.stack.shrink(amount);
    }

    public boolean timedOut() {
        this.age++;
        return this.age > TIMEOUT;
    }

    public boolean isDelivered() {
        return this.stack.getCount() <= 0;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public BlockPos getDestination() {
        return this.destination;
    }

    private String getPlayerName() {
        return this.playerName;
    }

}
