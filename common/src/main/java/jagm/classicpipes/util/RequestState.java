package jagm.classicpipes.util;

import jagm.classicpipes.blockentity.ProviderPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class RequestState {

    private final Map<ProviderPipe, List<ItemStack>> itemsToWithdraw;
    private final Map<BlockPos, List<ItemStack>> itemsToRoute;
    private final List<ItemStack> missingStacks;
    private final Set<Item> uniqueCraftedItems;

    public RequestState() {
        this.itemsToWithdraw = new HashMap<>();
        this.itemsToRoute = new HashMap<>();
        this.missingStacks = new ArrayList<>();
        this.uniqueCraftedItems = new HashSet<>();
    }

    public int amountAlreadyWithdrawing(ProviderPipe providerPipe, ItemStack stack) {
        int amount = 0;
        if (this.itemsToWithdraw.containsKey(providerPipe)) {
            for (ItemStack withdrawStack : this.itemsToWithdraw.get(providerPipe)) {
                if (ItemStack.isSameItemSameTags(withdrawStack, stack)) {
                    amount += withdrawStack.getCount();
                }
            }
        }
        return amount;
    }

    public void scheduleItemWithdrawal(ProviderPipe providerPipe, ItemStack stack) {
        if (this.itemsToWithdraw.containsKey(providerPipe)) {
            MiscUtil.mergeStackIntoList(this.itemsToWithdraw.get(providerPipe), stack);
        } else {
            List<ItemStack> withdrawStacks = new ArrayList<>();
            withdrawStacks.add(stack);
            this.itemsToWithdraw.put(providerPipe, withdrawStacks);
        }
    }

    public void scheduleItemRouting(BlockPos requestPos, ItemStack stack) {
        boolean matched = false;
        for (BlockPos pos : this.itemsToRoute.keySet()) {
            if (pos.equals(requestPos)) {
                MiscUtil.mergeStackIntoList(this.itemsToRoute.get(pos), stack);
                matched = true;
                break;
            }
        }
        if (!matched) {
            List<ItemStack> stacksToRoute = new ArrayList<>();
            stacksToRoute.add(stack);
            this.itemsToRoute.put(requestPos, stacksToRoute);
        }
    }

    public void addMissingStack(ItemStack stack) {
        MiscUtil.mergeStackIntoList(this.missingStacks, stack);
    }

    public int missingStacksSize() {
        return this.missingStacks.size();
    }

    public void reduceMissingStacks(int targetSize) {
        while (this.missingStacks.size() > targetSize) {
            this.missingStacks.remove(this.missingStacks.size() - 1);
        }
    }

    public List<ItemStack> getMissingStacks() {
        return this.missingStacks;
    }

    public Map<BlockPos, List<ItemStack>> getItemsToRoute() {
        return this.itemsToRoute;
    }

    public Map<ProviderPipe, List<ItemStack>> getItemsToWithdraw() {
        return this.itemsToWithdraw;
    }

    public void addCraftedItem(ItemStack stack) {
        this.uniqueCraftedItems.add(stack.getItem());
    }

    public int getUniqueCrafts() {
        return this.uniqueCraftedItems.size();
    }

}
