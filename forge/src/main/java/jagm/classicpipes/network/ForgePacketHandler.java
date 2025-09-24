package jagm.classicpipes.network;

import jagm.classicpipes.util.MiscUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ForgePacketHandler {

    private static final String VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(MiscUtil.resourceLocation("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private static int id = 0;

    public static <T> void registerServerPayload(Class<T> clazz, SelfHandler<T> packet) {
        INSTANCE.registerMessage(id++, clazz, packet::encode, packet::decode, (payload, context) -> {
            context.get().enqueueWork(() -> packet.handle(payload, context.get().getSender()));
            context.get().setPacketHandled(true);
        });
    }

    public static <T> void registerClientPayload(Class<T> clazz, SelfHandler<T> packet) {
        INSTANCE.registerMessage(id++, clazz, packet::encode, packet::decode, (message, context) -> {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(message, context.get().getSender())));
            context.get().setPacketHandled(true);
        });
    }

    public static <T> void sendToServer(T payload) {
        INSTANCE.sendToServer(payload);
    }

    public static <T> void sendToClient(ServerPlayer player, T payload) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

}
