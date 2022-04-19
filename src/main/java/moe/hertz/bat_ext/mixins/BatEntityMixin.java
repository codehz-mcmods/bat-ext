package moe.hertz.bat_ext.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import moe.hertz.bat_ext.entity.ICustomBatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.world.World;

@Mixin(BatEntity.class)
public abstract class BatEntityMixin extends LivingEntity {
  protected BatEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
    super(entityType, world);
  }

  @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/BatEntity;isRoosting()Z"), method = "tick()V", cancellable = true)
  private void skipTick(CallbackInfo ci) {
    System.out.println("cancel");
    if (((Object) this) instanceof ICustomBatEntity) {
      ci.cancel();
    }
  }
  // @Inject(at = @At("HEAD"), method = "tick()V", cancellable = true)
  // private void skipTick(CallbackInfo ci) {
  // if (((Object) this) instanceof ICustomBatEntity) {
  // System.out.println("HERE <---");
  // super.tick();
  // ci.cancel();
  // }
  // }
}
