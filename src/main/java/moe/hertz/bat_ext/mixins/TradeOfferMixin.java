package moe.hertz.bat_ext.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.village.TradeOffer;

@Mixin(TradeOffer.class)
public interface TradeOfferMixin {
  @Accessor
  public void setRewardingPlayerExperience(boolean reward);
}
