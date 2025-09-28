package jagm.classicpipes.util;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.*;
import jagm.classicpipes.inventory.menu.RequestMenu;
import jagm.classicpipes.network.ClientBoundItemListPayload;
import jagm.classicpipes.services.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
    private final List<Tuple<ProviderPipe, RequestedItem>> queue;
    private final Map<ProviderPipe, List<ItemStack>> takenFromCache;
    private final List<ItemStack> spareItems;
    private final Set<Item> craftedItemsForAdvancement;

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
        this.queue = new ArrayList<>();
        this.takenFromCache = new HashMap<>();
        this.spareItems = new ArrayList<>();
        this.craftedItemsForAdvancement = new HashSet<>();
    }

    public PipeNetwork(BlockPos pos) {
        this(pos, SortingMode.AMOUNT_DESCENDING);
    }

    private void enqueue(ItemStack stack, int amount, BlockPos requestPos, String playerName, ProviderPipe providerPipe) {
        RequestedItem requestedItem = new RequestedItem(stack.copyWithCount(amount), requestPos, playerName);
        boolean matched = false;
        for (Tuple<ProviderPipe, RequestedItem> tuple : this.queue) {
            if (tuple.a() == providerPipe && requestedItem.matches(tuple.b())) {
                matched = true;
                tuple.b().getStack().grow(amount);
                break;
            }
        }
        if (!matched) {
            this.queue.add(new Tuple<>(providerPipe, requestedItem));
        }
    }

    private MissingItem queueRequest(ItemStack stack, BlockPos requestPos, Player player, List<ItemStack> visited) {
        MissingItem missingItem = new MissingItem(stack.copy());
        String playerName = player != null ? player.getName().getString() : "";
        Iterator<ItemStack> iterator = this.spareItems.listIterator();
        while (iterator.hasNext()) {
            ItemStack spareItem = iterator.next();
            if (ItemStack.isSameItemSameTags(spareItem, stack)) {
                int amount = Math.min(spareItem.getCount(), missingItem.getCount());
                spareItem.shrink(amount);
                missingItem.shrink(amount);
                if (spareItem.isEmpty()) {
                    iterator.remove();
                }
                this.enqueue(stack, amount, requestPos, playerName, null);
                break;
            }
        }
        if (!missingItem.isEmpty()) {
            for (ProviderPipe providerPipe : this.providerPipes) {
                if (missingItem.isEmpty()) {
                    break;
                }
                List<ItemStack> alreadyTaken = this.takenFromCache.containsKey(providerPipe) ? this.takenFromCache.get(providerPipe) : new ArrayList<>();
                for (ItemStack cacheStack : providerPipe.getCache()) {
                    if (ItemStack.isSameItemSameTags(stack, cacheStack)) {
                        int cacheCount = cacheStack.getCount();
                        int takenIndex = -1;
                        for (int i = 0; i < alreadyTaken.size(); i++) {
                            ItemStack takenStack = alreadyTaken.get(i);
                            if (ItemStack.isSameItemSameTags(cacheStack, takenStack)) {
                                cacheCount -= takenStack.getCount();
                                takenIndex = i;
                                break;
                            }
                        }
                        int amount = Math.min(missingItem.getCount(), cacheCount);
                        if (amount > 0) {
                            this.enqueue(stack, amount, requestPos, playerName, providerPipe);
                            missingItem.shrink(amount);
                            if (takenIndex >= 0) {
                                ItemStack takenStack = alreadyTaken.get(takenIndex);
                                alreadyTaken.set(takenIndex, takenStack.copyWithCount(takenStack.getCount() + amount));
                            } else {
                                alreadyTaken.add(cacheStack.copyWithCount(amount));
                            }
                            this.takenFromCache.put(providerPipe, alreadyTaken);
                        }
                        break;
                    }
                }
            }
        }
        if (!missingItem.isEmpty()) {
            boolean foundCraftingPipe = false;
            for (RecipePipeEntity craftingPipe : this.recipePipes) {
                ItemStack result = craftingPipe.getResult();
                if (ItemStack.isSameItemSameTags(result, stack)) {
                    if (foundCraftingPipe) {
                        if (player != null) {
                            player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".multiple_recipes", stack.getItem().getDescription()).withStyle(ChatFormatting.YELLOW), false);
                        }
                        break;
                    }
                    int requiredCrafts = missingItem.getCount() / result.getCount() + (missingItem.getCount() % result.getCount() != 0 ? 1 : 0);
                    List<ItemStack> ingredients = craftingPipe.getIngredientsCollated();
                    boolean canCraft = true;
                    for (ItemStack ingredient : ingredients) {
                        boolean alreadyVisited = false;
                        for (ItemStack visitedStack : visited) {
                            if (ItemStack.isSameItemSameTags(visitedStack, ingredient)) {
                                alreadyVisited = true;
                                canCraft = false;
                                break;
                            }
                        }
                        if (!alreadyVisited) {
                            List<ItemStack> visited2 = new ArrayList<>(visited);
                            visited2.add(stack);
                            MissingItem missingForCraft = this.queueRequest(ingredient.copyWithCount(ingredient.getCount() * requiredCrafts), craftingPipe.getBlockPos(), player, visited2);
                            if (!missingForCraft.isEmpty()) {
                                missingItem.addMissingIngredient(missingForCraft);
                                canCraft = false;
                            }
                        }
                    }
                    if (canCraft) {
                        int amount = Math.min(result.getCount() * requiredCrafts, missingItem.getCount());
                        missingItem.shrink(amount);
                        this.enqueue(stack, amount, requestPos, playerName, null);
                        if (result.getCount() * requiredCrafts > amount) {
                            int remaining = result.getCount() * requiredCrafts - amount;
                            boolean matched = false;
                            for (ItemStack spareItem : this.spareItems) {
                                if (ItemStack.isSameItemSameTags(spareItem, stack)) {
                                    spareItem.grow(remaining);
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) {
                                this.spareItems.add(stack.copyWithCount(remaining));
                            }
                        }
                        this.craftedItemsForAdvancement.add(result.getItem());
                    }
                    foundCraftingPipe = true;
                }
            }
        }
        return missingItem;
    }

    public void request(ServerLevel level, ItemStack stack, BlockPos requestPos, Player player, boolean partialRequest) {
        MissingItem missingItem = this.queueRequest(stack.copy(), requestPos, player, new ArrayList<>());
        if (missingItem.isEmpty()) {
            boolean cancelled = false;
            for (Tuple<ProviderPipe, RequestedItem> tuple : this.queue) {
                this.addRequestedItem(tuple.b());
                if (tuple.a() != null && !tuple.a().extractItem(level, tuple.b().getStack())) {
                    cancelled = true;
                    if (!partialRequest) {
                        tuple.b().sendMessage(level, Component.translatable("chat." + ClassicPipes.MOD_ID + ".could_not_extract", stack.getCount(), stack.getItem().getDescription(), tuple.a().getProviderPipePos().toShortString()).withStyle(ChatFormatting.RED));
                    }
                    break;
                }
            }
            if (cancelled) {
                this.queue.forEach(tuple -> this.removeRequestedItem(tuple.b()));
            } else if (player != null) {
                ClassicPipes.REQUEST_ITEM_TRIGGER.trigger((ServerPlayer) player, stack, this.craftedItemsForAdvancement.size());
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".requested", stack.getCount(), stack.getItem().getDescription()).withStyle(ChatFormatting.GREEN), false);
            }
        } else if (partialRequest && missingItem.getCount() < stack.getCount()) {
            this.resetForNewRequest();
            this.request(level, stack.copyWithCount(stack.getCount() - missingItem.getCount()), requestPos, player, false);
            return;
        } else if (player != null) {
            player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.a", stack.getCount(), stack.getItem().getDescription()).withStyle(ChatFormatting.RED), false);
            for (ItemStack missing : missingItem.getBaseItems(new ArrayList<>())) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_item.b", missing.getCount(), missing.getItem().getDescription()).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        this.resetForNewRequest();
    }

    private void resetForNewRequest() {
        this.queue.clear();
        this.takenFromCache.clear();
        this.spareItems.clear();
        this.craftedItemsForAdvancement.clear();
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
                if (stockingPipe.isActiveStocking()) {
                    stockingPipe.tryRequests(level);
                }
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
                    if (ItemStack.isSameItemSameTags(stack, inStack)) {
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
                    if (ItemStack.isSameItemSameTags(alreadyCraftable, result)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    craftableItems.add(result.copyWithCount(1));
                }
            }
        }
        return new ClientBoundItemListPayload(existingItems, craftableItems, this.pos, requestPos, this.sortingMode);
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
