package jagm.classicpipes.util;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.*;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.item.LabelItem;
import jagm.classicpipes.item.ModLabelItem;
import jagm.classicpipes.item.TagLabelItem;
import jagm.classicpipes.network.ClientBoundItemListPayload;
import jagm.classicpipes.services.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class PipeNetwork {

    private static final byte DEFAULT_COOLDOWN = 40; // 2 seconds between client updates.

    private final BlockPos pos;
    private final Set<RoutingPipeEntity> routingPipes;
    private final Set<NetworkedPipeEntity> defaultRoutes;
    private final Set<ProviderPipe> providerPipes;
    private final Set<StockingPipeEntity> stockingPipes;
    private final Set<MatchingPipe> matchingPipes;
    private final Set<RecipePipeEntity> recipePipes;
    private SortingMode sortingMode;
    private boolean cacheChanged;
    private byte cacheCooldown;
    private final List<RequestedItem> requestedItems;

    public PipeNetwork(BlockPos pos, SortingMode sortingMode) {
        this.routingPipes = new HashSet<>();
        this.defaultRoutes = new HashSet<>();
        this.providerPipes = new LinkedHashSet<>();
        this.stockingPipes = new HashSet<>();
        this.matchingPipes = new HashSet<>();
        this.recipePipes = new HashSet<>();
        this.sortingMode = sortingMode;
        this.pos = pos;
        this.cacheChanged = false;
        this.cacheCooldown = (byte) 0;
        this.requestedItems = new ArrayList<>();
    }

    public PipeNetwork(BlockPos pos) {
        this(pos, SortingMode.AMOUNT_DESCENDING);
    }

    private Tuple<Integer, Boolean> amountCraftable(ItemStack stack, int requiredAmount, BlockPos requestPos, RequestState requestState, List<ItemStack> itemsInThisBranch) {
        int amount = 0;
        boolean hasRecipe = false;
        int missingStacksSize = requestState.missingStacksSize();
        boolean isLabel = stack.getItem() instanceof LabelItem;
        for (RecipePipeEntity recipePipe : this.recipePipes) {
            ItemStack resultStack = recipePipe.getResult();
            if (!isLabel && ItemStack.isSameItemSameComponents(resultStack, stack) || isLabel && ((LabelItem)stack.getItem()).itemMatches(stack, resultStack)) {
                hasRecipe = true;
                requestState.reduceMissingStacks(missingStacksSize);
                List<ItemStack> ingredients = recipePipe.getIngredientsCollated();
                int requiredCrafts = Math.ceilDiv(requiredAmount, resultStack.getCount());
                boolean loopFound = false;
                for (ItemStack ingredientStack : ingredients) {
                    for (ItemStack branchStack : itemsInThisBranch) {
                        if (ItemStack.isSameItemSameComponents(branchStack, ingredientStack)) {
                            missingStacksSize = requestState.missingStacksSize();
                            requestState.addMissingStack(ingredientStack.copyWithCount(ingredientStack.getCount() * requiredCrafts));
                            loopFound = true;
                            break;
                        }
                    }
                    if (loopFound) {
                        break;
                    }
                }
                if (!loopFound) {
                    int possibleCrafts = requiredCrafts;
                    for (ItemStack ingredientStack : ingredients) {
                        List<ItemStack> newBranchItems = new ArrayList<>(itemsInThisBranch);
                        newBranchItems.add(ingredientStack);
                        RequestState backupState = requestState.copy();
                        int requiredIngredientAmount = ingredientStack.getCount() * requiredCrafts;
                        int ingredientAmount = this.availableAmount(ingredientStack.copyWithCount(requiredIngredientAmount), recipePipe.getBlockPos(), requestState, newBranchItems);
                        possibleCrafts = Math.min(possibleCrafts, ingredientAmount / ingredientStack.getCount());
                        requestState.restore(backupState);
                    }
                    if (possibleCrafts > 0) {
                        int missingStacksSize2 = requestState.missingStacksSize();
                        for (ItemStack ingredientStack : ingredients) {
                            List<ItemStack> newBranchItems = new ArrayList<>(itemsInThisBranch);
                            newBranchItems.add(ingredientStack);
                            int possibleIngredientAmount = ingredientStack.getCount() * possibleCrafts;
                            this.availableAmount(ingredientStack.copyWithCount(possibleIngredientAmount), recipePipe.getBlockPos(), requestState, newBranchItems);
                        }
                        requestState.reduceMissingStacks(missingStacksSize2);
                        int amountToDeliver = Math.min(resultStack.getCount() * possibleCrafts, requiredAmount);
                        amount += amountToDeliver;
                        requestState.scheduleItemRouting(requestPos, resultStack.copyWithCount(amountToDeliver));
                        requiredAmount -= resultStack.getCount() * possibleCrafts;
                        missingStacksSize = requestState.missingStacksSize();
                        requestState.addCraftedItem(resultStack);
                        if (requiredAmount <= 0) {
                            if (requiredAmount < 0) {
                                requestState.addSpareStack(resultStack.copyWithCount(-requiredAmount));
                            }
                            break;
                        }
                    }
                }
            }
        }
        return new Tuple<>(amount, hasRecipe);
    }

    private int amountInNetwork(ItemStack stack, BlockPos requestPos, RequestState requestState) {
        int amount = 0;
        boolean isLabel = stack.getItem() instanceof LabelItem;
        for (ItemStack spareStack : requestState.getSpareStacks()) {
            if (isLabel) {
                if (((LabelItem)stack.getItem()).itemMatches(stack, spareStack)) {
                    int spareAmount = Math.min(spareStack.getCount(), stack.getCount());
                    if (spareAmount > 0) {
                        amount += spareAmount;
                        spareStack.shrink(spareAmount);
                        requestState.scheduleItemRouting(requestPos, spareStack.copyWithCount(spareAmount));
                    }
                    if (amount >= stack.getCount()) {
                        break;
                    }
                }
            } else if (ItemStack.isSameItemSameComponents(spareStack, stack)) {
                int spareAmount = Math.min(spareStack.getCount(), stack.getCount());
                if (spareAmount > 0) {
                    amount += spareAmount;
                    spareStack.shrink(spareAmount);
                    requestState.scheduleItemRouting(requestPos, spareStack.copyWithCount(spareAmount));
                }
                break;
            }
        }
        requestState.getSpareStacks().removeIf(ItemStack::isEmpty);
        if (amount < stack.getCount()) {
            for (ProviderPipe providerPipe : this.providerPipes) {
                for (ItemStack cacheStack : providerPipe.getCache()) {
                    if (isLabel) {
                        if (((LabelItem)stack.getItem()).itemMatches(stack, cacheStack)) {
                            int amountProvidable = Math.min(stack.getCount() - amount, cacheStack.getCount() - requestState.amountAlreadyWithdrawing(providerPipe, cacheStack));
                            if (amountProvidable > 0) {
                                amount += amountProvidable;
                                requestState.scheduleItemWithdrawal(providerPipe, cacheStack.copyWithCount(amountProvidable));
                                requestState.scheduleItemRouting(requestPos, cacheStack.copyWithCount(amountProvidable));
                            }
                            if (amount >= stack.getCount()) {
                                break;
                            }
                        }
                    } else if (ItemStack.isSameItemSameComponents(cacheStack, stack)) {
                        int amountProvidable = Math.min(stack.getCount() - amount, cacheStack.getCount() - requestState.amountAlreadyWithdrawing(providerPipe, cacheStack));
                        if (amountProvidable > 0) {
                            amount += amountProvidable;
                            requestState.scheduleItemWithdrawal(providerPipe, cacheStack.copyWithCount(amountProvidable));
                            requestState.scheduleItemRouting(requestPos, cacheStack.copyWithCount(amountProvidable));
                        }
                        break;
                    }
                }
                if (amount >= stack.getCount()) {
                    break;
                }
            }
        }
        return amount;
    }

    private int availableAmount(ItemStack stack, BlockPos requestPos, RequestState requestState, List<ItemStack> itemsInThisBranch) {
        int amount = this.amountInNetwork(stack, requestPos, requestState);
        if (amount < stack.getCount()) {
            Tuple<Integer, Boolean> tuple = this.amountCraftable(stack, stack.getCount() - amount, requestPos, requestState, itemsInThisBranch);
            amount += tuple.a();
            if (!tuple.b()) {
                requestState.addMissingStack(stack.copyWithCount(stack.getCount() - amount));
            }
        }
        return amount;
    }

    public void request(ServerLevel level, ItemStack stack, BlockPos requestPos, Player player, boolean partialRequests) {
        RequestState requestState = new RequestState();
        int amount = this.availableAmount(stack, requestPos, requestState, new ArrayList<>());
        if (amount < stack.getCount()) {
            if (partialRequests) {
                this.request(level, stack.copyWithCount(amount), requestPos, player, false);
            } else if (player != null) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.a", stack.getCount(), stack.getItem().getDescription()).withStyle(ChatFormatting.RED), false);
                for (ItemStack missingStack : requestState.collateMissingStacks()) {
                    if (missingStack.getItem() instanceof TagLabelItem) {
                        String label = missingStack.get(ClassicPipes.LABEL_COMPONENT);
                        if (label != null) {
                            MutableComponent tagTranslation = Component.translatableWithFallback(TagLabelItem.labelToTranslationKey(label), "");
                            if (!tagTranslation.getString().isEmpty()) {
                                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.translated_tag", missingStack.getCount(), "#" + label, tagTranslation).withStyle(ChatFormatting.YELLOW), false);
                            } else {
                                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.b", missingStack.getCount(), "#" + label).withStyle(ChatFormatting.YELLOW), false);
                            }
                        }
                    } else if (missingStack.getItem() instanceof ModLabelItem) {
                        String label = missingStack.get(ClassicPipes.LABEL_COMPONENT);
                        if (label != null) {
                            player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.mod", missingStack.getCount(), Services.LOADER_SERVICE.getModName(label)).withStyle(ChatFormatting.YELLOW), false);
                        }
                    } else {
                        player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.b", missingStack.getCount(), missingStack.getItem().getDescription()).withStyle(ChatFormatting.YELLOW), false);
                    }
                }
            }
        } else {
            String playerName = player != null ? player.getName().getString() : "";
            int requestedItemsSize = this.requestedItems.size();
            for (Map.Entry<BlockPos, List<ItemStack>> entry : requestState.getItemsToRoute().entrySet()) {
                for (ItemStack routeStack : entry.getValue()) {
                    this.requestedItems.add(new RequestedItem(routeStack, entry.getKey(), playerName));
                }
            }
            boolean success = true;
            for (Map.Entry<ProviderPipe, List<ItemStack>> entry : requestState.getItemsToWithdraw().entrySet()) {
                for (ItemStack withdrawStack : entry.getValue()) {
                    boolean withdrawalSuccessful = false;
                    for (ItemStack cacheStack : entry.getKey().getCache()) {
                        if (ItemStack.isSameItemSameComponents(cacheStack, withdrawStack)) {
                            int amountToExtract = Math.min(cacheStack.getCount(), withdrawStack.getCount());
                            withdrawalSuccessful = entry.getKey().extractItem(level, cacheStack.copyWithCount(amountToExtract));
                            break;
                        }
                    }
                    if (!withdrawalSuccessful) {
                        success = false;
                        while (this.requestedItems.size() > requestedItemsSize) {
                            this.requestedItems.removeLast();
                        }
                        if (player != null) {
                            player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".could_not_extract", stack.getCount(), stack.getItem().getDescription(), entry.getKey().getProviderPipePos().toShortString()).withStyle(ChatFormatting.RED), false);
                        }
                        break;
                    }
                }
                if (!success) {
                    break;
                }
            }
            if (success && player != null) {
                player.awardStat(ClassicPipes.ITEMS_REQUESTED_STAT, stack.getCount());
                ClassicPipes.REQUEST_ITEM_TRIGGER.trigger((ServerPlayer) player, stack, requestState.getUniqueCrafts());
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".requested", stack.getCount(), stack.getItem().getDescription()).withStyle(ChatFormatting.GREEN), false);
            }
        }
    }

    public void tick(ServerLevel level) {
        int updatablePipesCount = this.providerPipes.size() + this.matchingPipes.size() + this.stockingPipes.size();
        int pipeToUpdate = level.getRandom().nextInt(Math.max(100, updatablePipesCount));
        if (pipeToUpdate < this.providerPipes.size()) {
            int i = 0;
            for (ProviderPipe providerPipe : this.providerPipes) {
                if (i == pipeToUpdate) {
                    Direction facing = providerPipe.getFacing();
                    if (facing != null) {
                        providerPipe.updateCache();
                    }
                    break;
                }
                i++;
            }
        } else if (pipeToUpdate < this.providerPipes.size() + this.matchingPipes.size()) {
            int i = 0;
            for (MatchingPipe matchingPipe : this.matchingPipes) {
                if (i == pipeToUpdate) {
                    matchingPipe.updateCache();
                    break;
                }
                i++;
            }
        } else if (pipeToUpdate < updatablePipesCount) {
            int i = 0;
            for (StockingPipeEntity stockingPipe : this.stockingPipes) {
                if (i == pipeToUpdate) {
                    stockingPipe.updateCache();
                    break;
                }
                i++;
            }
        }
        if (this.cacheChanged && this.cacheCooldown <= 0) {
            List<ServerPlayer> playerList = level.getPlayers(player -> player.containerMenu instanceof RequestMenu menu && menu.getNetworkPos().equals(this.getPos()));
            if (!playerList.isEmpty()) {
                ClientBoundItemListPayload toSend = this.requestItemList(this.pos);
                for (ServerPlayer player : playerList) {
                    Services.LOADER_SERVICE.sendToClient(player, toSend);
                }
            }
            for (StockingPipeEntity stockingPipe : this.stockingPipes) {
                stockingPipe.updateCache();
            }
            this.cacheChanged = false;
            this.cacheCooldown = DEFAULT_COOLDOWN;
        } else if (this.cacheCooldown > 0) {
            this.cacheCooldown--;
        }
        this.getRequestedItems().removeIf(requestedItem -> {
            if (requestedItem.timedOut()) {
                requestedItem.sendMessage(level, Component.translatable("chat." + ClassicPipes.MOD_ID + ".timed_out", requestedItem.getAmountRemaining(), requestedItem.getStack().getItem().getDescription()).withStyle(ChatFormatting.RED));
                for (RecipePipeEntity craftingPipe : this.recipePipes) {
                    if (requestedItem.matches(craftingPipe.getResult())) {
                        craftingPipe.dropHeldItems(level, craftingPipe.getBlockPos());
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void resetRequests(ServerLevel level) {
        this.requestedItems.clear();
        for (RecipePipeEntity craftingPipe : this.recipePipes) {
            craftingPipe.dropHeldItems(level, craftingPipe.getBlockPos());
        }
    }

    public Set<RoutingPipeEntity> getRoutingPipes() {
        return this.routingPipes;
    }

    public Set<NetworkedPipeEntity> getDefaultRoutes() {
        return this.defaultRoutes;
    }

    public Set<StockingPipeEntity> getStockingPipes() {
        return this.stockingPipes;
    }

    public Set<MatchingPipe> getMatchingPipes() {
        return this.matchingPipes;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void addPipe(NetworkedPipeEntity pipe) {
        if (pipe.isDefaultRoute()) {
            this.defaultRoutes.add(pipe);
        }
        if (pipe instanceof RoutingPipeEntity routingPipe) {
            this.routingPipes.add(routingPipe);
        }
        if (pipe instanceof ProviderPipe providerPipe) {
            this.providerPipes.add(providerPipe);
        }
        if (pipe instanceof StockingPipeEntity stockingPipe) {
            this.stockingPipes.add(stockingPipe);
        }
        if (pipe instanceof MatchingPipe matchingPipe) {
            this.matchingPipes.add(matchingPipe);
        }
        if (pipe instanceof RecipePipeEntity recipePipe) {
            this.recipePipes.add(recipePipe);
        }
    }

    public void removePipe(ServerLevel level, NetworkedPipeEntity pipe) {
        if (pipe.isDefaultRoute()) {
            this.defaultRoutes.remove(pipe);
        }
        if (pipe instanceof RoutingPipeEntity routingPipe) {
            this.routingPipes.remove(routingPipe);
        }
        if (pipe instanceof ProviderPipe providerPipe) {
            this.providerPipes.remove(providerPipe);
        }
        if (pipe instanceof StockingPipeEntity stockingPipe) {
            this.stockingPipes.remove(stockingPipe);
        }
        if (pipe instanceof MatchingPipe matchingPipe) {
            this.matchingPipes.remove(matchingPipe);
        }
        if (pipe instanceof RecipePipeEntity recipePipe) {
            this.recipePipes.remove(recipePipe);
        }
        this.requestedItems.removeIf(requestedItem -> {
            if (requestedItem.getDestination().equals(pipe.getBlockPos())) {
                requestedItem.sendMessage(level, Component.translatable("chat." + ClassicPipes.MOD_ID + ".destination_removed", requestedItem.getAmountRemaining(), requestedItem.getStack().getItem().getDescription(), pipe.getBlockPos().toShortString()).withStyle(ChatFormatting.RED));
                return true;
            }
            return false;
        });
    }

    public ClientBoundItemListPayload requestItemList(BlockPos requestPos) {
        List<ItemStack> existingItems = new ArrayList<>();
        for (ProviderPipe providerPipe : this.providerPipes) {
            for (ItemStack stack : providerPipe.getCache()) {
                boolean alreadyThere = false;
                for (ItemStack inStack : existingItems) {
                    if (ItemStack.isSameItemSameComponents(stack, inStack)) {
                        inStack.grow(stack.getCount());
                        if (inStack.getCount() < 0) {
                            inStack.setCount(Integer.MAX_VALUE);
                        }
                        alreadyThere = true;
                        break;
                    }
                }
                if (!alreadyThere) {
                    existingItems.add(stack.copy());
                }
            }
        }
        List<ItemStack> craftableItems = new ArrayList<>();
        for (RecipePipeEntity recipePipe : this.recipePipes) {
            ItemStack result = recipePipe.getResult();
            if (!result.isEmpty()) {
                boolean matched = false;
                for (ItemStack alreadyCraftable : craftableItems) {
                    if (ItemStack.isSameItemSameComponents(alreadyCraftable, result)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    craftableItems.add(result.copyWithCount(1));
                }
            }
        }
        return new ClientBoundItemListPayload(existingItems, craftableItems, this.sortingMode, this.pos, requestPos);
    }

    public void cacheUpdated() {
        this.cacheChanged = true;
    }

    public void setSortingMode(SortingMode sortingMode) {
        this.sortingMode = sortingMode;
    }

    public SortingMode getSortingMode() {
        return this.sortingMode;
    }

    public List<RequestedItem> getRequestedItems() {
        return this.requestedItems;
    }

    public void removeRequestedItem(RequestedItem requestedItem) {
        this.requestedItems.remove(requestedItem);
    }

    public void addRequestedItem(RequestedItem requestedItem) {
        this.requestedItems.add(requestedItem);
    }
}
