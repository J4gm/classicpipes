package jagm.classicpipes.blockentity;

import com.mojang.serialization.Codec;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.NetworkedPipeBlock;
import jagm.classicpipes.block.RecipePipeBlock;
import jagm.classicpipes.inventory.container.Filter;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

public class RecipePipeEntity extends NetworkedPipeEntity implements MenuProvider {

    private static final byte DEFAULT_COOLDOWN = 8;

    private final FilterContainer filter;
    private final Direction[] slotDirections;
    private final List<List<ItemStack>> heldItems;
    private int waitingForCraft;
    private boolean crafterTicked;
    private byte cooldown;
    private boolean blockingMode;

    public RecipePipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.RECIPE_PIPE_ENTITY, pos, state);
        this.filter = new FilterContainer(this, 10, true);
        this.slotDirections = new Direction[10];
        List<Direction> buttonDirections = this.getDirectionsForButtons(state);
        Arrays.fill(this.slotDirections, buttonDirections.isEmpty() ? Direction.DOWN : buttonDirections.getFirst());
        this.heldItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            List<ItemStack> list = new ArrayList<>();
            this.heldItems.add(list);
        }
        this.blockingMode = true;
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
        if (this.crafterTicked && this.hasNetwork()) {
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
            if (container instanceof CrafterBlockEntity crafter) {
                if (this.isEmpty()) {
                    level.scheduleTick(crafterPos, crafter.getBlockState().getBlock(), 0);
                    level.playSound(null, crafterPos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS);
                    this.crafterTicked = true;
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
                    this.slotDirections[i] = buttonDirections.getFirst();
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
            if (!(stack.getItem() instanceof LabelItem) && ItemStack.isSameItemSameComponents(slotStack, stack) || slotStack.getItem() instanceof LabelItem labelItem && labelItem.itemMatches(slotStack, stack)) {
                matchingSlots.add(slot);
            }
        }
        if (!matchingSlots.isEmpty()) {
            while (!stack.isEmpty()) {
                int minSlot = matchingSlots.getFirst();
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
                Map<Direction, CrafterBlockEntity> crafters = new HashMap<>();
                for (int i = 0; i < readyToCraft; i++) {
                    for (int slot = 0; slot < 9; slot++) {
                        ItemStack ingredient = this.filter.getItem(slot);
                        if (crafters.containsKey(this.slotDirections[slot])) {
                            crafters.get(this.slotDirections[slot]).setSlotState(slot, !ingredient.isEmpty());
                        } else if (this.getLevel() != null && this.getLevel().getBlockEntity(this.getBlockPos().relative(this.slotDirections[slot])) instanceof CrafterBlockEntity crafter) {
                            crafters.put(this.slotDirections[slot], crafter);
                            crafter.setSlotState(slot, !ingredient.isEmpty());
                        }
                        if (!ingredient.isEmpty()) {
                            int ingredientRemaining = ingredient.getCount();
                            while (ingredientRemaining > 0) {
                                ItemStack heldStack = this.heldItems.get(slot).getFirst();
                                int amountToTake = Math.min(heldStack.getCount(), ingredientRemaining);
                                ClassicPipes.LOGGER.info(heldStack.getItemName().getString());
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
                                    this.heldItems.get(slot).removeFirst();
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
        if (!stack.isEmpty() && this.waitingForCraft > 0 && item.getFromDirection().equals(this.slotDirections[9]) && ItemStack.isSameItemSameComponents(this.getResult(), stack)) {
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
    protected void loadAdditional(ValueInput valueInput) {
        this.filter.clearContent();
        this.heldItems.forEach(List::clear);
        super.loadAdditional(valueInput);
        ValueInput.TypedInputList<Byte> directionsByteList = valueInput.listOrEmpty("slot_directions", Codec.BYTE);
        int i = 0;
        for (byte directionByte : directionsByteList) {
            if (i >= 10) {
                break;
            }
            this.slotDirections[i] = Direction.from3DDataValue(directionByte);
            i++;
        }
        ValueInput.TypedInputList<ItemStackWithSlot> filterList = valueInput.listOrEmpty("filter", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : filterList) {
            this.filter.setItem(slotStack.slot(), slotStack.stack());
        }
        ValueInput.TypedInputList<List<ItemStack>> heldItemList = valueInput.listOrEmpty("held_items", ExtraCodecs.compactListCodec(MiscUtil.UNLIMITED_STACK_CODEC));
        i = 0;
        for (List<ItemStack> list : heldItemList) {
            this.heldItems.set(i, new ArrayList<>(list));
            i++;
            if (i >= 9) {
                break;
            }
        }
        this.waitingForCraft = valueInput.getIntOr("waiting_for_craft", 0);
        this.crafterTicked = valueInput.getBooleanOr("crafter_ticked", false);
        this.cooldown = valueInput.getByteOr("cooldown", DEFAULT_COOLDOWN);
        this.blockingMode = valueInput.getBooleanOr("blocking_mode", true);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        ValueOutput.TypedOutputList<Byte> directionsByteList = valueOutput.list("slot_directions", Codec.BYTE);
        for (Direction direction : this.slotDirections) {
            directionsByteList.add(direction == null ? (byte) 0 : (byte) direction.get3DDataValue());
        }
        ValueOutput.TypedOutputList<ItemStackWithSlot> filterList = valueOutput.list("filter", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < this.filter.getContainerSize(); slot++) {
            ItemStack stack = this.filter.getItem(slot);
            if (!stack.isEmpty()) {
                filterList.add(new ItemStackWithSlot(slot, stack));
            }
        }
        ValueOutput.TypedOutputList<List<ItemStack>> heldItemList = valueOutput.list("held_items", ExtraCodecs.compactListCodec(MiscUtil.UNLIMITED_STACK_CODEC));
        for (List<ItemStack> list : this.heldItems) {
            heldItemList.add(list);
        }
        valueOutput.putInt("waiting_for_craft", this.waitingForCraft);
        valueOutput.putBoolean("crafter_ticked", this.crafterTicked);
        valueOutput.putByte("cooldown", this.cooldown);
        valueOutput.putBoolean("blocking_mode", this.blockingMode);
    }

    public Filter getFilter() {
        return this.filter;
    }

    public void setBlockingMode(boolean blockingMode) {
        this.blockingMode = blockingMode;
    }

    public boolean isBlockingMode() {
        return this.blockingMode;
    }
}
