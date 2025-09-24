package jagm.classicpipes.util;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class RequestItemTrigger extends SimpleCriterionTrigger<RequestItemTrigger.RequestItemTriggerInstance> {

    private static final ResourceLocation TYPE = MiscUtil.resourceLocation("request_item");

    public void trigger(ServerPlayer player, ItemStack stack, int uniqueItemsCrafted) {
        this.trigger(player, instance -> instance.matches(stack, uniqueItemsCrafted));
    }

    @Override
    protected RequestItemTriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        ItemPredicate item = json.has("item") ? ItemPredicate.fromJson(json.get("item")) : null;
        MinMaxBounds.Ints uniqueItemsCrafted = json.has("unique_items_crafted") ? MinMaxBounds.Ints.fromJson(json.get("unique_items_crafted")) : null;
        return new RequestItemTriggerInstance(TYPE, player, item, uniqueItemsCrafted);
    }

    @Override
    public ResourceLocation getId() {
        return TYPE;
    }

    public static class RequestItemTriggerInstance extends AbstractCriterionTriggerInstance {

        private final ItemPredicate item;
        private final MinMaxBounds.Ints uniqueItemsCrafted;

        public RequestItemTriggerInstance(ResourceLocation criterion, ContextAwarePredicate player, ItemPredicate item, MinMaxBounds.Ints uniqueItemsCrafted) {
            super(criterion, player);
            this.item = item;
            this.uniqueItemsCrafted = uniqueItemsCrafted;
        }

        public boolean matches(ItemStack stack, int uniqueItemsCrafted) {
            if (this.item == null || this.item.matches(stack)) {
                return this.uniqueItemsCrafted == null || this.uniqueItemsCrafted.matches(uniqueItemsCrafted);
            }
            return false;
        }

    }

}
