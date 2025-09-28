package jagm.classicpipes.item;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ModLabelItem extends LabelItem {

    public ModLabelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack labelStack = player.getItemInHand(hand);
        ItemStack targetStack = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (targetStack.isEmpty()) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".nothing_in_offhand"), false);
            }
        } else {
            CompoundTag compoundTag = labelStack.getOrCreateTag();
            String mod = MiscUtil.modFromItem(targetStack);
            compoundTag.putString("classic_pipes_label", mod);
            labelStack.setTag(compoundTag);
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".mod_set", Component.literal(Services.LOADER_SERVICE.getModName(mod)).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(labelStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag compoundTag = stack.getOrCreateTag();
        String mod = compoundTag.contains("classic_pipes_label", CompoundTag.TAG_STRING) ? compoundTag.getString("classic_pipes_label") : "";
        if (!mod.isEmpty()) {
            tooltip.add(Component.literal(Services.LOADER_SERVICE.getModName(mod)).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("item." + ClassicPipes.MOD_ID + ".mod_label.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean itemMatches(ItemStack tagStack, ItemStack compareStack) {
        CompoundTag compoundTag = tagStack.getOrCreateTag();
        String mod = compoundTag.contains("classic_pipes_label", CompoundTag.TAG_STRING) ? compoundTag.getString("classic_pipes_label") : "";
        return !mod.isEmpty() && mod.equals(MiscUtil.modFromItem(compareStack));
    }

}
