package jagm.classicpipes.util;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MissingItem {

    private final ItemStack stack;
    private final List<MissingItem> missingIngredients;

    public MissingItem(ItemStack stack) {
        this.stack = stack;
        this.missingIngredients = new ArrayList<>();
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public int getCount() {
        return this.stack.getCount();
    }

    public void shrink(int decrement) {
        this.stack.shrink(decrement);
    }

    public void grow(int increment) {
        this.stack.grow(increment);
    }

    public boolean hasMissingIngredients() {
        return !this.missingIngredients.isEmpty();
    }

    public void addMissingIngredient(MissingItem ingredient) {
        for (MissingItem alreadyThere : this.missingIngredients) {
            if (ItemStack.isSameItemSameTags(ingredient.stack, alreadyThere.stack)) {
                alreadyThere.grow(ingredient.getCount());
                for (MissingItem ingredientIngredient : ingredient.missingIngredients) {
                    alreadyThere.addMissingIngredient(ingredientIngredient);
                }
                return;
            }
        }
        this.missingIngredients.add(ingredient);
    }

    public List<ItemStack> getBaseItems(List<ItemStack> baseItems) {
        if (this.hasMissingIngredients()) {
            for (MissingItem ingredient : this.missingIngredients) {
                baseItems = ingredient.getBaseItems(baseItems);
            }
        } else {
            boolean matched = false;
            for (ItemStack alreadyThere : baseItems) {
                if (ItemStack.isSameItemSameTags(this.stack, alreadyThere)) {
                    alreadyThere.grow(this.stack.getCount());
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                baseItems.add(this.stack.copy());
            }
        }
        return baseItems;
    }

}
