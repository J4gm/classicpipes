package jagm.classicpipes.network;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class ForgeServerPacketHandler {

    public static final SimpleChannel INSTANCE = ChannelBuilder.named(MiscUtil.identifier("main")).simpleChannel();

    public static <T extends SelfHandler> void registerServerPayload(Class<T> clazz, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        INSTANCE.play().serverbound().add(clazz, codec, (payload, context) -> {
            context.enqueueWork(() -> payload.handle(context.getSender()));
            context.setPacketHandled(true);
        });
    }

    public static <T extends SelfHandler> void registerClientPayload(Class<T> clazz, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        INSTANCE.play().clientbound().add(clazz, codec, (payload, context) -> {
            context.setPacketHandled(true);
        });
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        INSTANCE.send(payload, PacketDistributor.PLAYER.with(player));
    }

}
