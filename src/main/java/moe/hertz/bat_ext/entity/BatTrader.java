package moe.hertz.bat_ext.entity;

import lombok.Getter;
import lombok.Setter;
import moe.hertz.bat_ext.BatExt;
import moe.hertz.side_effects.IFakeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Npc;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.World;

public class BatTrader extends BatEntity implements IFakeEntity, Merchant, Npc {
  public static final EntityType<BatTrader> TYPE = Registry.register(
      Registry.ENTITY_TYPE,
      new Identifier("batext", "trader"),
      FabricEntityTypeBuilder
          .<BatTrader>create(SpawnGroup.MISC, BatTrader::new)
          .dimensions(EntityDimensions.fixed(0.5f, 0.9f))
          .trackRangeChunks(5)
          .build());

  @Getter
  @Setter
  private PlayerEntity customer;
  private TradeOfferList offers;
  @Getter
  private int experience;
  @Getter
  private boolean leveledMerchant;

  public BatTrader(EntityType<? extends BatEntity> entityType, World world) {
    super(entityType, world);
  }

  @Override
  public EntityType<?> getFakeType() {
    return EntityType.BAT;
  }

  @Override
  public TradeOfferList getOffers() {
    if (offers == null) {
      var traders = BatExt.BAT_TRADERS.getRegistry().values();
      if (!traders.isEmpty()) {
        offers = traders.iterator().next().getOfferList();
      } else {
        offers = new TradeOfferList();
      }
    }
    return offers;
  }

  @Override
  public void setOffersFromServer(TradeOfferList list) {
    offers = list;
  }

  @Override
  public void setExperienceFromServer(int var1) {
  }

  @Override
  public void onSellingItem(ItemStack var1) {
  }

  @Override
  public void trade(TradeOffer var1) {
  }

  @Override
  public Entity moveToWorld(ServerWorld destination) {
    customer = null;
    return super.moveToWorld(destination);
  }

  @Override
  protected ActionResult interactMob(PlayerEntity player, Hand hand) {
    if (hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
      customer = player;
      sendOffers(player, Text.of("BAT TRADER"), 0);
      return ActionResult.SUCCESS;
    }
    return super.interactMob(player, hand);
  }

  @Override
  public SoundEvent getYesSound() {
    return SoundEvents.ENTITY_VILLAGER_YES;
  }

  @Override
  public boolean isClient() {
    return world.isClient;
  }
}
