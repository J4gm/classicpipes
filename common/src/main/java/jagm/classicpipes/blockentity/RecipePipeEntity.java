package jagm.classicpipes.blockentity;

import com.mojang.serialization.Codec;
import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.block.NetworkedPipeBlock;
import jagm.classicpipes.block.RecipePipeBlock;
import jagm.classicpipes.inventory.container.Filter;
import jagm.classicpipes.inventory.container.FilterContainer;
import jagm.classicpipes.inventory.menu.RecipePipeMenu;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.ItemInPipe;
import jagm.classicpipes.util.MiscUtil;
import jagm.classicpipes.util.RequestedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private final NonNullList<ItemStack> heldItems;
    private int waitingForCraft;
    private boolean crafterTicked;
    private byte cooldown;

    public RecipePipeEntity(BlockPos pos, BlockState state) {
        super(ClassicPipes.RECIPE_PIPE_ENTITY, pos, state);
        this.filter = new FilterContainer(this, 10, true);
        this.slotDirections = new Direction[10];
        List<Direction> buttonDirections = this.getDirectionsForButtons(state);
        Arrays.fill(this.slotDirections, buttonDirections.isEmpty() ? Direction.DOWN : buttonDirections.getFirst());
        this.heldItems = NonNullList.withSize(9, ItemStack.EMPTY);
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
        for (int slot = 0; slot < 9; slot++) {
            if (ItemStack.isSameItemSameComponents(this.filter.getItem(slot), item.getStack())) {
                matchingSlots.add(slot);
            }
        }
        if (!matchingSlots.isEmpty()) {
            ItemStack stack = item.getStack().copy();
            while (!stack.isEmpty()) {
                int minSlot = matchingSlots.getFirst();
                int minAmount = this.heldItems.get(minSlot).getCount();
                for (int slot : matchingSlots) {
                    int slotAmount = this.heldItems.get(slot).getCount();
                    if (slotAmount < minAmount) {
                        minSlot = slot;
                        minAmount = slotAmount;
                    }
                }
                this.heldItems.set(minSlot, stack.copyWithCount(this.heldItems.get(minSlot).getCount() + 1));
                stack.shrink(1);
            }
        } else {
            super.eject(level, pos, item);
        }
        attemptCraft();
        this.setChanged();
    }

    private void attemptCraft() {
        if (this.waitingForCraft == 0) {
            boolean readyToCraft = true;
            for (int slot = 0; slot < 9; slot++) {
                if (this.heldItems.get(slot).getCount() < this.filter.getItem(slot).getCount()) {
                    readyToCraft = false;
                    break;
                }
                BlockState state = this.getBlockState();
                if (!this.filter.getItem(slot).isEmpty() && !state.getValue(RecipePipeBlock.PROPERTY_BY_DIRECTION.get(this.slotDirections[slot])).equals(NetworkedPipeBlock.ConnectionState.UNLINKED)) {
                    readyToCraft = false;
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
            if (readyToCraft) {
                Map<Direction, CrafterBlockEntity> crafters = new HashMap<>();
                for (int slot = 0; slot < 9; slot++) {
                    ItemStack ingredient = this.filter.getItem(slot);
                    if (crafters.containsKey(this.slotDirections[slot])) {
                        crafters.get(this.slotDirections[slot]).setSlotState(slot, !ingredient.isEmpty());
                    } else if (this.getLevel() != null && this.getLevel().getBlockEntity(this.getBlockPos().relative(this.slotDirections[slot])) instanceof CrafterBlockEntity crafter) {
                        crafters.put(this.slotDirections[slot], crafter);
                        crafter.setSlotState(slot, !ingredient.isEmpty());
                    }
                    if (!ingredient.isEmpty()) {
                        this.heldItems.get(slot).shrink(ingredient.getCount());
                        this.queued.add(new ItemInPipe(
                                ingredient.copy(),
                                ItemInPipe.DEFAULT_SPEED,
                                ItemInPipe.HALFWAY,
                                Direction.DOWN,
                                this.slotDirections[slot],
                                false,
                                (short) 0
                        ));
                    }
                }
                this.waitingForCraft = this.getResult().getCount();
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

    public NonNullList<ItemStack> getHeldItems() {
        return heldItems;
    }

    public void dropHeldItems(ServerLevel serverLevel, BlockPos pos) {
        for (ItemStack stack : this.heldItems) {
            if (!stack.isEmpty()) {
                ItemEntity droppedItem = new ItemEntity(serverLevel, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, stack);
                droppedItem.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(droppedItem);
            }
        }
        this.heldItems.clear();
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
            if (this.waitingForCraft <= 0) {
                this.waitingForCraft = 0;
                this.crafterTicked = false;
                attemptCraft();
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
        return new RecipePipeMenu(id, playerInventory, this.filter, this.slotDirections, this.getDirectionsForButtons(this.getBlockState()), this.getBlockPos());
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
        this.heldItems.clear();
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
        ValueInput.TypedInputList<ItemStackWithSlot> heldItemList = valueInput.listOrEmpty("held_items", ItemStackWithSlot.CODEC);
        for (ItemStackWithSlot slotStack : heldItemList) {
            if (slotStack.slot() >= 0 && slotStack.slot() < 9) {
                this.heldItems.set(slotStack.slot(), slotStack.stack());
            }
        }
        this.waitingForCraft = valueInput.getIntOr("waiting_for_craft", 0);
        this.crafterTicked = valueInput.getBooleanOr("crafter_ticked", false);
        this.cooldown = valueInput.getByteOr("cooldown", DEFAULT_COOLDOWN);
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
        ValueOutput.TypedOutputList<ItemStackWithSlot> heldItemList = valueOutput.list("held_items", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < this.heldItems.size(); slot++) {
            ItemStack stack = this.heldItems.get(slot);
            if (!stack.isEmpty()) {
                heldItemList.add(new ItemStackWithSlot(slot, stack));
            }
        }
        valueOutput.putInt("waiting_for_craft", this.waitingForCraft);
        valueOutput.putBoolean("crafter_ticked", this.crafterTicked);
        valueOutput.putByte("cooldown", this.cooldown);
    }

    public Filter getFilter() {
        return this.filter;
    }

}
