package jagm.classicpipes.blockentity;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.NetworkedPipeBlock;
import jagm.classicpipes.block.RecipePipeBlock;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.item.LabelItem;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.RequestedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class RecipePipeEntity extends NetworkedPipeEntity implements MenuProvider {

    private static final byte DEFAULT_COOLDOWN = 8;

    private final FilterContainer filter;
    private final Direction[] slotDirections;
    private final List<List<ItemStack>> heldItems;
    private final boolean quarkInstalled;
    private int waitingForCraft;
    private boolean crafterTicked;
    private byte cooldown;
    private boolean blockingMode;

    public RecipePipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.RECIPE_PIPE_ENTITY, pos, state);
        this.filter = new FilterContainer(this, 10, true);
        this.slotDirections = new Direction[10];
        List<Direction> buttonDirections = this.getDirectionsForButtons(state);
        Arrays.fill(this.slotDirections, buttonDirections.isEmpty() ? Direction.DOWN : buttonDirections.get(0));
        this.heldItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            List<ItemStack> list = new ArrayList<>();
            this.heldItems.add(list);
        }
        this.blockingMode = true;
        this.quarkInstalled = Services.LOADER_SERVICE.quarkInstalled();
    }

    public Direction[] getSlotDirections() {
        return this.slotDirections;
    }

    public void setSlotDirection(int slot, Direction direction) {
        this.slotDirections[slot] = direction;
    }

    @Override
    public void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        BlockPos crafterPos = pos.relative(this.slotDirections[9]);
        if (quarkInstalled && this.crafterTicked && this.hasNetwork()) {
            for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
                if (requestedItem.matches(this.getResult())) {
                    requestedItem.sendMessage(level, Component.translatable("chat." + ClassicPipes.MOD_ID + ".crafter_jammed", crafterPos.toShortString()).withStyle(ChatFormatting.RED));
                }
            }
            this.getNetwork().resetRequests(level);
            this.crafterTicked = false;
            this.waitingForCraft = 0;
            this.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        } else if (this.waitingForCraft > 0) {
            BlockEntity container = level.getBlockEntity(crafterPos);
            if (quarkInstalled && Services.LOADER_SERVICE.isQuarkCrafter(container)) {
                if (this.isEmpty()) {
                    this.crafterTicked = true;
                    Services.LOADER_SERVICE.quarkCrafterCraft(level, container);
                    level.playSound(null, crafterPos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS);
                }
            } else if (this.cooldown-- <= 0) {
                if (!(container instanceof ItemPipeEntity) && Services.LOADER_SERVICE.extractSpecificItem(this, level, crafterPos, this.slotDirections[9].getOpposite(), this.getResult().copyWithCount(1))) {
                    level.sendBlockUpdated(pos, state, state, 2);
                    this.setChanged();
                }
                this.cooldown = DEFAULT_COOLDOWN;
            }
        } else if (!this.queued.isEmpty()) {
            this.addQueuedItems(level, false);
        }
    }

    @Override
    public void update(ServerLevel level, BlockState state, BlockPos pos, Direction direction, boolean wasConnected) {
        super.update(level, state, pos, direction, wasConnected);
        this.checkSlotDirections();
    }

    @Override
    protected void initialiseNetworking(ServerLevel level, BlockState state, BlockPos pos) {
        super.initialiseNetworking(level, state, pos);
        this.checkSlotDirections();
    }

    private void checkSlotDirections() {
        List<Direction> buttonDirections = this.getDirectionsForButtons(this.getBlockState());
        if (!buttonDirections.isEmpty()) {
            for (int i = 0; i < this.slotDirections.length; i++) {
                if (!buttonDirections.contains(this.slotDirections[i])) {
                    this.slotDirections[i] = buttonDirections.get(0);
                    this.setChanged();
                }
            }
        }
    }

    @Override
    public void eject(ServerLevel level, BlockPos pos, ItemInPipe item) {
        List<Integer> matchingSlots = new ArrayList<>();
        ItemStack stack = item.getStack().copy();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack slotStack = this.filter.getItem(slot);
            if (!(stack.getItem() instanceof LabelItem) && ItemStack.isSameItemSameTags(slotStack, stack) || slotStack.getItem() instanceof LabelItem labelItem && labelItem.itemMatches(slotStack, stack)) {
                matchingSlots.add(slot);
            }
        }
        if (!matchingSlots.isEmpty()) {
            while (!stack.isEmpty()) {
                int minSlot = matchingSlots.get(0);
                int minAmount = 0;
                for (ItemStack heldStack : this.heldItems.get(minSlot)) {
                    minAmount += heldStack.getCount();
                }
                for (int slot : matchingSlots) {
                    int slotAmount = 0;
                    for (ItemStack heldStack : this.heldItems.get(slot)) {
                        slotAmount += heldStack.getCount();
                    }
                    if (slotAmount < minAmount) {
                        minSlot = slot;
                        minAmount = slotAmount;
                    }
                }
                MiscUtil.mergeStackIntoList(this.heldItems.get(minSlot), stack.copyWithCount(1));
                stack.shrink(1);
            }
        } else {
            super.eject(level, pos, item);
        }
        this.attemptCraft();
        this.setChanged();
    }

    private void attemptCraft() {
        if (this.waitingForCraft == 0 || !this.blockingMode) {
            int readyToCraft = Integer.MAX_VALUE;
            for (int slot = 0; slot < 9; slot++) {
                if (!this.filter.getItem(slot).isEmpty()) {
                    int heldAmount = 0;
                    for (ItemStack heldStack : this.heldItems.get(slot)) {
                        heldAmount += heldStack.getCount();
                    }
                    readyToCraft = Math.min(readyToCraft, heldAmount / this.filter.getItem(slot).getCount());
                }
                BlockState state = this.getBlockState();
                if (!this.filter.getItem(slot).isEmpty() && !state.getValue(RecipePipeBlock.PROPERTY_BY_DIRECTION.get(this.slotDirections[slot])).equals(NetworkedPipeBlock.ConnectionState.UNLINKED)) {
                    readyToCraft = 0;
                    if (this.getLevel() instanceof ServerLevel serverLevel && this.hasNetwork()) {
                        for (RequestedItem requestedItem : this.getNetwork().getRequestedItems()) {
                            if (requestedItem.matches(this.getResult())) {
                                requestedItem.sendMessage(serverLevel, Component.translatable("chat." + ClassicPipes.MOD_ID + ".missing_recipe_pipe_direction", this.getBlockPos().toShortString()).withStyle(ChatFormatting.RED));
                            }
                        }
                        this.crafterTicked = false;
                        this.waitingForCraft = 0;
                        this.getNetwork().resetRequests(serverLevel);
                        this.setChanged();
                        serverLevel.sendBlockUpdated(this.getBlockPos(),state, state, 2);
                    }
                    break;
                }
            }
            if (readyToCraft > 0 && readyToCraft < Integer.MAX_VALUE) {
                Map<Direction, BlockEntity> crafters = new HashMap<>();
                for (int i = 0; i < (this.blockingMode ? 1 : readyToCraft); i++) {
                    for (int slot = 0; slot < 9; slot++) {
                        ItemStack ingredient = this.filter.getItem(slot);
                        if (quarkInstalled) {
                            if (crafters.containsKey(this.slotDirections[slot])) {
                                Services.LOADER_SERVICE.setQuarkCrafterSlotState(crafters.get(this.slotDirections[slot]), slot, !ingredient.isEmpty());
                            } else if (this.getLevel() != null) {
                                BlockEntity container = this.getLevel().getBlockEntity(this.getBlockPos().relative(this.slotDirections[slot]));
                                if (Services.LOADER_SERVICE.isQuarkCrafter(container)) {
                                    crafters.put(this.slotDirections[slot], container);
                                    Services.LOADER_SERVICE.setQuarkCrafterSlotState(container, slot, !ingredient.isEmpty());
                                }
                            }
                        }
                        if (!ingredient.isEmpty()) {
                            int ingredientRemaining = ingredient.getCount();
                            while (ingredientRemaining > 0) {
                                ItemStack heldStack = this.heldItems.get(slot).get(0);
                                int amountToTake = Math.min(heldStack.getCount(), ingredientRemaining);
                                ClassicPipes.LOGGER.info(heldStack.getItem().getDescription().getString());
                                this.queued.add(new ItemInPipe(
                                        heldStack.copyWithCount(amountToTake),
                                        ItemInPipe.DEFAULT_SPEED,
                                        ItemInPipe.HALFWAY,
                                        Direction.DOWN,
                                        this.slotDirections[slot],
                                        false,
                                        (short) 0
                                ));
                                heldStack.shrink(amountToTake);
                                if (heldStack.isEmpty()) {
                                    this.heldItems.get(slot).remove(this.heldItems.get(slot).size() - 1);
                                }
                                ingredientRemaining -= amountToTake;
                            }
                        }
                    }
                    this.waitingForCraft += this.getResult().getCount();
                }
            }
        }
    }

    public ItemStack getResult() {
        return this.filter.getItem(9).copy();
    }

    public List<ItemStack> getIngredients() {
        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack ingredient = this.filter.getItem(i).copy();
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }
        return ingredients;
    }

    public List<ItemStack> getIngredientsCollated() {
        List<ItemStack> collated = new ArrayList<>();
        for (ItemStack ingredient : this.getIngredients()) {
            MiscUtil.mergeStackIntoList(collated, ingredient);
        }
        return collated;
    }

    public List<List<ItemStack>> getHeldItems() {
        return heldItems;
    }

    public void dropHeldItems(ServerLevel serverLevel, BlockPos pos) {
        for (List<ItemStack> list : this.heldItems) {
            for (ItemStack stack : list) {
                if (!stack.isEmpty()) {
                    ItemEntity droppedItem = new ItemEntity(serverLevel, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, stack);
                    droppedItem.setDefaultPickUpDelay();
                    serverLevel.addFreshEntity(droppedItem);
                }
            }
        }
        this.heldItems.forEach(List::clear);
    }

    @Override
    public void disconnect(ServerLevel level) {
        this.dropHeldItems(level, this.getBlockPos());
        super.disconnect(level);
    }

    @Override
    public void setRemoved() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            super.disconnect(serverLevel);
        }
        this.remove = true;
    }

    @Override
    public void insertPipeItem(Level level, ItemInPipe item) {
        ItemStack stack = item.getStack();
        if (!stack.isEmpty() && this.waitingForCraft > 0 && item.getFromDirection().equals(this.slotDirections[9]) && ItemStack.isSameItemSameTags(this.getResult(), stack)) {
            this.waitingForCraft -= stack.getCount();
            this.crafterTicked = false;
            if (this.waitingForCraft <= 0) {
                this.waitingForCraft = 0;
                this.attemptCraft();
            }
        }
        super.insertPipeItem(level, item);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + ClassicPipes.MOD_ID + ".recipe_pipe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RecipePipeMenu(id, playerInventory, this.filter, this.slotDirections, this.getDirectionsForButtons(this.getBlockState()), this.getBlockPos(), this.blockingMode);
    }

    public List<Direction> getDirectionsForButtons(BlockState state) {
        List<Direction> availableDirections = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (state.getValue(NetworkedPipeBlock.PROPERTY_BY_DIRECTION.get(direction)).equals(NetworkedPipeBlock.ConnectionState.UNLINKED)) {
                availableDirections.add(direction);
            }
        }
        return availableDirections;
    }

    @Override
    public void load(CompoundTag valueInput) {
        this.filter.clearContent();
        this.heldItems.forEach(List::clear);
        super.load(valueInput);
        byte[] directionsByteList = valueInput.getByteArray("slot_directions");
        int i = 0;
        for (byte directionByte : directionsByteList) {
            if (i >= 10) {
                break;
            }
            this.slotDirections[i] = Direction.from3DDataValue(directionByte);
            i++;
        }
        ListTag filterList = valueInput.getList("filter", ListTag.TAG_COMPOUND);
        filterList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getInt("slot");
                MiscUtil.loadFromTag(tag, ItemStack.CODEC, stack -> this.filter.setItem(slot, stack));
            }
        });
        ListTag heldItemList = valueInput.getList("held_items", ListTag.TAG_COMPOUND);
        heldItemList.forEach(tag -> {
            if (tag instanceof CompoundTag compoundTag) {
                int slot = compoundTag.getInt("slot");
                if (slot >= 0 && slot < 9) {
                    List<ItemStack> stacks = new ArrayList<>();
                    ListTag stacksList = compoundTag.getList("stacks", ListTag.TAG_LIST);
                    for (Tag stackTag: stacksList) {
                        MiscUtil.loadFromTag(stackTag, ItemStack.CODEC, stacks::add);
                    }
                    this.heldItems.set(slot, stacks);
                }
            }
        });
        this.waitingForCraft = valueInput.getInt("waiting_for_craft");
        if (quarkInstalled) {
            this.crafterTicked = valueInput.getBoolean("crafter_ticked");
        }
        this.cooldown = valueInput.getByte("cooldown");
        this.blockingMode = valueInput.getBoolean("blocking_mode");
    }

    @Override
    protected void saveAdditional(CompoundTag valueOutput) {
        super.saveAdditional(valueOutput);
        byte[] directionsByteArray = new byte[10];
        for (int i = 0; i < this.slotDirections.length; i++) {
            directionsByteArray[i] = this.slotDirections[i] == null ? (byte) 0 : (byte) this.slotDirections[i].get3DDataValue();
        }
        valueOutput.putByteArray("slot_directions", directionsByteArray);
        ListTag filterList = new ListTag();
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("slot", slot);
                MiscUtil.saveToTag(tag, stack, ItemStack.CODEC, filterList::add);
            }
        }
        valueOutput.put("filter", filterList);
        ListTag heldItemList = new ListTag();
        for (int slot = 0; slot < this.heldItems.size(); slot++) {
            List<ItemStack> stacks = this.heldItems.get(slot);
            ListTag stacksList = new ListTag();
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) {
                    CompoundTag stackTag = new CompoundTag();
                    MiscUtil.saveToTag(stackTag, stack, ItemStack.CODEC, stacksList::add);
                }
            }
            CompoundTag tag = new CompoundTag();
            tag.putInt("slot", slot);
            tag.put("stacks", stacksList);
            heldItemList.add(tag);
        }
        valueOutput.put("held_items", heldItemList);
        valueOutput.putInt("waiting_for_craft", this.waitingForCraft);
        if (quarkInstalled) {
            valueOutput.putBoolean("crafter_ticked", this.crafterTicked);
        }
        valueOutput.putByte("cooldown", this.cooldown);
        valueOutput.putBoolean("blocking_mode", this.blockingMode);
    }

    public void setBlockingMode(boolean blockingMode) {
        this.blockingMode = blockingMode;
    }

    public boolean isBlockingMode() {
        return this.blockingMode;
    }
}
