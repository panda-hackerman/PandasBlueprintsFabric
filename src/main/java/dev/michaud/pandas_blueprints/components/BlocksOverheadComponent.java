package dev.michaud.pandas_blueprints.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Makes an item able to block attacks from above, when equipped in the head slot.
 *
 * @param damageReductions How much damage should be blocked from a given attack
 * @param itemDamage       How much to reduce item damage after an attack
 * @param bypassedBy       A damage type that bypasses the blocking
 * @param blockSound       The sound to play when an attack is blocked
 */
public record BlocksOverheadComponent(List<DamageReduction> damageReductions,
                                      ItemDamage itemDamage,
                                      Optional<TagKey<DamageType>> bypassedBy,
                                      Optional<RegistryEntry<SoundEvent>> blockSound) {

  public static final Codec<BlocksOverheadComponent> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
          DamageReduction.CODEC.listOf()
              .optionalFieldOf("damage_reductions", List.of(DamageReduction.DEFAULT))
              .forGetter(BlocksOverheadComponent::damageReductions),
          ItemDamage.CODEC
              .optionalFieldOf("item_damage", ItemDamage.DEFAULT)
              .forGetter(BlocksOverheadComponent::itemDamage),
          TagKey.codec(RegistryKeys.DAMAGE_TYPE).optionalFieldOf("bypassed_by")
              .forGetter(BlocksOverheadComponent::bypassedBy),
          SoundEvent.ENTRY_CODEC.optionalFieldOf("block_sound")
              .forGetter(BlocksOverheadComponent::blockSound)
      ).apply(instance, BlocksOverheadComponent::new)
  );

  public static float getDamageBlockedAmount(LivingEntity entity, ServerWorld world,
      DamageSource source, float amount) {

    if (amount <= 0) {
      return 0; // Nothing to block
    }

    final ItemStack headItem = entity.getEquippedStack(EquipmentSlot.HEAD);
    final BlocksOverheadComponent component = headItem.get(
        ModComponentTypes.BLOCKS_OVERHEAD_ATTACKS);

    if (component == null || component.bypassesBlocking(source)) {
      return 0; // No blocking occurs
    }

    final double angleRad; // Angle in radians
    final Entity sourceEntity = source.getSource();
    final Vec3d entityPos = entity.getEntityPos();
    final Vec3d sourcePos = source.getPosition();

    if (sourceEntity instanceof FallingBlockEntity
        || sourceEntity instanceof ProjectileEntity) {

      final Vec3d velocity = sourceEntity.getVelocity().normalize();
      final double dot = velocity.dotProduct(new Vec3d(0, -1, 0));
      angleRad = Math.acos(MathHelper.clamp(dot, -1, 1));

    } else if (sourcePos != null) {

      final Vec3d delta = sourcePos.subtract(entityPos).normalize();
      final double dot = delta.dotProduct(new Vec3d(0, 1, 0));
      angleRad = Math.acos(MathHelper.clamp(dot, -1, 1));

    } else {
      angleRad = Math.PI;
    }

    final float blockedAmount = component.getReductionAmountFromSource(source, amount, angleRad);
    component.onItemHit(world, headItem, entity, EquipmentSlot.HEAD, blockedAmount);

    if (!source.isIn(DamageTypeTags.IS_PROJECTILE)
        && sourceEntity instanceof LivingEntity attacker) {
      entity.takeShieldHit(world, attacker);
    }

    return blockedAmount;
  }

  /**
   * Called when the helmet is hit.
   */
  public void onItemHit(World world, ItemStack stack, LivingEntity entity,
      EquipmentSlot slot, float shieldDamage) {
    if (entity instanceof PlayerEntity playerEntity) {
      final int damage = itemDamage.calculate(shieldDamage);
      if (damage > 0) {
        stack.damage(damage, entity, slot);
      }
    }
  }

  public void playBlockSound(ServerWorld world, LivingEntity from) {
    blockSound.ifPresent(
        sound -> world.playSound(null, from.getX(), from.getY(), from.getZ(), sound,
            from.getSoundCategory(), 1, 1)
    );
  }

  /**
   * @return True if this damage source bypasses blocking
   */
  public boolean bypassesBlocking(DamageSource source) {

    if (source.getSource() instanceof PersistentProjectileEntity projectile
        && projectile.getPierceLevel() > 0) {
      return true; // Piercing projectiles always bypass blocking
    }

    return bypassedBy.filter(source::isIn).isPresent();
  }

  /**
   * @return how much to reduce damage for the given source and angle.
   */
  public float getReductionAmountFromSource(DamageSource source, float damage, double angle) {
    final float reduction = damageReductions.stream()
        .map(damageReduction -> damageReduction.getReductionAmount(source, damage, angle))
        .reduce(Float::sum)
        .orElse(0f);

    return MathHelper.clamp(reduction, 0, damage);
  }

  /**
   * Controls how much damage should be blocked from a given attack.
   *
   * @param type                  Damage types to block, defaults to all damage types.
   * @param verticalBlockingAngle Maximum vertical angle to block (in degrees), must be positive
   * @param base                  Constant amount of damage to block.
   * @param factor                Additional fraction of the dealt damage to block.
   * @see net.minecraft.component.type.BlocksAttacksComponent.DamageReduction
   */
  public record DamageReduction(Optional<RegistryEntryList<DamageType>> type,
                                float verticalBlockingAngle, float base, float factor) {

    public static final Codec<DamageReduction> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            RegistryCodecs.entryList(RegistryKeys.DAMAGE_TYPE).optionalFieldOf("type")
                .forGetter(DamageReduction::type),
            Codecs.POSITIVE_FLOAT.optionalFieldOf("vertical_blocking_angle", 90f)
                .forGetter(DamageReduction::verticalBlockingAngle),
            Codec.FLOAT.fieldOf("base").forGetter(DamageReduction::base),
            Codec.FLOAT.fieldOf("factor").forGetter(DamageReduction::factor)
        ).apply(instance, DamageReduction::new)
    );

    public static final DamageReduction DEFAULT = new DamageReduction(Optional.empty(), 45, 0, 1);

    public float getReductionAmount(DamageSource source, float damage, double angle) {
      if (angle > Math.toRadians(verticalBlockingAngle)) {
        return 0;
      }

      if (type.isEmpty() || type.get().contains(source.getTypeRegistryEntry())) {
        final float reduction = base + (factor * damage);
        return MathHelper.clamp(reduction, 0, damage);
      }

      return 0;
    }

  }

  /**
   * Controls how much to reduce item durability after an attack.
   *
   * @param threshold The minimum attack damage before the item loses durability.
   * @param base      Constant amount of durability to lose, if the threshold is passed.
   * @param factor    Additional fraction/multiple of damage to reduce durability by, if the
   *                  threshold is passed.
   * @see net.minecraft.component.type.BlocksAttacksComponent.ItemDamage
   */
  public record ItemDamage(float threshold, float base, float factor) {

    public static final Codec<ItemDamage> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codecs.NON_NEGATIVE_FLOAT.fieldOf("threshold").forGetter(ItemDamage::threshold),
            Codec.FLOAT.fieldOf("base").forGetter(ItemDamage::base),
            Codec.FLOAT.fieldOf("factor").forGetter(ItemDamage::factor)
        ).apply(instance, ItemDamage::new)
    );

    public static final ItemDamage DEFAULT = new ItemDamage(1, 0, 1);

    /**
     * @param damage The amount of damage blocked by the shield
     * @return the amount of durability to remove
     */
    int calculate(float damage) {
      if (damage >= threshold) {
        return MathHelper.floor(base + factor * damage);
      }

      return 0;
    }
  }

}