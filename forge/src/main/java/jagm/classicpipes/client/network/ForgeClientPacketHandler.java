package jagm.classicpipes.client.network;

import jagm.classicpipes.network.ForgeServerPacketHandler;
import jagm.classicpipes.network.SelfHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.PacketDistributor;

public class ForgeClientPacketHandler {

    public static <T extends SelfHandler> void registerClientPayload(Class<T> clazz, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        ForgeServerPacketHandler.INSTANCE.play().clientbound().add(clazz, codec, (payload, context) -> {
            context.enqueueWork(() -> payload.handle(Minecraft.getInstance().player));
            context.setPacketHandled(true);
        });
    }

    public static void sendToServer(CustomPacketPayload payload) {
        ForgeServerPacketHandler.INSTANCE.send(payload, PacketDistributor.SERVER.noArg());
    }

}
