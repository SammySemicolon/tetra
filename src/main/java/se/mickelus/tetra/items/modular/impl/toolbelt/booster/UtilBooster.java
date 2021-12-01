package se.mickelus.tetra.items.modular.impl.toolbelt.booster;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.client.CInputPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.modular.impl.toolbelt.ToolbeltHelper;
import se.mickelus.tetra.items.modular.impl.toolbelt.inventory.QuickslotInventory;
import se.mickelus.tetra.items.modular.impl.toolbelt.inventory.StorageInventory;
import se.mickelus.tetra.items.modular.impl.toolbelt.inventory.ToolbeltInventory;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.util.CastOptional;

public class UtilBooster {

    public static final String activeKey = "booster.active";
    public static final String chargedKey = "booster.charged";
    public static final String fuelKey = "booster.fuel";
    public static final String bufferKey = "booster.buffer";
    public static final String cooldownKey = "booster.cooldown";

    public static final int fuelCapacity = 110;
    public static final int fuelCost = 1;
    public static final int fuelCostCharged = 40;
    public static final int fuelRecharge = 1;
    public static final int cooldownTicks = 20;

    public static final int gunpowderGain = 80;

    public static final float boostStrength = 0.04f;
    public static final float chargedBoostStrength = 1.2f;
    public static final float boostLevelMultiplier = 0.4f;

    public static boolean hasBooster(PlayerEntity player) {
        ItemStack itemStack = ToolbeltHelper.findToolbelt(player);

        return canBoost(itemStack);
    }

    public static boolean canBoost(ItemStack itemStack) {
        return getBoosterLevel(itemStack) > 0;
    }

    public static int getBoosterLevel(ItemStack itemStack) {
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof IModularItem) {
            IModularItem item = (IModularItem) itemStack.getItem();
            return item.getEffectLevel(itemStack, ItemEffect.booster);
        }

        return 0;
    }


    public static boolean hasFuel(CompoundNBT tag, boolean charged) {
        if (charged) {
            return tag.getInt(fuelKey) >= fuelCostCharged;
        }
        return tag.getInt(fuelKey) >= fuelCost;
    }

    public static int getFuel(CompoundNBT tag) {
        return tag.getInt(fuelKey);
    }

    public static float getFuelPercent(CompoundNBT tag) {
        return tag.getInt(fuelKey) * 1F / fuelCapacity;
    }

    public static void boostPlayer(PlayerEntity player, CompoundNBT tag, int level) {
        float boostBase = boostStrength + boostStrength * (level - 1) * 0.4f;
        if (player.isFallFlying()) {
            Vector3d Vector3d = player.getLookAngle();
            player.push(
                    Vector3d.x * 0.01f + (Vector3d.x * 1.5f - player.getDeltaMovement().x) * 0.05f,
                    Vector3d.y * 0.01f + (Vector3d.y * 1.5f - player.getDeltaMovement().y) * 0.05f,
                    Vector3d.z * 0.01f + (Vector3d.z * 1.5f - player.getDeltaMovement().z) * 0.05f);
        } else if (player.getDeltaMovement().y > -0.1) {
            if (player.isCrouching()) {
                player.push(0, boostBase / 1.5, 0);
            } else {
                player.push(0, boostBase, 0);
            }
            player.fallDistance = 0;
        } else {
            player.push(0, boostBase + 0.8 * -player.getDeltaMovement().y, 0);
        }

        if (player.level instanceof ServerWorld) {
            ((ServerWorld) player.level).sendParticles(ParticleTypes.SMOKE,
                    player.getX() - 0.2 + Math.random() * 0.4,
                    player.getY() + Math.random() * 0.2,
                    player.getZ() - 0.2 + Math.random() * 0.4,
                    8, 0, -0.3, 0, 0.1D);

            if (Math.random() > 0.3) {
                ((ServerWorld)player.level).sendParticles(ParticleTypes.FLAME,
                        player.getX() - 0.2 + Math.random() * 0.4,
                        player.getY() + Math.random() * 0.2,
                        player.getZ() - 0.2 + Math.random() * 0.4,
                        1, 0, -0.3, 0, 0.1D);
            }
        }
    }

    public static void boostHorizontal(PlayerEntity player) {
        if (player.zza != 0 || player.xxa != 0) {
            ItemStack itemStack = ToolbeltHelper.findToolbelt(player);
            int level = UtilBooster.getBoosterLevel(itemStack);
            if (level > 0) {

                // todo: needs a custom packet for syncing moveStrafing & moveForward to the server, CInputPacket only works when riding something
                CastOptional.cast(player, ClientPlayerEntity.class).ifPresent(cp -> {
                    cp.connection.send(new CInputPacket(cp.xxa, cp.zza, cp.input.jumping, cp.input.shiftKeyDown));
                });

                CompoundNBT tag = itemStack.getOrCreateTag();

                if (UtilBooster.hasFuel(tag, false)) {
                    UtilBooster.consumeFuel(tag, false);

                    player.moveRelative(0.05f, new Vector3d(player.xxa, player.yya, player.zza));

                    if (player.level.isClientSide) {
                        Vector3d direction = getAbsoluteMotion(-player.xxa, -player.zza, player.yRot);
                        for (int i = 0; i < 8; i++) {
                            player.getCommandSenderWorld().addParticle(ParticleTypes.SMOKE,
                                    player.getX(), player.getY() + player.getBbHeight() * 0.4, player.getZ(),
                                    Math.random() * (0.2 * direction.x + 0.07) -0.05,
                                    Math.random() * 0.1 - 0.05,
                                    Math.random() * (0.2 * direction.z + 0.07) -0.05);
                        }

                        if (Math.random() > 0.3) {
                            player.getCommandSenderWorld().addParticle(ParticleTypes.FLAME,
                                    player.getX(), player.getY() + player.getBbHeight() * 0.4, player.getZ(),
                                    Math.random() * (0.2 * direction.x + 0.07) -0.05,
                                    Math.random() * 0.1 - 0.05,
                                    Math.random() * (0.2 * direction.z + 0.07) -0.05);
                        }
                    }

                }
            }
        }
    }

    private static Vector3d getAbsoluteMotion(float strafe, float forward, float facing) {
        float sin = MathHelper.sin(facing * ((float) Math.PI / 180F));
        float cos = MathHelper.cos(facing * ((float) Math.PI / 180F));
        return new Vector3d(strafe * cos - forward * sin, 0, forward * cos + strafe * sin);
    }

    public static void boostPlayerCharged(PlayerEntity player, CompoundNBT tag, int level) {
        float boostBase = chargedBoostStrength + chargedBoostStrength * (level - 1) * boostLevelMultiplier;
        Vector3d lookVector = player.getLookAngle();

        // current velocity projected onto the look vector
        player.setDeltaMovement(lookVector.scale(player.getDeltaMovement().dot(lookVector) / lookVector.dot(lookVector)));

        player.push(
                lookVector.x * boostBase,
                Math.max(lookVector.y * boostBase / 2 + 0.3, 0.1),
                lookVector.z * boostBase);
        player.hurtMarked = true;

        player.move(MoverType.SELF, new Vector3d(0, 0.4, 0));

        if (player.level instanceof ServerWorld) {
            ((ServerWorld)player.level).sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(),
                    player.getY() + player.getBbHeight() * 0.4, player.getZ(), 10, 0,
                    -0.1, 0, 0.1D);
            ((ServerWorld)player.level).sendParticles(ParticleTypes.FLAME, player.getX(),
                    player.getY() + player.getBbHeight() * 0.4, player.getZ(), 3, 0,
                    -0.1, 0, 0.1D);
        }
    }

    public static void consumeFuel(CompoundNBT tag, boolean charged) {
        if (charged) {
            tag.putInt(fuelKey, tag.getInt(fuelKey) - fuelCostCharged);
        } else {
            tag.putInt(fuelKey, tag.getInt(fuelKey) - fuelCost);
        }
        tag.putInt(cooldownKey, cooldownTicks);
    }

    public static void consumeFuel(CompoundNBT tag, int amount) {
        tag.putInt(fuelKey, tag.getInt(fuelKey) - amount);
        tag.putInt(cooldownKey, cooldownTicks);
    }

    public static void rechargeFuel(CompoundNBT tag, ItemStack itemStack) {
        int fuel = tag.getInt(fuelKey);
        int buffer = tag.getInt(bufferKey);
        int cooldown = tag.getInt(cooldownKey);
        if (cooldown > 0) {
            tag.putInt(cooldownKey, cooldown - 1);
        } else if (fuel + fuelRecharge < fuelCapacity) {
            if (buffer > 0) {
                tag.putInt(fuelKey, fuel + fuelRecharge);
                tag.putInt(bufferKey, buffer - 1);
            } else {
                refuelBuffer(tag, itemStack);
            }
        }
    }

    private static void refuelBuffer(CompoundNBT tag, ItemStack itemStack) {
        ToolbeltInventory inventory = new QuickslotInventory(itemStack);
        int index = inventory.getFirstIndexForItem(Items.GUNPOWDER);
        if (index != -1) {
            inventory.removeItem(index, 1);
            tag.putInt(bufferKey, gunpowderGain);
            return;
        }

        inventory = new StorageInventory(itemStack);
        index = inventory.getFirstIndexForItem(Items.GUNPOWDER);
        if (index != -1) {
            inventory.removeItem(index, 1);
            tag.putInt(bufferKey, gunpowderGain);
            return;
        }

        tag.putInt(cooldownKey, cooldownTicks);
    }

    public static boolean isActive(CompoundNBT tag) {
        return tag.getBoolean(activeKey);
    }

    public static void setActive(CompoundNBT tag, boolean active, boolean charged) {
        tag.putBoolean(activeKey, active);
        if (charged) {
            tag.putBoolean(chargedKey, charged);
        }
    }

}
