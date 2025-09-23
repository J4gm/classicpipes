package jagm.classicpipes.inventory.menu;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.blockentity.NetworkedPipeEntity;
import jagm.classicpipes.inventory.container.RequestMenuContainer;
import jagm.classicpipes.network.ClientBoundItemListPayload;
import jagm.classicpipes.network.ServerBoundSortingModePayload;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.SortingMode;
import jagm.classicpipes.util.Tuple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestMenu extends AbstractContainerMenu {

    private static final Pattern MOD_LOOKUP = Pattern.compile("@\\S+");
    private static final Pattern TAG_LOOKUP = Pattern.compile("#\\S+");

    private List<Tuple<ItemStack, Boolean>> networkItems;
    private final Container toDisplay;
    public final NonNullList<Slot> displaySlots = NonNullList.create();
    private final Map<ItemStack, Boolean> craftableCache;
    private String search;
    private int page;
    private int maxPage;
    private SortingMode sortingMode;
    private final BlockPos networkPos;
    private final BlockPos requestPos;
    private final BlockEntity controllerPipe;
    private final BlockEntity requestPipe;

    public RequestMenu(int id, Inventory inventory, ClientBoundItemListPayload payload) {
        super(ClassicPipes.REQUEST_MENU, id);
        this.networkItems = buildNetworkItems(payload.existingItems(), payload.craftableItems());
        this.craftableCache = new HashMap<>();
        this.networkPos = payload.networkPos();
        this.requestPos = payload.requestPos();
        this.sortingMode = payload.sortingMode();
        this.networkItems.sort(this.sortingMode.getComparator());
        this.toDisplay = new RequestMenuContainer();
        Level level = inventory.player.level();
        this.controllerPipe = level.getBlockEntity(this.networkPos);
        this.requestPipe = level.getBlockEntity(this.requestPos);
        this.search = "";
        this.page = 0;
        this.maxPage = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 9; j++) {
                displaySlots.add(new FilterSlot(this.toDisplay, i * 9 + j, 8 + j * 18, 36 + i * 18));
            }
        }
        this.updateSearch();
    }

    private static List<Tuple<ItemStack, Boolean>> buildNetworkItems(List<ItemStack> existingItems, List<ItemStack> craftableItems) {
        List<Tuple<ItemStack, Boolean>> networkItems = new ArrayList<>();
        for (ItemStack craftable : craftableItems) {
            // Craftable items should have a stack size of 1 when they get here.
            networkItems.add(new Tuple<>(craftable, true));
        }
        int craftableCount = networkItems.size();
        for (ItemStack stack : existingItems) {
            boolean matched = false;
            for (int i = 0; i < craftableCount; i++) {
                if (ItemStack.isSameItemSameComponents(stack, networkItems.get(i).a())) {
                    // Since the game doesn't like handling empty stacks, craftable stacks get one extra item, which is accounted for by the client screen.
                    networkItems.set(i, new Tuple<>(stack.copyWithCount(stack.getCount() + 1), true));
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                networkItems.add(new Tuple<>(stack, false));
            }
        }
        return networkItems;
    }

    public void update(List<ItemStack> existingItems, List<ItemStack> craftableItems) {
        this.networkItems = buildNetworkItems(existingItems, craftableItems);
        this.update();
    }

    public void update() {
        this.networkItems.sort(this.sortingMode.getComparator());
        this.updateSearch();
    }

    public void updateSearch() {
        this.toDisplay.clearContent();
        this.craftableCache.clear();
        int display = this.toDisplay.getContainerSize();
        List<Tuple<ItemStack, Boolean>> matchingItems = new ArrayList<>();
        Iterator<Tuple<ItemStack, Boolean>> iterator = this.networkItems.listIterator();
        while (iterator.hasNext()) {
            Tuple<ItemStack, Boolean> tuple = iterator.next();
            if (tuple.a().isEmpty()) {
                iterator.remove();
            } else if (this.search.isEmpty() || itemMatchesSearch(tuple.a(), this.search)) {
                matchingItems.add(tuple);
            }
        }
        this.maxPage = matchingItems.size() / display;
        if (this.page > this.maxPage) {
            this.page = this.maxPage;
        }
        int index = 0;
        for (Tuple<ItemStack, Boolean> tuple : matchingItems) {
            if (index >= (this.page + 1) * display) {
                break;
            }
            if (index >= this.page * display) {
                int slot = index % display;
                this.toDisplay.setItem(slot, tuple.a());
                this.craftableCache.put(tuple.a(), tuple.b());
            }
            index++;
        }
    }

    public int getMaxPage() {
        return this.maxPage;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.controllerPipe instanceof NetworkedPipeEntity controller && this.requestPipe instanceof NetworkedPipeEntity requester) {
            return player.level().getBlockEntity(controller.getBlockPos()) == controller && Container.stillValidBlockEntity(requester, player) && controller.isController() && controller.getNetwork() == requester.getNetwork();
        }
        return false;
    }

    public String getSearch() {
        return this.search;
    }

    public SortingMode getSortingMode() {
        return this.sortingMode;
    }

    public void setSortingMode(SortingMode sortingMode) {
        this.sortingMode = sortingMode;
        Services.LOADER_SERVICE.sendToServer(new ServerBoundSortingModePayload(sortingMode));
        this.networkItems.sort(this.sortingMode.getComparator());
        this.updateSearch();
    }

    public void setSearch(String search) {
        this.search = search;
        this.updateSearch();
    }

    public void changePage(int increment) {
        this.page += increment;
        if (this.page < 0) {
            this.page = 0;
        } else if (this.page > this.maxPage) {
            this.page = this.maxPage;
        }
        this.updateSearch();
    }

    public int getPage() {
        return this.page;
    }

    public BlockPos getNetworkPos() {
        return this.networkPos;
    }

    public BlockPos getRequestPos() {
        return this.requestPos;
    }

    public boolean itemCraftable(ItemStack stack) {
        return this.craftableCache.get(stack);
    }

    private static boolean itemMatchesSearch(ItemStack stack, String search) {
        Matcher modMatcher = MOD_LOOKUP.matcher(search);
        if (modMatcher.find()) {
            String match = modMatcher.group();
            search = search.replace(match, "");
            String searchedMod = normalise(match.replaceFirst("@", ""));
            String itemModID = MiscUtil.modFromItem(stack);
            String itemModName = normalise(Services.LOADER_SERVICE.getModName(itemModID));
            if (!normalise(itemModID).contains(searchedMod) && !itemModName.contains(searchedMod)) {
                return false;
            }
        }
        Matcher tagMatcher = TAG_LOOKUP.matcher(search);
        while (tagMatcher.find()) {
            boolean foundTag = false;
            String match = tagMatcher.group();
            search = search.replace(match, "");
            for (TagKey<Item> tag : stack.getTags().toList()) {
                String searchedTag = normalise(match.replaceFirst("#", ""));
                String itemTag = normalise(tag.location().toString());
                if (itemTag.contains(searchedTag)) {
                    foundTag = true;
                    break;
                }
            }
            if (!foundTag) {
                return false;
            }
        }
        search = normalise(search);
        String itemName = normalise(stack.getDisplayName().getString());
        return itemName.contains(search);
    }

    private static String normalise(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

}
