package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.PipeBlock;
import jagm.classicpipes.services.Services;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackWithSlot> ITEM_STACK_WITH_SLOT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ItemStackWithSlot::slot,
            ItemStack.STREAM_CODEC,
            ItemStackWithSlot::stack,
            ItemStackWithSlot::new
    );

    public static Identifier identifier(String name) {
        return Identifier.fromNamespaceAndPath(ClassicPipes.MOD_ID, name);
    }

    public static <T> ResourceKey<T> makeKey(ResourceKey<? extends Registry<T>> registry, String name) {
        return ResourceKey.create(registry, identifier(name));
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

    public static void mergeStackIntoList(List<ItemStack> list, ItemStack stack) {
        if (!stack.isEmpty()) {
            boolean matched = false;
            for (ItemStack listStack : list) {
                if (ItemStack.isSameItemSameComponents(listStack, stack)) {
                    listStack.grow(stack.getCount());
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                list.add(stack.copy());
            }
        }
    }

    public static <VALUE, STACK> Optional<TagKey<?>> getTagEquivalent(Collection<STACK> stacks, Function<STACK, VALUE> stackToValue, Supplier<Stream<HolderSet.Named<VALUE>>> tagSupplier) {
        List<VALUE> values = stacks.stream().map(stackToValue).toList();
        return tagSupplier.get().filter(tag -> areEquivalent(tag, values)).<TagKey<?>>map(HolderSet.Named::key).findFirst();
    }

    private static <VALUE> boolean areEquivalent(HolderSet.Named<VALUE> tag, List<VALUE> values) {
        int count = tag.size();
        if (count != values.size()) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            VALUE tagValue = tag.get(i).value();
            VALUE value = values.get(i);
            if (!value.equals(tagValue)) {
                return false;
            }
        }
        return true;
    }

}
