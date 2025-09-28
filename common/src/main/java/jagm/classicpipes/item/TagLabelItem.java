package jagm.classicpipes.item;

import jagm.classicpipes.ClassicPipes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class TagLabelItem extends LabelItem {

    public TagLabelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack labelStack = player.getItemInHand(hand);
        ItemStack targetStack = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        List<String> tags = new ArrayList<>();
        for (TagKey<Item> tagKey : targetStack.getTags().toList()) {
            tags.add(tagKey.location().toString());
        }
        if (targetStack.isEmpty()) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".nothing_in_offhand"), false);
            }
        } else if (tags.isEmpty()) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("chat." + ClassicPipes.MOD_ID + ".no_tags_in_hand"), false);
            }
        } else {
            CompoundTag compoundTag = labelStack.getOrCreateTag();
            String currentTag = compoundTag.contains("classic_pipes_label", CompoundTag.TAG_STRING) ? compoundTag.getString("classic_pipes_label") : "";
            if (!currentTag.isEmpty() && !tags.contains(currentTag)) {
                compoundTag.putString("classic_pipes_label", tags.get(0));
                if (level.isClientSide()) {
                    player.displayClientMessage(tagSetMessage(tags.get(0)), false);
                }
            } else {
                for (int i = 0; i < tags.size(); i++) {
                    if (tags.get(i).equals(currentTag)) {
                        String tag = tags.get((i + 1) % tags.size());
                        compoundTag.putString("classic_pipes_label", tag);
                        if (level.isClientSide()) {
                            player.displayClientMessage(tagSetMessage(tag), false);
                        }
                        break;
                    }
                }
            }
            labelStack.setTag(compoundTag);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(labelStack);
    }

    private Component tagSetMessage(String tag) {
        MutableComponent tagTranslation = Component.translatableWithFallback(labelToTranslationKey(tag), "");
        if (tagTranslation.getString().isEmpty()) {
            return Component.translatable("chat." + ClassicPipes.MOD_ID + ".tag_set", Component.literal("#" + tag).withStyle(ChatFormatting.YELLOW));
        } else {
            return Component.translatable("chat." + ClassicPipes.MOD_ID + ".tag_set_translatable", Component.literal("#" + tag).withStyle(ChatFormatting.YELLOW), tagTranslation.withStyle(ChatFormatting.YELLOW));
        }
    }

    private static String labelToTranslationKey(String label) {
        String[] split = label.split(":");
        if (split.length < 2) {
            return "tag.item.minecraft." + label.replace("/", ".");
        } else {
            return "tag.item." + split[0] + "." + split[1].replace("/", ".");
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag compoundTag = stack.getOrCreateTag();
        String tag = compoundTag.contains("classic_pipes_label", CompoundTag.TAG_STRING) ? compoundTag.getString("classic_pipes_label") : "";
        if (!tag.isEmpty()) {
            MutableComponent tagTranslation = Component.translatableWithFallback(labelToTranslationKey(tag), "");
            if (!tagTranslation.getString().isEmpty()) {
                tooltip.add(tagTranslation.withStyle(ChatFormatting.YELLOW));
            }
            tooltip.add(Component.literal("#" + tag).withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item." + ClassicPipes.MOD_ID + ".tag_label.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean itemMatches(ItemStack tagStack, ItemStack compareStack) {
        CompoundTag compoundTag = tagStack.getOrCreateTag();
        String tag = compoundTag.contains("classic_pipes_label", CompoundTag.TAG_STRING) ? compoundTag.getString("classic_pipes_label") : "";
        if (!tag.isEmpty()) {
            for (TagKey<Item> tagKey : compareStack.getTags().toList()) {
                if (tag.equals(tagKey.location().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

}
