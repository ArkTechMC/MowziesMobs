package com.bobmowzie.mowziesmobs.server.entity.barakoa;

import com.bobmowzie.mowziesmobs.MowziesMobs;
import com.bobmowzie.mowziesmobs.client.model.tools.ControlledAnimation;
import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.client.particle.ParticleHandler;
import com.bobmowzie.mowziesmobs.client.particle.ParticleOrb;
import com.bobmowzie.mowziesmobs.client.particle.util.AdvancedParticleBase;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleComponent;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleComponent.PropertyControl.EnumParticleProperty;
import com.bobmowzie.mowziesmobs.server.advancement.AdvancementHandler;
import com.bobmowzie.mowziesmobs.server.ai.BarakoaHurtByTargetAI;
import com.bobmowzie.mowziesmobs.server.ai.animation.*;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.entity.LeaderSunstrikeImmune;
import com.bobmowzie.mowziesmobs.server.entity.MowzieEntity;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntitySuperNova;
import com.bobmowzie.mowziesmobs.server.inventory.ContainerBarakoTrade;
import com.bobmowzie.mowziesmobs.server.item.BarakoaMask;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import com.bobmowzie.mowziesmobs.server.loot.LootTableHandler;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import com.ilexiconn.llibrary.server.animation.Animation;
import com.ilexiconn.llibrary.server.animation.AnimationHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntityBarako extends MowzieEntity implements LeaderSunstrikeImmune, IMob {
    public static final Animation DIE_ANIMATION = Animation.create(130);
    public static final Animation HURT_ANIMATION = Animation.create(13);
    public static final Animation BELLY_ANIMATION = Animation.create(40);
    public static final Animation TALK_ANIMATION = Animation.create(80);
    public static final Animation SUNSTRIKE_ANIMATION = Animation.create(15);
    public static final Animation ATTACK_ANIMATION = Animation.create(30);
    public static final Animation SPAWN_ANIMATION = Animation.create(20);
    public static final Animation SOLAR_BEAM_ANIMATION = Animation.create(100);
    public static final Animation BLESS_ANIMATION = Animation.create(60);
    public static final Animation SUPERNOVA_ANIMATION = Animation.create(100);
    private static final int MAX_HEALTH = 140;
    private static final int SUNSTRIKE_PAUSE_MAX = 40;
    private static final int SUNSTRIKE_PAUSE_MIN = 20;
    private static final int LASER_PAUSE = 230;
    private static final int SUPERNOVA_PAUSE = 230;
    private static final int BARAKOA_PAUSE = 140;
    private static final int HEAL_PAUSE = 75;
    private static final DataParameter<Integer> DIRECTION = EntityDataManager.createKey(EntityBarako.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DIALOGUE = EntityDataManager.createKey(EntityBarako.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> ANGRY = EntityDataManager.createKey(EntityBarako.class, DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> DESIRES = EntityDataManager.createKey(EntityBarako.class, DataSerializers.ITEMSTACK);
    private static final DataParameter<CompoundNBT> TRADED_PLAYERS = EntityDataManager.createKey(EntityBarako.class, DataSerializers.COMPOUND_NBT);
    public ControlledAnimation legsUp = new ControlledAnimation(15);
    public ControlledAnimation angryEyebrow = new ControlledAnimation(5);
    private PlayerEntity customer;
    // TODO: Enum!
    public int barakoaSpawnCount = 0;
    // TODO: use Direction!
    private int direction = 0;
    private boolean blocksByFeet = true;
    private int timeUntilSunstrike = 0;
    private int timeUntilLaser = 0;
    private int timeUntilBarakoa = 0;
    private int timeUntilSupernova = 0;
    private int timeUntilHeal = 0;
    public PlayerEntity blessingPlayer;
    private BarakoaHurtByTargetAI hurtByTargetAI;

    @OnlyIn(Dist.CLIENT)
    public Vec3d[] betweenHandPos;
    @OnlyIn(Dist.CLIENT)
    public Vec3d[] blessingPlayerPos;

    private static EntityPredicate GIVE_ACHIEVEMENT_PRED = new EntityPredicate().setLineOfSiteRequired().setUseInvisibilityCheck();

    private static ParticleComponent.KeyTrack superNovaKeyTrack1 = new ParticleComponent.KeyTrack(
            new float[]{0, 20f, 20f, 0},
            new float[]{0, 0.5f, 0.9f, 1}
    );
    private static ParticleComponent.KeyTrack superNovaKeyTrack2 = ParticleComponent.KeyTrack.oscillate(0, 1, 30);

    public EntityBarako(EntityType<? extends EntityBarako> type, World world) {
        super(type, world);
        if (getDirection() == 0) {
            this.setDirection(rand.nextInt(4) + 1);
        }
        experienceValue = 45;

        if (world.isRemote) {
            betweenHandPos = new Vec3d[]{new Vec3d(0, 0, 0)};
            blessingPlayerPos = new Vec3d[]{new Vec3d(0, 0, 0)};
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        hurtByTargetAI = new BarakoaHurtByTargetAI(this, true);
        this.targetSelector.addGoal(3, hurtByTargetAI);
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 0, false, true, target -> {
                if (target instanceof PlayerEntity) {
                    if (this.world.getDifficulty() == Difficulty.PEACEFUL) return false;
                    ItemStack headArmorStack = ((PlayerEntity) target).inventory.armorInventory.get(3);
                    if (headArmorStack.getItem() instanceof BarakoaMask) {
                        return false;
                    }
                }
                return true;
            }));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, ZombieEntity.class, 0, true, false, null));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, SkeletonEntity.class, 0, true, false, null));
        this.goalSelector.addGoal(2, new SimpleAnimationAI<>(this, BELLY_ANIMATION, false));
        this.goalSelector.addGoal(2, new SimpleAnimationAI<EntityBarako>(this, TALK_ANIMATION, false) {
            @Override
            public void startExecuting() {
                super.startExecuting();
//                whichDialogue = getWhichDialogue();
            }
        });
        this.goalSelector.addGoal(2, new SimpleAnimationAI<EntityBarako>(this, BLESS_ANIMATION, false) {
            @Override
            public void startExecuting() {
                super.startExecuting();
                blessingPlayer = getCustomer();
            }
        });
        this.goalSelector.addGoal(2, new SimpleAnimationAI<EntityBarako>(this, SUPERNOVA_ANIMATION, false) {
            @Override
            public void startExecuting() {
                super.startExecuting();
                playSound(MMSounds.ENTITY_SUPERNOVA_START.get(), 3f, 1f);
            }

            @Override
            public void tick() {
                super.tick();
                if (entity.getAnimationTick() == 30) {
                    playSound(MMSounds.ENTITY_SUPERNOVA_BLACKHOLE.get(), 2f, 1.2f);
                }
                if (entity.getAnimationTick() == 40) {
                    playSound(MMSounds.ENTITY_BARAKO_SCREAM.get(), 1.5f, 1f);
                }

                if (!entity.world.isRemote) {
                    if (entity.getAnimationTick() == 44) {
                        Vec3d offset = new Vec3d(1.1f, 0, 0);
                        offset = offset.rotateYaw((float) Math.toRadians(-entity.rotationYaw - 90));
                        EntitySuperNova superNova = new EntitySuperNova(EntityHandler.SUPER_NOVA, entity.world, entity, entity.getPosX() + offset.x, entity.getPosY() + 0.05, entity.getPosZ() + offset.z);
                        world.addEntity(superNova);
                    }
                }
            }
        });
        this.goalSelector.addGoal(2, new AnimationSunStrike<EntityBarako>(this, SUNSTRIKE_ANIMATION) {
            @Override
            public void startExecuting() {
                super.startExecuting();
                if (entityTarget != null) {
                    prevX = entityTarget.getPosX();
                    prevZ = entityTarget.getPosZ();
                }
            }
        });
        this.goalSelector.addGoal(2, new AnimationRadiusAttack<EntityBarako>(this, ATTACK_ANIMATION, 4f, ConfigHandler.MOBS.BARAKO.combatConfig.attackMultiplier.get(), 3f, 12, true){
            @Override
            public void startExecuting() {
                super.startExecuting();
                playSound(MMSounds.ENTITY_BARAKO_BURST.get(), 1.7f, 1.5f);
            }
        });
        this.goalSelector.addGoal(2, new AnimationSpawnBarakoa(this, SPAWN_ANIMATION));
        this.goalSelector.addGoal(2, new AnimationSolarBeam<>(this, SOLAR_BEAM_ANIMATION));
        this.goalSelector.addGoal(3, new AnimationTakeDamage<>(this));
        this.goalSelector.addGoal(1, new AnimationDieAI<>(this));
//        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
//        this.goalSelector.addGoal(7, new LookAtGoal(this, EntityBarakoa.class, 8.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    //    public EntityBarako(World world, int direction) {
//        this(world);
//        this.setDirection(direction);
//    }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return 1.4f;
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MAX_HEALTH * ConfigHandler.MOBS.BARAKO.combatConfig.healthMultiplier.get());
        this.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40);
        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5);
    }

    @Override
    public boolean preventDespawn() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (getAnimation() == NO_ANIMATION) {
            if (getAttackTarget() == null && !isAIDisabled()) {
                int soundType = MathHelper.nextInt(rand, 0, 9);
                if (soundType < MMSounds.ENTITY_BARAKO_TALK.size()) {
                    this.playSound(MMSounds.ENTITY_BARAKO_TALK.get(soundType).get(), 2F, 1.0F);
                    this.setWhichDialogue(soundType + 1);
                    AnimationHandler.INSTANCE.sendAnimationMessage(this, TALK_ANIMATION);
                }
            } else {
                int soundType = MathHelper.nextInt(rand, 1, 10);
                if (soundType < 7) {
                    this.playSound(MMSounds.ENTITY_BARAKO_ANGRY.get(soundType - 1).get(), 2F, 1.0F);
//                    setWhichDialogue(soundType);
//                    AnimationHandler.INSTANCE.sendAnimationMessage(this, 3);
                }
            }
        }
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MMSounds.ENTITY_BARAKO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        this.playSound(MMSounds.ENTITY_BARAKO_DIE.get(), 2f, 1);
        return null;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksExisted == 1) {
            direction = getDirection();
        }
        if (!(getAnimation() == ATTACK_ANIMATION && getAnimationTick() >= 12 && getAnimationTick() <= 14)) this.repelEntities(1.8f, 1.8f, 1.8f, 1.8f);
        this.rotationYaw = (direction - 1) * 90;
        this.renderYawOffset = rotationYaw;
//        this.posX = prevPosX;
//        this.posZ = prevPosZ;
        setMotion(0, getMotion().y, 0);

        if (getAttackTarget() != null) {
            LivingEntity target = getAttackTarget();
            this.setAngry(true);
            float entityHitAngle = (float) ((Math.atan2(target.getPosZ() - getPosZ(), target.getPosX() - getPosX()) * (180 / Math.PI) - 90) % 360);
            float entityAttackingAngle = rotationYaw % 360;
            if (entityHitAngle < 0) {
                entityHitAngle += 360;
            }
            if (entityAttackingAngle < 0) {
                entityAttackingAngle += 360;
            }
            float entityRelativeAngle = Math.abs(entityHitAngle - entityAttackingAngle);
            Vec3d betweenEntitiesVec = getPositionVector().subtract(target.getPositionVector());
            boolean targetComingCloser = target.getMotion().dotProduct(betweenEntitiesVec) > 0;
            if (getAnimation() == NO_ANIMATION && !isAIDisabled() && rand.nextInt(80) == 0 && getEntitiesNearby(EntityBarakoa.class, 25).size() < 5 && targetDistance > 4.5 && timeUntilBarakoa <= 0) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, SPAWN_ANIMATION);
                timeUntilBarakoa = BARAKOA_PAUSE;
            } else if (getAnimation() == NO_ANIMATION && !isAIDisabled() && getHealthRatio() <= 0.6 && timeUntilLaser <= 0 && (entityRelativeAngle < 60 || entityRelativeAngle > 300) && getEntitySenses().canSee(target)) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, SOLAR_BEAM_ANIMATION);
                timeUntilLaser = LASER_PAUSE;
            } else if (getAnimation() == NO_ANIMATION && !isAIDisabled() && getHealthRatio() <= 0.6 && timeUntilSupernova <= 0 && targetDistance <= 10.5) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, SUPERNOVA_ANIMATION);
                timeUntilSupernova = SUPERNOVA_PAUSE;
            } else if (getAnimation() == NO_ANIMATION && !isAIDisabled() && (targetDistance <= 6f && targetComingCloser || targetDistance < 4.f)) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, ATTACK_ANIMATION);
            } else if (getAnimation() == NO_ANIMATION && !isAIDisabled() && timeUntilSunstrike <= 0) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, SUNSTRIKE_ANIMATION);
                timeUntilSunstrike = getTimeUntilSunstrike();
            }
            if (!hurtByTargetAI.shouldContinueExecuting()) {
                hurtByTargetAI.resetTask();
            }
        } else {
            if (!world.isRemote) {
                this.setAngry(false);
            }
        }

        if (ticksExisted % 20 == 0) {
            blocksByFeet = checkBlocksByFeet();
        }

        if (blocksByFeet) {
            legsUp.increaseTimer();
        } else {
            legsUp.decreaseTimer();
        }

        if (getAngry()) {
            angryEyebrow.increaseTimer();
        } else {
            angryEyebrow.decreaseTimer();
        }

        if (getAnimation() == NO_ANIMATION && !isAIDisabled() && getAttackTarget() == null && rand.nextInt(200) == 0) {
            AnimationHandler.INSTANCE.sendAnimationMessage(this, BELLY_ANIMATION);
        }

        if (getAnimation() == BELLY_ANIMATION && (getAnimationTick() == 9 || getAnimationTick() == 29)) {
            this.playSound(MMSounds.ENTITY_BARAKO_BELLY.get(), 3f, 1f);
        }

//        if (getAnimation() == TALK_ANIMATION && getAnimationTick() == 1) {
//            whichDialogue = getWhichDialogue();
//        }

        if (getAnimation() == ATTACK_ANIMATION) {
            rotationYawHead = rotationYaw;
//            if (getAnimationTick() == 1) {
//                this.playSound(MMSounds.ENTITY_BARAKO_BURST, 1.7f, 1.5f);
//            }
            if (getAnimationTick() == 10) {
                if (world.isRemote) {
                    spawnExplosionParticles(30);
                }
                this.playSound(MMSounds.ENTITY_BARAKO_ATTACK.get(), 1.7f, 0.9f);
            }
            if (getAnimationTick() <= 6 && world.isRemote) {
                int particleCount = 8;
                while (--particleCount != 0) {
                    double radius = 2f;
                    double yaw = rand.nextFloat() * 2 * Math.PI;
                    double pitch = rand.nextFloat() * 2 * Math.PI;
                    double ox = radius * Math.sin(yaw) * Math.sin(pitch);
                    double oy = radius * Math.cos(pitch);
                    double oz = radius * Math.cos(yaw) * Math.sin(pitch);
                    float offsetX = (float) (-0.3 * Math.sin(rotationYaw * Math.PI / 180));
                    float offsetZ = (float) (-0.3 * Math.cos(rotationYaw * Math.PI / 180));
                    float offsetY = 1;
                    world.addParticle(new ParticleOrb.OrbData((float) getPosX() + offsetX, (float) getPosY() + offsetY, (float) getPosZ() + offsetZ, 6), getPosX() + ox + offsetX, getPosY() + offsetY + oy, getPosZ() + oz + offsetZ, 0, 0, 0);
                }
            }
        }

        if (getAnimation() == BLESS_ANIMATION) {
            rotationYawHead = rotationYaw;

            if (getAnimationTick() == 1) {
                blessingPlayer = getCustomer();
            }
            if (world.isRemote && blessingPlayer != null) {
                blessingPlayerPos[0] = blessingPlayer.getPositionVector().add(new Vec3d(0, blessingPlayer.getHeight() / 2f, 0));
                if (getAnimationTick() > 5 && getAnimationTick() < 40) {
                    int particleCount = 2;
                    while (--particleCount != 0) {
                        double radius = 0.7f;
                        double yaw = rand.nextFloat() * 2 * Math.PI;
                        double pitch = rand.nextFloat() * 2 * Math.PI;
                        double ox = radius * Math.sin(yaw) * Math.sin(pitch);
                        double oy = radius * Math.cos(pitch);
                        double oz = radius * Math.cos(yaw) * Math.sin(pitch);
                        AdvancedParticleBase.spawnParticle(world, ParticleHandler.ORB2.get(), getPosX() + ox, getPosY() + 0.8f + oy, getPosZ() + oz, 0, 0, 0, true, 0, 0, 0, 0, 5F, 1, 1, 1, 1, 1, 20, true, new ParticleComponent[]{
                                new ParticleComponent.Attractor(blessingPlayerPos, 0.5f, 0.2f, ParticleComponent.Attractor.EnumAttractorBehavior.LINEAR),
                                new ParticleComponent.PropertyControl(EnumParticleProperty.POS_X, new ParticleComponent.Oscillator(0, (float) ox, 6f, 2.5f), true),
                                new ParticleComponent.PropertyControl(EnumParticleProperty.POS_Y, new ParticleComponent.Oscillator(0, (float) oy, 6f, 2.5f), true),
                                new ParticleComponent.PropertyControl(EnumParticleProperty.POS_Z, new ParticleComponent.Oscillator(0, (float) oz, 6f, 2.5f), true),
                                new ParticleComponent.PropertyControl(EnumParticleProperty.ALPHA, new ParticleComponent.KeyTrack(
                                        new float[]{0f, 1f},
                                        new float[]{0, 0.8f}
                                ), false)
                        });
                    }
                }
                if (getAnimationTick() % 15 == 0) {
                    AdvancedParticleBase.spawnParticle(world, ParticleHandler.RING2.get(), getPosX(), getPosY() + 0.8f, getPosZ(), 0, 0, 0, true, 0, 0, 0, 0, 3.5F, 1, 223/255f, 66/255f, 1, 1, 15, true, new ParticleComponent[]{
                            new ParticleComponent.PropertyControl(EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(1f, 0f), false),
                            new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(5f, 35f), false)
                    });
                }
            }
        }

        if (getAnimation() == SUPERNOVA_ANIMATION) {
            if (world.isRemote && betweenHandPos.length > 0) {
                superNovaEffects();
            }
            if (getAnimationTick() < 30) {
                List<LivingEntity> entities = getEntityLivingBaseNearby(16, 16, 16, 16);
                for (LivingEntity inRange : entities) {
                    if (inRange instanceof LeaderSunstrikeImmune) continue;
                    if (inRange instanceof PlayerEntity && inRange.isInvulnerable()) continue;
                    Vec3d diff = inRange.getPositionVector().subtract(getPositionVector().add(0, 3, 0));
                    diff = diff.normalize().scale(0.03);
                    inRange.setMotion(getMotion().subtract(diff));

                    if (inRange.getPosY() < getPosY() + 3) inRange.setMotion(inRange.getMotion().add(0, 0.075, 0));
                }
            }
        }

        if (ticksExisted % 40 == 0) {
            for (PlayerEntity player : getPlayersNearby(15, 15, 15, 15)) {
                ItemStack headArmorStack = player.inventory.armorInventory.get(3);
                if (getAttackTarget() != player && canAttack(player, GIVE_ACHIEVEMENT_PRED) && headArmorStack.getItem() instanceof BarakoaMask) {
                    if (player instanceof ServerPlayerEntity) AdvancementHandler.SNEAK_VILLAGE_TRIGGER.trigger((ServerPlayerEntity) player);
                }
            }
        }

        if (!world.isRemote && getAttackTarget() == null && getAnimation() != SOLAR_BEAM_ANIMATION && getAnimation() != SUPERNOVA_ANIMATION) {
            timeUntilHeal--;
            if (ConfigHandler.MOBS.BARAKO.healsOutOfBattle.get() && timeUntilHeal <= 0) heal(0.3f);
        }
        else {
            timeUntilHeal = HEAL_PAUSE;
        }

        if (timeUntilSunstrike > 0) {
            timeUntilSunstrike--;
        }
        if (timeUntilLaser > 0 && getAnimation() != SUPERNOVA_ANIMATION) {
            timeUntilLaser--;
        }
        if (timeUntilBarakoa > 0) {
            timeUntilBarakoa--;
        }
        if (timeUntilSupernova > 0 && getAnimation() != SOLAR_BEAM_ANIMATION) {
            timeUntilSupernova--;
        }

//        if (getAnimation() == NO_ANIMATION) {
//            AnimationHandler.INSTANCE.sendAnimationMessage(this, SUPERNOVA_ANIMATION);
//        }
    }

    private void superNovaEffects() {
        if (getAnimationTick() == 1) {
            superNovaKeyTrack1 = new ParticleComponent.KeyTrack(
                    new float[]{0, 25f, 32f, 0},
                    new float[]{0, 0.6f, 0.85f, 1}
            );
            superNovaKeyTrack2 = ParticleComponent.KeyTrack.oscillate(0, 7, 24);
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.SUN.get(), getPosX(), getPosY(), getPosZ(), 0, 0, 0, true, 0, 0, 0, 0, 0F, 1, 1, 1, 1, 1, 33, true, new ParticleComponent[]{
                    new ParticleComponent.PinLocation(betweenHandPos),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, superNovaKeyTrack1, false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, superNovaKeyTrack2, true)
            });
        }
        if (getAnimationTick() == 33) {
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.SUN_NOVA.get(), getPosX(), getPosY(), getPosZ(), 0, 0, 0, true, 0, 0, 0, 0, 20F, 1, 1, 1, 0, 1, 13, true, new ParticleComponent[]{
                    new ParticleComponent.PinLocation(betweenHandPos),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                            new float[]{11f, 7f, 5.5f, 1f, 30},
                            new float[]{0, 0.15f, 0.8f, 0.89f, 1}
                    ), false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.ALPHA, new ParticleComponent.KeyTrack(
                            new float[]{0f, 1f, 1f, 0f},
                            new float[]{0, 0.15f, 0.89f, 1}
                    ), false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.PARTICLE_ANGLE, ParticleComponent.KeyTrack.startAndEnd(0f, -6f), false)
            });
        }
        if (getAnimationTick() == 32) {
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.FLARE.get(), getPosX(), getPosY(), getPosZ(), 0, 0, 0, true, 0, 0, 0, 0, 5F, 1,1,1, 0.7, 1, 3, true, new ParticleComponent[]{
                    new ParticleComponent.PinLocation(betweenHandPos),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.POS_Y, ParticleComponent.constant(-0.15f), true),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                            new float[]{0f, 22f, 0f},
                            new float[]{0, 0.2f, 1}
                    ), false)
            });
        }
        if (getAnimationTick() > 30 && getAnimationTick() < 41) {
            for (int i = 0; i < 6; i++) {
                float phaseOffset = rand.nextFloat();
                double value = rand.nextDouble() * 0.3 + 0.05;
                AdvancedParticleBase.spawnParticle(world, ParticleHandler.PIXEL.get(), betweenHandPos[0].x, betweenHandPos[0].y, betweenHandPos[0].z, 0, 0, 0, true, 0, 0, 0, 0, 5F, value, value, value, 1, 1, 6, false, new ParticleComponent[]{
                        new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                                new float[]{0f, 3f},
                                new float[]{0, 0.2f}
                        ), false),
                        new ParticleComponent.Orbit(betweenHandPos, ParticleComponent.KeyTrack.startAndEnd(0 + phaseOffset, -0.4f + phaseOffset), ParticleComponent.KeyTrack.startAndEnd(0.5f + rand.nextFloat(), 0), ParticleComponent.constant(0), ParticleComponent.constant(0), ParticleComponent.constant(0), true),
                });
            }
        }
        if (getAnimationTick() > 1 && getAnimationTick() < 27) {
            for (int i = 0; i < 6; i++) {
                Vec3d particlePos = new Vec3d(rand.nextFloat() * 5, 0, 0);
                particlePos = particlePos.rotateYaw((float) (rand.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.rotatePitch((float) (rand.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.add(betweenHandPos[0]);
                double value = rand.nextDouble() * 0.5 + 0.1;
                AdvancedParticleBase.spawnParticle(world, ParticleHandler.PIXEL.get(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0, true, 0, 0, 0, 0, 5F, value, value, value, 1, 1, 7, false, new ParticleComponent[]{
                        new ParticleComponent.Attractor(betweenHandPos, 1.1f, 1f, ParticleComponent.Attractor.EnumAttractorBehavior.EXPONENTIAL),
                        new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                                new float[]{0f, 3.5f},
                                new float[]{0, 0.2f}
                        ), false)
                });
            }
        }
        float timeFrac = Math.min((float)getAnimationTick() / 20f, 1f);
        if (getAnimationTick() > 1 && getAnimationTick() < 25 && getAnimationTick() % (int)(4 * (1 - timeFrac) + 1) == 0) {
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.RING_SPARKS.get(),  getPosX(), getPosY(), getPosZ(), 0, 0, 0, true, 0, 0, 0, rand.nextFloat() * (float)Math.PI * 2, 5F, 1, 1, 1, 1, 1, 6 + rand.nextFloat() * 3, true, new ParticleComponent[]{
                    new ParticleComponent.PinLocation(betweenHandPos),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(10f + 20f * timeFrac * timeFrac + 10f * rand.nextFloat() * timeFrac, 0f), false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(0f, 0.7f), false)
            });
        }
        if (getAnimationTick() == 14) {
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.FLARE.get(),  getPosX(), getPosY(), getPosZ(), 0, 0, 0, true, 0, 0, 0, 0, 5F, 1, 1, 1, 1, 1, 18, true, new ParticleComponent[]{
                    new ParticleComponent.PinLocation(betweenHandPos),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.POS_Y, ParticleComponent.constant(-0.1f), true),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                            new float[]{0f, 35f, 0f},
                            new float[]{0, 0.8f, 1}
                    ), false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.Oscillator(-5, 5, 42, 0), true)
            });
        }

        if (getAnimationTick() == 32) {
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.BURST_IN.get(),  getPosX(), getPosY(), getPosZ(), 0, 0, 0, false, 0, Math.PI/2f, 0, 0, 5F, 0, 0, 0, 1, 1, 10, true, new ParticleComponent[]{
                    new ParticleComponent.PinLocation(betweenHandPos),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(25f, 0f), false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(0f, 1f), false),
                    new ParticleComponent.PropertyControl(EnumParticleProperty.SCALE, new ParticleComponent.Oscillator(-2, 2, 42, 0), true),
            });
        }

        if (getAnimationTick() == 44) {
            float scale = 85f;
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.RING_BIG.get(), betweenHandPos[0].x, betweenHandPos[0].y, betweenHandPos[0].z, 0, 0, 0, false, 0, Math.PI/2f, 0, 0, 5F, 1,1,1, 1, 1, 40, true, new ParticleComponent[]{
                    new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                            new float[]{0.0f * scale, 0.59f * scale, 0.87f * scale, 0.974f * scale, 0.998f * scale, 1f * scale},
                            new float[]{0, 0.2f, 0.4f, 0.6f, 0.8f, 1f}
                    ), false),
                    new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(1f, 0f), false),
            });
            scale = 120f;
            AdvancedParticleBase.spawnParticle(world, ParticleHandler.GLOW.get(), betweenHandPos[0].x, betweenHandPos[0].y, betweenHandPos[0].z, 0, 0, 0, true, 0, 0, 0, 0, 5F, 0.95, 0.9,0.35, 1, 1, 40, true, new ParticleComponent[]{
                    new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, new ParticleComponent.KeyTrack(
                            new float[]{0.0f * scale, 0.59f * scale, 0.87f * scale, 0.974f * scale, 0.998f * scale, 1f * scale},
                            new float[]{0, 0.2f, 0.4f, 0.6f, 0.8f, 1f}
                    ), false),
                    new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(1f, 0f), false),
            });
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        timeUntilHeal = HEAL_PAUSE;
        return super.attackEntityFrom(source, damage);
    }

    @Override
    public Animation getDeathAnimation() {
        return DIE_ANIMATION;
    }

    @Override
    public Animation getHurtAnimation() {
        return HURT_ANIMATION;
    }

    private boolean checkBlocksByFeet() {
        BlockState blockLeft;
        BlockState blockRight;
        BlockPos posLeft;
        BlockPos posRight;
        if (direction == 1) {
            posLeft = new BlockPos(MathHelper.floor(getPosX()) + 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) + 1);
            posRight = new BlockPos(MathHelper.floor(getPosX()) - 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) + 1);
            blockLeft = world.getBlockState(posLeft);
            blockRight = world.getBlockState(posRight);
        } else if (direction == 2) {
            posLeft = new BlockPos(MathHelper.floor(getPosX()) - 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) + 1);
            posRight = new BlockPos(MathHelper.floor(getPosX()) - 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) - 1);
            blockLeft = world.getBlockState(posLeft);
            blockRight = world.getBlockState(posRight);
        } else if (direction == 3) {
            posLeft = new BlockPos(MathHelper.floor(getPosX()) - 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) - 1);
            posRight = new BlockPos(MathHelper.floor(getPosX()) + 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) - 1);
            blockLeft = world.getBlockState(posLeft);
            blockRight = world.getBlockState(posRight);
        } else if (direction == 4) {
            posLeft = new BlockPos(MathHelper.floor(getPosX()) + 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) - 1);
            posRight = new BlockPos(MathHelper.floor(getPosX()) + 1, Math.round((float) (getPosY() - 1)), MathHelper.floor(getPosZ()) + 1);
            blockLeft = world.getBlockState(posLeft);
            blockRight = world.getBlockState(posRight);
        } else {
            return false;
        }
//        System.out.println(direction + ", " + (MathHelper.floor(posX) - 1) + ", " + Math.round((float) (posY - 1)) + ", " + MathHelper.floor(posZ) + 1);
        return blockLeft.getMaterial().blocksMovement() || blockRight.getMaterial().blocksMovement();
    }

    private void spawnExplosionParticles(int amount) {
        for (int i = 0; i < amount; i++) {
            final float velocity = 0.25F;
            float yaw = i * (MathUtils.TAU / amount);
            float vy = rand.nextFloat() * 0.1F - 0.05f;
            float vx = velocity * MathHelper.cos(yaw);
            float vz = velocity * MathHelper.sin(yaw);
            world.addParticle(ParticleTypes.FLAME, getPosX(), getPosY() + 1, getPosZ(), vx, vy, vz);
        }
    }

    @Override
    protected void registerData() {
        super.registerData();
        getDataManager().register(DIRECTION, 0);
        getDataManager().register(DIALOGUE, 0);
        getDataManager().register(ANGRY, false);
        getDataManager().register(DESIRES, new ItemStack(Items.GOLD_BLOCK, 7));
        getDataManager().register(TRADED_PLAYERS, new CompoundNBT());
    }

    public int getDirection() {
        return getDataManager().get(DIRECTION);
    }

    public void setDirection(int direction) {
        getDataManager().set(DIRECTION, direction);
    }

    public int getWhichDialogue() {
        return getDataManager().get(DIALOGUE);
    }

    public void setWhichDialogue(int dialogue) {
        getDataManager().set(DIALOGUE, dialogue);
    }

    public boolean getAngry() {
        return getDataManager().get(ANGRY);
    }

    public void setAngry(boolean angry) {
        getDataManager().set(ANGRY, angry);
    }

    public void setDesires(ItemStack stack) {
    	getDataManager().set(DESIRES, stack);
    }

    public ItemStack getDesires() {
    	return getDataManager().get(DESIRES);
    }

    public void setTradedPlayersCompound(ListNBT players) {
        CompoundNBT compound = new CompoundNBT();
        compound.put("players", players);
        getDataManager().set(TRADED_PLAYERS, compound);
    }

    public Set<UUID> getTradedPlayers() {
        Set<UUID> tradedPlayers = new HashSet<>();
        CompoundNBT compound = getDataManager().get(TRADED_PLAYERS);
        ListNBT players = compound.getList("players", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            tradedPlayers.add(NBTUtil.readUniqueId(players.getCompound(i)));
        }
        return tradedPlayers;
    }

    public boolean doesItemSatisfyDesire(ItemStack stack) {
        return canPayFor(stack, getDesires());
    }

    public boolean fulfillDesire(Slot input) {
        ItemStack desires = getDesires();
        if (canPayFor(input.getStack(), desires)) {
            input.decrStackSize(desires.getCount());
            return true;
        }
        return false;
    }

    public boolean hasTradedWith(PlayerEntity player) {
        return getTradedPlayers().contains(PlayerEntity.getUUID(player.getGameProfile()));
    }

    public void rememberTrade(PlayerEntity player) {
        UUID uuid = PlayerEntity.getUUID(player.getGameProfile());
        CompoundNBT compound = getDataManager().get(TRADED_PLAYERS);
        ListNBT players = compound.getList("players", Constants.NBT.TAG_COMPOUND);
        players.add(NBTUtil.writeUniqueId(uuid));
        compound.put("players", players);
        getDataManager().set(TRADED_PLAYERS, compound);
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putInt("direction", getDirection());
        CompoundNBT compoundTradedPlayers = getDataManager().get(TRADED_PLAYERS);
        ListNBT players = compoundTradedPlayers.getList("players", Constants.NBT.TAG_COMPOUND);
        compound.put("players", players);
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        setDirection(compound.getInt("direction"));
        ListNBT players = compound.getList("players", Constants.NBT.TAG_COMPOUND);
        setTradedPlayersCompound(players);
    }


    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {}

    private int getTimeUntilSunstrike() {
        float damageRatio = 1 - getHealthRatio();
        if (damageRatio > 0.6) {
            damageRatio = 0.6f;
        }
        return (int) (SUNSTRIKE_PAUSE_MAX - (damageRatio / 0.6f) * (SUNSTRIKE_PAUSE_MAX - SUNSTRIKE_PAUSE_MIN));
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{DIE_ANIMATION, HURT_ANIMATION, BELLY_ANIMATION, TALK_ANIMATION, SUNSTRIKE_ANIMATION, ATTACK_ANIMATION, SPAWN_ANIMATION, SOLAR_BEAM_ANIMATION, BLESS_ANIMATION, SUPERNOVA_ANIMATION};
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        List<EntityBarakoa> barakoa = getEntitiesNearby(EntityBarakoa.class, 20, 10, 20, 20);
        for (EntityBarakoa entityBarakoa : barakoa) {
            if (entityBarakoa.isBarakoDevoted()) entityBarakoa.timeUntilDeath = rand.nextInt(20);
        }

        super.onDeath(cause);
    }

    public boolean isTrading() {
        return customer != null;
    }

    public PlayerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(PlayerEntity customer) {
        this.customer = customer;
    }

    public void openGUI(PlayerEntity playerEntity) {
        setCustomer(playerEntity);
        MowziesMobs.PROXY.setReferencedMob(this);
        if (!this.world.isRemote && getAttackTarget() == null && isAlive()) {
            playerEntity.openContainer(new INamedContainerProvider() {
                @Override
                public Container createMenu(int id, PlayerInventory playerInventory, PlayerEntity player) {
                    return new ContainerBarakoTrade(id, EntityBarako.this, playerInventory);
                }

                @Override
                public ITextComponent getDisplayName() {
                    return EntityBarako.this.getDisplayName();
                }
            });
        }
    }

    @Override
    protected boolean processInteract(PlayerEntity player, Hand hand) {
        if (canTradeWith(player) && getAttackTarget() == null && isAlive()) {
            openGUI(player);
            return true;
        }
        return false;
    }

    public boolean canTradeWith(PlayerEntity player) {
        if (isTrading() || getHealth() <= 0) {
            return false;
        }
        ItemStack headStack = player.inventory.armorInventory.get(3);
        return headStack.getItem() instanceof BarakoaMask;
    }

    private static boolean canPayFor(ItemStack stack, ItemStack worth) {
        return stack.getItem() == worth.getItem() && stack.getCount() >= worth.getCount();
    }

    @Override
    protected boolean hasBossBar() {
        return ConfigHandler.MOBS.BARAKO.hasBossBar.get();
    }

    @Override
    protected BossInfo.Color bossBarColor() {
        return BossInfo.Color.YELLOW;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return LootTableHandler.BARAKO;
    }

    @Override
    public ILivingEntityData onInitialSpawn(IWorld world, DifficultyInstance difficulty, SpawnReason reason, ILivingEntityData livingData, CompoundNBT compound) {
        if (reason == SpawnReason.SPAWN_EGG) {
            // Try to guess which player spawned Barako, rotate towards them
            List<PlayerEntity> players = getPlayersNearby(5, 5, 5, 5);
            if (!players.isEmpty()) {
                PlayerEntity closestPlayer = players.get(0);
                float closestPlayerDist = 6;
                for (PlayerEntity player : players) {
                    if (player.getHeldItemMainhand().getItem() == ItemHandler.BARAKO_SPAWN_EGG || player.getHeldItemMainhand().getItem() == ItemHandler.BARAKO_SPAWN_EGG) {
                        float thisDist = this.getDistance(player);
                        if (thisDist < closestPlayerDist) {
                            closestPlayer = player;
                            closestPlayerDist = thisDist;
                        }
                    }
                }
                float angle = (float) getAngleBetweenEntities(this, closestPlayer) + 225;
                int direction = (int) (angle / 90) % 4 + 1;
                setDirection(direction);
            }
        }
        return super.onInitialSpawn(world, difficulty, reason, livingData, compound);
    }
}
