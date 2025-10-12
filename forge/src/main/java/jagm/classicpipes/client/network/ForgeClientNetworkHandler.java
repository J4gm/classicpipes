package jagm.classicpipes.client.network;

import jagm.classicpipes.network.ForgeServerNetworkHandler;
import jagm.classicpipes.network.SelfHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ForgeClientNetworkHandler {

    public static <T> void registerClientPayload(int id, Class<T> clazz, SelfHandler<T> packet) {
        ForgeServerNetworkHandler.INSTANCE.registerMessage(id, clazz, packet::encode, packet::decode, (message, context) -> {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(message, Minecraft.getInstance().player)));
            context.get().setPacketHandled(true);
        });
    }

    public static <T> void sendToServer(T payload) {
        ForgeServerNetworkHandler.INSTANCE.sendToServer(payload);
    }

}
