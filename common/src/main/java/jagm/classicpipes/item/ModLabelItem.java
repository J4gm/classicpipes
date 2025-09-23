package jagm.classicpipes.item;

import jagm.classicpipes.ClassicPipes;
import jagm.classicpipes.services.Services;
import jagm.classicpipes.util.MiscUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
            String mod = MiscUtil.modFromItem(targetStack);
            labelStack.set(ClassicPipes.LABEL_COMPONENT, mod);
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".mod_set", Component.literal(Services.LOADER_SERVICE.getModName(mod)).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
            }
        }
        return InteractionResultHolder.success(labelStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String mod = stack.get(ClassicPipes.LABEL_COMPONENT);
        if (mod != null) {
            tooltip.add(Component.literal(Services.LOADER_SERVICE.getModName(mod)).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("item." + ClassicPipes.MOD_ID + ".mod_label.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean itemMatches(ItemStack tagStack, ItemStack compareStack) {
        String mod = tagStack.get(ClassicPipes.LABEL_COMPONENT);
        return mod != null && mod.equals(MiscUtil.modFromItem(compareStack));
    }

}
