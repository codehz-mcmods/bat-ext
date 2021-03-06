package moe.hertz.bat_ext.data;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BatTraderData {
    @Getter
    private final @Nullable ParticleEffect particle;
    private final NbtCompound offerListData;

    public TradeOfferList generateOfferList() {
        return new TradeOfferList(this.offerListData);
    }

    public static BatTraderData read(JsonObject json) {
        ParticleEffect particle = readParticleEffect(JsonHelper.getObject(json, "particle"));
        TradeOfferList offerList = readTradeOfferList(JsonHelper.getArray(json, "offers"));
        return new BatTraderData(particle, offerList.toNbt());
    }

    private static ParticleEffect readParticleEffect(JsonObject desc) {
        var name = JsonHelper.getString(desc, "name");
        var parameter = JsonHelper.getString(desc, "parameter", "");
        Identifier identifier;
        try {
            identifier = new Identifier(name);
        } catch (InvalidIdentifierException e) {
            throw new JsonSyntaxException("Cannot parse particle identifier " + name, e);
        }
        var type = Registry.PARTICLE_TYPE
                .getOrEmpty(identifier)
                .orElseThrow(() -> new JsonSyntaxException(String.format("Particle %s not found", identifier)));
        return readParameters(parameter, type, identifier);
    }

    @SuppressWarnings("deprecation")
    private static <T extends ParticleEffect> T readParameters(String parameter, ParticleType<T> type,
            Identifier identifier) {
        try {
            return type.getParametersFactory().read(type, new StringReader(' ' + parameter));
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException(
                    String.format("Cannot parse particle %s parameters: %s", identifier, parameter), e);
        }
    }

    private static TradeOffer getTradeOffer(JsonObject offer) {
        var first = getItemStackFrom(offer, "buy_1");
        var second = getOptionalItemStackFrom(offer, "buy_2");
        var output = getItemStackFrom(offer, "sell");
        var max = JsonHelper.getInt(offer, "count");
        var xp = JsonHelper.getInt(offer, "xp", 0);
        return new TradeOffer(first, second, output, 0, max, xp, 0);
    }

    private static TradeOfferList readTradeOfferList(JsonArray offers) {
        var offerList = new TradeOfferList();
        var idx = 0;
        for (var offerel : offers) {
            var offer = JsonHelper.asObject(offerel, String.format("offers[%d]", idx++));
            offerList.add(getTradeOffer(offer));
        }
        return offerList;
    }

    private static ItemStack getOptionalItemStackFrom(JsonObject offer, String name) {
        return offer.has(name) ? readItemStack(JsonHelper.getObject(offer, name)) : ItemStack.EMPTY;
    }

    private static ItemStack getItemStackFrom(JsonObject offer, String name) {
        return readItemStack(JsonHelper.getObject(offer, name));
    }

    private static ItemStack readItemStack(JsonObject json) {
        var itemStr = JsonHelper.getString(json, "item");
        var itemInput = new StringReader(itemStr);
        try {
            Item item = Registry.ITEM.getOrEmpty(Identifier.fromCommandInput(itemInput))
                    .orElseThrow(() -> new JsonSyntaxException("Unknown item '" + itemStr + "'"));
            if (item == Items.AIR) {
                throw new JsonSyntaxException("Invalid item: " + itemStr);
            }
            var count = JsonHelper.getInt(json, "count");
            var stack = new ItemStack(item, count);
            if (itemInput.canRead() && itemInput.peek() == '{') {
                stack.setNbt(new StringNbtReader(itemInput).parseCompound());
            }
            return stack;
        } catch (CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid item: " + itemStr);
        }
    }
}
