package moe.hertz.bat_ext.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import moe.hertz.bat_ext.mixins.TradeOfferMixin;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class BatTraderData {
    private final NbtCompound offerListData;

    private BatTraderData(TradeOfferList offerList) {
        this.offerListData = offerList.toNbt();
    }

    public TradeOfferList generateOfferList() {
        return new TradeOfferList(this.offerListData);
    }

    public static BatTraderData read(JsonObject json) {
        var offers = JsonHelper.getArray(json, "offers");
        TradeOfferList offerList = new TradeOfferList();
        var idx = 0;
        for (var offerel : offers) {
            var offer = JsonHelper.asObject(offerel, String.format("offers[%d]", idx++));
            var first = getItemStack(JsonHelper.getObject(offer, "buy"));
            var second = offer.has("buyB") ? getItemStack(JsonHelper.getObject(offer, "buyB")) : ItemStack.EMPTY;
            var output = getItemStack(JsonHelper.getObject(offer, "sell"));
            var max = JsonHelper.getInt(offer, "maxUses");
            var rewardExp = JsonHelper.getBoolean(offer, "rewardExp", true);
            var xp = JsonHelper.getInt(offer, "xp", 1);
            var priceMultiplier = JsonHelper.getFloat(offer, "priceMultiplier", 0f);
            var specialPrice = JsonHelper.getInt(offer, "specialPrice", 0);
            var demand = JsonHelper.getInt(offer, "demand", 0);
            var ins = new TradeOffer(first, second, output, 0, max, xp, priceMultiplier, demand);
            ins.setSpecialPrice(specialPrice);
            var inmix = (TradeOfferMixin) (Object) ins;
            inmix.setRewardingPlayerExperience(rewardExp);
            offerList.add(ins);
        }
        return new BatTraderData(offerList);
    }

    private static ItemStack getItemStack(JsonObject json) {
        var item = getItem(json);
        var count = JsonHelper.getInt(json, "count");
        var stack = new ItemStack(item, count);
        var tag = JsonHelper.getString(json, "tag", "");
        if (!tag.isEmpty()) {
            try {
                stack.setNbt(StringNbtReader.parse(tag));
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("Failed to parse nbt: " + tag, e);
            }
        }
        return stack;
    }

    private static Item getItem(JsonObject json) {
        String string = JsonHelper.getString(json, "item");
        Item item = Registry.ITEM.getOrEmpty(new Identifier(string))
                .orElseThrow(() -> new JsonSyntaxException("Unknown item '" + string + "'"));
        if (item == Items.AIR) {
            throw new JsonSyntaxException("Invalid item: " + string);
        }
        return item;
    }
}
