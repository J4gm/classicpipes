package jagm.classicpipes.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

public class RequestItemTrigger extends SimpleCriterionTrigger<RequestItemTrigger.RequestItemTriggerInstance> {

    public void trigger(ServerPlayer player, ItemStack stack, int uniqueItemsCrafted) {
        this.trigger(player, instance -> instance.matches(stack, uniqueItemsCrafted));
    }

    @Override
    public Codec<RequestItemTriggerInstance> codec() {
        return RequestItemTriggerInstance.CODEC;
    }

    public record RequestItemTriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<ItemPredicate> item, Optional<MinMaxBounds.Ints> uniqueItemsCrafted) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<RequestItemTriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(RequestItemTriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(RequestItemTriggerInstance::item),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("unique_items_crafted").forGetter(RequestItemTriggerInstance::uniqueItemsCrafted)

        ).apply(instance, RequestItemTriggerInstance::new));

        public boolean matches(ItemStack stack, int uniqueItemsCrafted) {
            if (this.item().isEmpty() || this.item().get().test(stack)) {
                return this.uniqueItemsCrafted().isEmpty() || this.uniqueItemsCrafted().get().matches(uniqueItemsCrafted);
            }
            return false;
        }
    }

}
