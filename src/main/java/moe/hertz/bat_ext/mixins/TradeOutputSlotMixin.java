package moe.hertz.bat_ext.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import moe.hertz.bat_ext.entity.BatTrader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.TradeOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.village.Merchant;

@Mixin(TradeOutputSlot.class)
public abstract class TradeOutputSlotMixin {
  @Shadow
  Merchant merchant;

  @Inject(at = @At("HEAD"), method = "onTakeItem")
  void fixTakeItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
    if (merchant instanceof BatTrader bat) {
      var sp = (ServerPlayerEntity) player;
      sp.sendMessage(Text.of("trade"), false);
    }
  }
}
