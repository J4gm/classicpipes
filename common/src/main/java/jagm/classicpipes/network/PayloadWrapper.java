package jagm.classicpipes.network;

import net.minecraft.resources.ResourceLocation;

public interface PayloadWrapper<T> {

    SelfHandler<T> getHandler();

    ResourceLocation getType();

}
