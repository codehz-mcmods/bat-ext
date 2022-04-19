package moe.hertz.bat_ext.entity;

import java.util.Comparator;
import java.util.EnumSet;

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
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.World;

public class BatTrader extends BatEntity implements IFakeEntity, Merchant, Npc, ICustomBatEntity {
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
    this.moveControl = new FlightMoveControl(this, 20, true);
    this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0f);
    this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0f);
  }

  public static DefaultAttributeContainer.Builder createBatTraderAttributes() {
    return createBatAttributes().add(EntityAttributes.GENERIC_FLYING_SPEED, 1.0f);
  }

  @Override
  protected EntityNavigation createNavigation(World world) {
    var birdNavigation = new BirdNavigation(this, world) {
      @Override
      public boolean isValidPosition(BlockPos pos) {
        return true;
      }
      @Override
      public void tick() {
        if (isRoosting())
          return;
        super.tick();
      }
    };
    birdNavigation.setCanPathThroughDoors(false);
    birdNavigation.setCanSwim(false);
    birdNavigation.setCanEnterOpenDoors(true);
    return birdNavigation;
  }

  @Override
  protected void initGoals() {
    this.goalSelector.add(1, new FlyToTargetGoal());
    this.goalSelector.add(2, new FlyAroundGoal());
    this.goalSelector.add(3, new TakeOffGoal());
    this.targetSelector.add(1, new FindTargetGoal());
  }

  private class FindTargetGoal extends Goal {
    private final TargetPredicate PLAYERS_IN_RANGE_PREDICATE = TargetPredicate.createNonAttackable()
        .setBaseMaxDistance(32.0);
    private int delay = FindTargetGoal.toGoalTicks(20);

    @Override
    public boolean canStart() {
      if (customer != null)
        return false;
      if (this.delay > 0) {
        --this.delay;
        return false;
      }
      this.delay = FindTargetGoal.toGoalTicks(60);
      var selected = world.getClosestPlayer(PLAYERS_IN_RANGE_PREDICATE, BatTrader.this);
      if (selected != null && selected instanceof ServerPlayerEntity player) {
        setTarget(selected);
        player.sendMessage(Text.of("capture"), false);
        return true;
      }
      return false;
    }

    @Override
    public boolean shouldContinue() {
      var target = getTarget();
      if (target != null) {
        if (target.isAlive() && !target.isSpectator() && target.squaredDistanceTo(BatTrader.this) < 1024.0) {
          return true;
        }
        setTarget(null);
        if (target instanceof ServerPlayerEntity player) {
          player.sendMessage(Text.of(String.format("lost: (%b %b %f)", target.isAlive(), target.isSpectator(),
              target.squaredDistanceTo(BatTrader.this))), false);
        }
      }
      return false;
    }
  }

  class FlyToTargetGoal extends MovementGoal {
    private int delay = Goal.toGoalTicks(20);

    @Override
    public boolean canStart() {
      if (getTarget() == null || isRoosting())
        return false;
      if (this.delay > 0) {
        --this.delay;
        return false;
      }
      this.delay = Goal.toGoalTicks(50);
      return !isNearTarget(32.0);
    }

    @Override
    public void start() {
      setGlowing(true);
      if (getTarget() instanceof ServerPlayerEntity player) {
        player.sendMessage(Text.of("start"), false);
      }
      navigation.startMovingTo(getTarget(), 1.0);
    }

    @Override
    public void stop() {
      setGlowing(false);
      navigation.stop();
      if (getTarget() instanceof ServerPlayerEntity player) {
        player.sendMessage(Text.of("stop"), false);
      }
    }

    public boolean shouldContinue() {
      return !isRoosting() && !navigation.isIdle() && !isNearTarget(9.0);
    }

    private boolean isNearTarget(double sqdis) {
      var target = getTarget();
      if (target != null) {
        return target.squaredDistanceTo(BatTrader.this) < sqdis;
      }
      return false;
    }
  }

  class FlyAroundGoal extends MovementGoal {
    private BlockPos hangingPosition;

    @Override
    public boolean canStart() {
      return navigation.isIdle() && !isRoosting();
    }

    @Override
    public boolean shouldRunEveryTick() {
      return true;
    }

    @Override
    public void stop() {
      hangingPosition = null;
    }

    @Override
    public void tick() {
      BlockPos blockPos = getBlockPos().up();
      if (!(hangingPosition == null || world.isAir(hangingPosition) && hangingPosition.getY() > world.getBottomY())) {
        hangingPosition = null;
      }
      if (hangingPosition == null || random.nextInt(30) == 0
          || hangingPosition.isWithinDistance(getPos(), 2.0)) {
        hangingPosition = new BlockPos(
            getX() + (double) random.nextInt(7) - (double) random.nextInt(7),
            getY() + (double) random.nextInt(6) - 2.0,
            getZ() + (double) random.nextInt(7) - (double) random.nextInt(7));
      }
      double d = (double) hangingPosition.getX() + 0.5 - getX();
      double e = (double) hangingPosition.getY() + 0.1 - getY();
      double f = (double) hangingPosition.getZ() + 0.5 - getZ();
      Vec3d vec3d = getVelocity();
      Vec3d vec3d2 = vec3d.add((Math.signum(d) * 0.5 - vec3d.x) * (double) 0.1f,
          (Math.signum(e) * (double) 0.7f - vec3d.y) * (double) 0.1f, (Math.signum(f) * 0.5 - vec3d.z) * (double) 0.1f);
      setVelocity(vec3d2);
      float g = (float) (MathHelper.atan2(vec3d2.z, vec3d2.x) * 57.2957763671875) - 90.0f;
      float h = MathHelper.wrapDegrees(g - getYaw());
      forwardSpeed = 0.5f;
      setYaw(getYaw() + h);
      if (random.nextInt(100) == 0 && world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
        setRoosting(true);
      }
    }
  }

  class TakeOffGoal extends MovementGoal {
    private int delay = Goal.toGoalTicks(100);

    @Override
    public boolean canStart() {
      if (!isRoosting())
        return false;
      if (this.delay > 0) {
        --this.delay;
        return false;
      }
      this.delay = Goal.toGoalTicks(200);
      return getTarget() != null;
    }

    @Override
    public void start() {
      setRoosting(false);
    }

    @Override
    public boolean shouldContinue() {
      return false;
    }
  }

  abstract class MovementGoal
      extends Goal {
    public MovementGoal() {
      this.setControls(EnumSet.of(Goal.Control.MOVE));
    }
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
        offers = traders.iterator().next().generateOfferList();
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
  public void trade(TradeOffer offer) {
    offer.use();
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

  @Override
  protected void mobTick() {
  }
}
