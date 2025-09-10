package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.PipeBlock;
import jagm.classicpipes.services.Services;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

public class MiscUtil {

    public static final boolean DEBUG_MODE = false;
    public static final Comparator<Tuple<ItemStack, Boolean>> AMOUNT = Comparator.comparing(tuple -> tuple.a().getCount() - (tuple.b() ? 1 : 0));
    public static final Comparator<Tuple<ItemStack, Boolean>> NAME = Comparator.comparing(tuple -> tuple.a().getItem().getName().getString());
    public static final Comparator<Tuple<ItemStack, Boolean>> MOD = Comparator.comparing(tuple -> Services.LOADER_SERVICE.getModName(modFromItem(tuple.a())));
    public static final Comparator<Tuple<ItemStack, Boolean>> CRAFTABLE = Comparator.comparing(Tuple::b);

    public static final Codec<ItemStack> UNLIMITED_STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
            Codec.INT.fieldOf("count").forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
    ).apply(instance, ItemStack::new));

    public static ResourceLocation resourceLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(ClassicPipes.MOD_ID, name);
    }

    public static <T> ResourceKey<T> makeKey(ResourceKey<? extends Registry<T>> registry, String name) {
        return ResourceKey.create(registry, resourceLocation(name));
    }

    public static Direction nextDirection(Direction direction) {
        return Direction.from3DDataValue((direction.get3DDataValue() + 1) % 6);
    }

    public static Direction prevDirection(Direction direction) {
        return Direction.from3DDataValue((direction.get3DDataValue() + 5) % 6);
    }

    public static boolean itemIsPipe(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock() instanceof PipeBlock;
        }
        return false;
    }

    public static String modFromItem(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().split(":")[0];
    }

}
