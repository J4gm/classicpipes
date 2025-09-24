package jagm.classicpipes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public abstract class SelfHandler<T> {

    public abstract FriendlyByteBuf encode(T payload, FriendlyByteBuf buffer);

    public abstract T decode(FriendlyByteBuf buffer);

    public abstract void handle(T payload, Player player);

}
