package jagm.classicpipes.client.screen;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.inventory.menu.FilterMenu;
import jagm.classicpipes.item.TagLabelItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FilterScreen<T extends FilterMenu> extends AbstractContainerScreen<T> {

    private final Registry<Item> itemRegistry;
    private final Map<String, List<Item>> tagCache;
    private short tick;

    public FilterScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            this.itemRegistry = level.registryAccess().lookupOrThrow(Registries.ITEM);
        } else {
            this.itemRegistry = null;
        }
        this.tagCache = new HashMap<>();
        this.tick = 0;
    }

    public int filterSlots() {
        return this.getMenu().getFilter().getContainerSize();
    }

    public int filterScreenLeft() {
        return this.leftPos;
    }

    public int filterScreenTop() {
        return this.topPos;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        String label = stack.get(ClassicPipes.LABEL_COMPONENT);
        if (slot.index < this.filterSlots() && stack.getItem() instanceof TagLabelItem && label != null && this.itemRegistry != null) {
            List<Item> itemList;
            if (this.tagCache.containsKey(label)) {
                itemList = this.tagCache.get(label);
            } else {
                itemList = new ArrayList<>();
                TagKey<Item> tagKey = TagKey.create(this.itemRegistry.key(), Identifier.parse(label));
                this.itemRegistry.getTagOrEmpty(tagKey).forEach(holder -> itemList.add(holder.value()));
                this.tagCache.put(label, itemList);
            }
            int seed = slot.x + slot.y * this.imageWidth;
            ItemStack stackToRender = itemList.isEmpty() ? stack : new ItemStack(itemList.get((this.tick / 20) % itemList.size()));
            graphics.renderItem(stackToRender, slot.x, slot.y, seed);
            graphics.renderItemDecorations(this.font, stack, slot.x, slot.y, null);
            graphics.pose().pushMatrix();
            graphics.drawString(this.font, "#", slot.x, slot.y, -256, true);
            graphics.pose().popMatrix();
        } else {
            super.renderSlot(graphics, slot, mouseX, mouseY);
        }
    }

    @Override
    protected void containerTick() {
        this.tick++;
        super.containerTick();
    }

}
