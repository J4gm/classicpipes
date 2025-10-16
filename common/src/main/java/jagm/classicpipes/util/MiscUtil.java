package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.PipeBlock;
import jagm.classicpipes.services.Services;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class MiscUtil {

    public static final boolean DEBUG_MODE = false;

    public static final Comparator<Tuple<ItemStack, Boolean>> AMOUNT = Comparator.comparing(tuple -> tuple.a().getCount() - (tuple.b() ? 1 : 0));
    public static final Comparator<Tuple<ItemStack, Boolean>> NAME = Comparator.comparing(tuple -> tuple.a().getDisplayName().getString());
    public static final Comparator<Tuple<ItemStack, Boolean>> MOD = Comparator.comparing(tuple -> Services.LOADER_SERVICE.getModName(modFromItem(tuple.a())));
    public static final Comparator<Tuple<ItemStack, Boolean>> CRAFTABLE = Comparator.comparing(Tuple::b);

    // Uses int instead of byte for stack size.
    public static void writeStackToBuffer(FriendlyByteBuf buffer, ItemStack stack) {
        if (stack.isEmpty()) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            Item item = stack.getItem();
            buffer.writeId(BuiltInRegistries.ITEM, item);
            buffer.writeInt(stack.getCount());
            CompoundTag compoundtag = null;
            if (item.canBeDepleted() || item.shouldOverrideMultiplayerNbt()) {
                compoundtag = stack.getTag();
            }
            buffer.writeNbt(compoundtag);
        }
    }

    public static ItemStack readStackFromBuffer(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            Item item = buffer.readById(BuiltInRegistries.ITEM);
            if (item != null) {
                int count = buffer.readInt();
                ItemStack stack = new ItemStack(item, count);
                stack.setTag(buffer.readNbt());
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ResourceLocation resourceLocation(String name) {
        return new ResourceLocation(ClassicPipes.MOD_ID, name);
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

    public static <T> void loadFromTag(Tag tag, Codec<T> codec, Consumer<? super T> resultConsumer) {
        codec.parse(NbtOps.INSTANCE, tag).result().ifPresent(resultConsumer);
    }

    public static <T> void saveToTag(Tag tag, T thing, Codec<T> codec, Consumer<? super Tag> resultConsumer) {
        codec.encode(thing, NbtOps.INSTANCE, tag).result().ifPresent(resultConsumer);
    }

    public static <T> void saveToTag(T thing, Codec<T> codec, Consumer<? super Tag> resultConsumer) {
        codec.encodeStart(NbtOps.INSTANCE, thing).result().ifPresent(resultConsumer);
    }

    public static void mergeStackIntoList(List<ItemStack> list, ItemStack stack) {
        boolean matched = false;
        for (ItemStack listStack : list) {
            if (ItemStack.isSameItemSameComponents(listStack, stack)) {
                listStack.grow(stack.getCount());
                matched = true;
                break;
            }
        }
        if (!matched) {
            list.add(stack);
        }
    }

}
