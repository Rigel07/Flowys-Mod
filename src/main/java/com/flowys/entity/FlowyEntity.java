package com.flowys.entity;

import com.flowys.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class FlowyEntity extends TamableAnimal {
    public static final int EXPR_NORMAL = 0, EXPR_HAPPY = 1, EXPR_SURPRISED = 2, EXPR_SLEEP = 3;

    private static final EntityDataAccessor<Integer> DATA_EXPRESSION =
            SynchedEntityData.defineId(FlowyEntity.class, EntityDataSerializers.INT);

    /** Cada cuántos ticks suelta un objeto aleatorio (5 minutos = 6000 ticks). */
    private static final int GIFT_INTERVAL = 5 * 60 * 20;
    private static final int PHRASE_COUNT = 12;
    private static final ResourceLocation GIFT_LOOT = new ResourceLocation("flowys", "gameplay/flowy_gift");

    private final SimpleContainer inventory = new SimpleContainer(27) {
        @Override
        public boolean stillValid(Player player) {
            return !FlowyEntity.this.isRemoved() && player.distanceToSqr(FlowyEntity.this) < 64.0D;
        }
    };

    private int giftTimer = GIFT_INTERVAL;
    private int speechTimer = 600;
    private int happyTicks = 0;

    public FlowyEntity(EntityType<? extends FlowyEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.speechTimer = 600 + this.getRandom().nextInt(1200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)          // 3 corazones
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    public static boolean checkSpawnRules(EntityType<FlowyEntity> type, LevelAccessor level,
                                          MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, reason, pos, random);
    }

    // ---------------------------------------------------------------- IA

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return !FlowyEntity.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new StayGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2D, 5.0F, 2.0F, true));
        this.goalSelector.addGoal(5, new FlockGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    /** Se quedan quietas flotando cuando se les ordena. */
    static class StayGoal extends Goal {
        private final FlowyEntity flowy;

        StayGoal(FlowyEntity flowy) {
            this.flowy = flowy;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return flowy.isTame() && flowy.isOrderedToSit();
        }

        @Override
        public void start() {
            flowy.getNavigation().stop();
            flowy.setInSittingPose(true);
        }

        @Override
        public void stop() {
            flowy.setInSittingPose(false);
        }
    }

    /** Las Flowys salvajes tienden a juntarse en grupo. */
    static class FlockGoal extends Goal {
        private final FlowyEntity flowy;
        private FlowyEntity target;

        FlockGoal(FlowyEntity flowy) {
            this.flowy = flowy;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (flowy.isTame() || flowy.getRandom().nextInt(30) != 0) return false;
            List<FlowyEntity> list = flowy.level().getEntitiesOfClass(FlowyEntity.class,
                    flowy.getBoundingBox().inflate(24.0D), f -> f != flowy && !f.isTame());
            FlowyEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (FlowyEntity f : list) {
                double d = flowy.distanceToSqr(f);
                if (d < bestDist) {
                    bestDist = d;
                    best = f;
                }
            }
            if (best == null || bestDist < 36.0D) return false;
            target = best;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !flowy.getNavigation().isDone()
                    && flowy.distanceToSqr(target) > 16.0D;
        }

        @Override
        public void start() {
            flowy.getNavigation().moveTo(target, 1.0D);
        }

        @Override
        public void stop() {
            target = null;
        }
    }

    // ---------------------------------------------------------- Datos

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_EXPRESSION, EXPR_NORMAL);
    }

    public int getExpression() {
        return this.entityData.get(DATA_EXPRESSION);
    }

    private void setExpression(int e) {
        if (getExpression() != e) this.entityData.set(DATA_EXPRESSION, e);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("FlowyInventory", inventory.createTag());
        tag.putInt("GiftTimer", giftTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        inventory.fromTag(tag.getList("FlowyInventory", Tag.TAG_COMPOUND));
        if (tag.contains("GiftTimer")) giftTimer = tag.getInt("GiftTimer");
    }

    // --------------------------------------------------------- Lógica

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        if (--giftTimer <= 0) {
            giftTimer = GIFT_INTERVAL;
            dropGift();
        }
        if (--speechTimer <= 0) {
            speechTimer = 600 + this.getRandom().nextInt(1200);   // 30-90 s
            sayPhrase();
        }
        if (happyTicks > 0) happyTicks--;
        updateExpression();
    }

    private void updateExpression() {
        int e = EXPR_NORMAL;
        if (this.hurtTime > 0) e = EXPR_SURPRISED;
        else if (happyTicks > 0 || this.isInLove()) e = EXPR_HAPPY;
        else if (this.isOrderedToSit() && this.level().isNight()) e = EXPR_SLEEP;
        setExpression(e);
    }

    private void dropGift() {
        if (!(this.level() instanceof ServerLevel server)) return;
        LootTable table = server.getServer().getLootData().getLootTable(GIFT_LOOT);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT);
        table.getRandomItems(params).forEach(stack -> this.spawnAtLocation(stack));
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
    }

    private void sayPhrase() {
        List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(10.0D));
        if (players.isEmpty()) {
            speechTimer = 200;
            return;
        }
        String key = "message.flowys.phrase." + (1 + this.getRandom().nextInt(PHRASE_COUNT));
        Component msg = Component.translatable("chat.type.text", this.getDisplayName(), Component.translatable(key));
        for (Player p : players) p.sendSystemMessage(msg);
        happyTicks = 60;
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.7F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7D, getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    // ----------------------------------------------------- Interacción

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame()) {
            if (stack.is(Items.WATER_BUCKET)) {
                if (!this.level().isClientSide) {
                    this.tame(player);
                    this.setOrderedToSit(false);
                    this.setHealth(this.getMaxHealth());
                    this.getNavigation().stop();
                    this.happyTicks = 100;
                    this.level().broadcastEntityEvent(this, (byte) 7);   // corazones
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return InteractionResult.PASS;
        }

        // Domesticada: una flor para reproducirlas
        if (this.isFood(stack)) {
            return super.mobInteract(player, hand);
        }

        if (this.isOwnedBy(player) && hand == InteractionHand.MAIN_HAND) {
            if (!this.level().isClientSide) {
                if (player.isSecondaryUseActive()) {
                    // Agachado + clic derecho: abrir inventario
                    if (player instanceof ServerPlayer sp) {
                        sp.openMenu(new SimpleMenuProvider(
                                (id, inv, p) -> ChestMenu.threeRows(id, inv, this.inventory),
                                this.getDisplayName()));
                    }
                } else {
                    // Clic derecho: seguir / quedarse quieta
                    boolean sit = !this.isOrderedToSit();
                    this.setOrderedToSit(sit);
                    this.getNavigation().stop();
                    this.happyTicks = 40;
                    player.displayClientMessage(Component.translatable(
                            sit ? "message.flowys.stay" : "message.flowys.follow"), true);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SUNFLOWER) || stack.is(ItemTags.SMALL_FLOWERS);
    }

    @Override
    public boolean canFallInLove() {
        return this.isTame() && super.canFallInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        FlowyEntity baby = ModEntities.FLOWY.get().create(level);
        if (baby != null && this.getOwnerUUID() != null) {
            baby.setOwnerUUID(this.getOwnerUUID());
            baby.setTame(true);
        }
        return baby;
    }

    // ------------------------------------------------- Daño / muerte

    /** Domesticadas: inmortales (solo /kill y el vacío las afectan). */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.isTame()) return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        return super.isInvulnerableTo(source);
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        super.dropAllDeathLoot(source);   // objeto aleatorio (loot_tables/entities/flowy.json)
        Containers.dropContents(this.level(), this.blockPosition(), this.inventory);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    // ---------------------------------------------------------- Sonidos

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
