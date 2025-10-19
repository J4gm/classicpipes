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
    private final List<ItemStack> spareStacks;

    public RequestState() {
        this.itemsToWithdraw = new HashMap<>();
        this.itemsToRoute = new HashMap<>();
        this.missingStacks = new ArrayList<>();
        this.uniqueCraftedItems = new HashSet<>();
        this.spareStacks = new ArrayList<>();
    }

    private RequestState(Map<ProviderPipe, List<ItemStack>> itemsToWithdraw, Map<BlockPos, List<ItemStack>> itemsToRoute, List<ItemStack> missingStacks, Set<Item> uniqueCraftedItems, List<ItemStack> spareStacks) {
        this.itemsToWithdraw = itemsToWithdraw;
        this.itemsToRoute = itemsToRoute;
        this.missingStacks = missingStacks;
        this.uniqueCraftedItems = uniqueCraftedItems;
        this.spareStacks = spareStacks;
    }

    public RequestState copy() {
        Map<ProviderPipe, List<ItemStack>> copiedItemsToWithdraw = new HashMap<>();
        for (Map.Entry<ProviderPipe, List<ItemStack>> entry : this.itemsToWithdraw.entrySet()) {
            List<ItemStack> copiedWithdrawStacks = new ArrayList<>();
            entry.getValue().forEach(withdrawStack -> copiedWithdrawStacks.add(withdrawStack.copy()));
            copiedItemsToWithdraw.put(entry.getKey(), copiedWithdrawStacks);
        }
        Map<BlockPos, List<ItemStack>> copiedItemsToRoute = new HashMap<>();
        for (Map.Entry<BlockPos, List<ItemStack>> entry : this.itemsToRoute.entrySet()) {
            List<ItemStack> copiedRouteStacks = new ArrayList<>();
            entry.getValue().forEach(routeStack -> copiedRouteStacks.add(routeStack.copy()));
            copiedItemsToRoute.put(entry.getKey(), copiedRouteStacks);
        }
        List<ItemStack> copiedSpareStacks = new ArrayList<>();
        this.spareStacks.forEach(spareStack -> copiedSpareStacks.add(spareStack.copy()));
        return new RequestState(copiedItemsToWithdraw, copiedItemsToRoute, this.missingStacks, new HashSet<>(this.uniqueCraftedItems), copiedSpareStacks);
    }

    public void restore(RequestState backupState) {
        this.itemsToWithdraw.clear();
        this.itemsToWithdraw.putAll(backupState.itemsToWithdraw);
        this.itemsToRoute.clear();
        this.itemsToRoute.putAll(backupState.itemsToRoute);
        this.uniqueCraftedItems.clear();
        this.uniqueCraftedItems.addAll(backupState.uniqueCraftedItems);
        this.spareStacks.clear();
        this.spareStacks.addAll(backupState.spareStacks);
    }

    public int amountAlreadyWithdrawing(ProviderPipe providerPipe, ItemStack stack) {
        int amount = 0;
        if (this.itemsToWithdraw.containsKey(providerPipe)) {
            for (ItemStack withdrawStack : this.itemsToWithdraw.get(providerPipe)) {
                if (ItemStack.isSameItemSameComponents(withdrawStack, stack)) {
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
        this.missingStacks.add(stack);
    }

    public int missingStacksSize() {
        return this.missingStacks.size();
    }

    public void reduceMissingStacks(int targetSize) {
        while (this.missingStacks.size() > targetSize) {
            this.missingStacks.removeLast();
        }
    }

    public List<ItemStack> collateMissingStacks() {
        List<ItemStack> collatedStacks = new ArrayList<>();
        for (ItemStack missingStack : this.missingStacks) {
            MiscUtil.mergeStackIntoList(collatedStacks, missingStack);
        }
        return collatedStacks;
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

    public void addSpareStack(ItemStack stack) {
        MiscUtil.mergeStackIntoList(this.spareStacks, stack);
    }

    public List<ItemStack> getSpareStacks() {
        return this.spareStacks;
    }
}
