package jagm.classicpipes.network;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ForgeServerNetworkHandler {

    private static final String VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(MiscUtil.resourceLocation("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static <T> void registerServerPayload(int id, Class<T> clazz, SelfHandler<T> packet) {
        INSTANCE.registerMessage(id, clazz, packet::encode, packet::decode, (payload, context) -> {
            context.get().enqueueWork(() -> packet.handle(payload, context.get().getSender()));
            context.get().setPacketHandled(true);
        });
    }

    public static <T> void registerClientPayload(int id, Class<T> clazz, SelfHandler<T> packet) {
        INSTANCE.registerMessage(id, clazz, packet::encode, packet::decode, (message, context) -> {
            context.get().setPacketHandled(true);
        });
    }

    public static <T> void sendToClient(ServerPlayer player, T payload) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

}
