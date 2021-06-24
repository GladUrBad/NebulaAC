package com.gladurbad.nebula.check.movement;

import com.gladurbad.nebula.Nebula;
import com.gladurbad.nebula.check.Check;
import com.gladurbad.nebula.data.PlayerData;
import com.gladurbad.nebula.util.MovementUtil;
import com.gladurbad.nebula.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Speed extends Check implements Listener {

    private static final Map<Integer, Double> JUMPS = new HashMap<>();

    public Speed(final String name) {
        super(name);
        JUMPS.put(1, 0.42);
        JUMPS.put(2, 0.3332);
        JUMPS.put(3, 0.248136);
        JUMPS.put(4, 0.164773);
        JUMPS.put(5, 0.083078);
        JUMPS.put(6, 0.0);
        JUMPS.put(7, -0.0784);
        JUMPS.put(8, -0.155232);
        JUMPS.put(9, -0.230527);
        JUMPS.put(10, -0.304316);
        JUMPS.put(11, -0.376630);
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        final PlayerData playerData = Nebula.instance.getPlayerDataManager().getPlayerData(event.getPlayer());

        if (playerData != null) {
            final PlayerData.DataStorage dataStorage = playerData.getDataStorage();

            this.checkJumpSpeed(event, playerData, dataStorage);
            this.checkYPort(event, playerData, dataStorage);
            this.checkMaximumSpeed(event, playerData, dataStorage);
            this.checkEventSpeed(event, playerData, dataStorage);
        }
    }

    public void checkJumpSpeed(final PlayerMoveEvent event, final PlayerData data, final PlayerData.DataStorage dataStorage) {
        check: {
            if (data.getBukkitPlayer().hasPotionEffect(PotionEffectType.JUMP)) break check;
            if (System.currentTimeMillis() - dataStorage.lastTeleport < 2000L) break check;
            if (System.currentTimeMillis() - dataStorage.lastDamageTakenTime < 1500L) break check;

            final double deltaY = event.getTo().getY() - event.getFrom().getY();

            if (deltaY > 0.5) {
                flag(data, "jumpspeed=" + deltaY);
            }
        }
    }
    public void checkYPort(final PlayerMoveEvent event, final PlayerData playerData, final PlayerData.DataStorage dataStorage) {
        if (dataStorage.speedSinceUnderBlockTicks < 40) return;
        if (playerData.getBukkitPlayer().hasPotionEffect(PotionEffectType.JUMP)) return;
        if (System.currentTimeMillis() - dataStorage.lastDamageTakenTime < 1500L) return;

        final List<Block> collidingBlocks = MovementUtil.getCollidingBlocks(event.getTo());

        dataStorage.speedYportAirTicks = !playerData.getBukkitPlayer().isOnGround() ? dataStorage.speedYportAirTicks + 1 : 0;

        final double realDeltaY = event.getTo().getY() - event.getFrom().getY();
        final double deltaY = dataStorage.speedLastDeltaY;

        final double deltaXz = Math.hypot(
                event.getTo().getX() - event.getFrom().getX(),
                event.getTo().getZ() - event.getFrom().getZ()
        );

        if (deltaXz == 0) return;

        if (dataStorage.speedYportAirTicks == 1) {
            dataStorage.speedYportShouldCheck = deltaY > 0;
        }

        if (realDeltaY > 0 && deltaY <= 0) {
            dataStorage.speedYportShouldCheck2 = collidingBlocks.stream().noneMatch(block -> block.getType() == Material.SLIME_BLOCK);
        }

        if (!dataStorage.speedYportShouldCheck || !dataStorage.speedYportShouldCheck2) return;

        final double expectedY = JUMPS.getOrDefault(dataStorage.speedYportAirTicks, 0D);

        if (expectedY != 0D && JUMPS.values().stream().allMatch(value -> Math.abs(value - expectedY) > 0.01)) {
            final double difference = Math.abs(deltaY - expectedY);

            if (difference > 0.01) {
                if (++dataStorage.speedYportVerbose > 10) {
                    flag(playerData, String.format("edy=%.2f, dy=%.2f", expectedY, deltaY));
                }
            } else {
                dataStorage.speedYportVerbose -= dataStorage.speedYportVerbose > 0 ? 0.25 : 0;
            }
        }
    }

    public void checkEventSpeed(final PlayerMoveEvent event, final PlayerData playerData, final PlayerData.DataStorage dataStorage) {
        final long delta = System.currentTimeMillis() - dataStorage.lastEventSpeedTimestamp;
        ++dataStorage.eventSpeedTicks;
        if ((dataStorage.totalEventSpeedTime += delta) > 1000L) {
            if (dataStorage.eventSpeedTicks > 22) {
                if (++dataStorage.eventSpeedVerbose > 3) {
                    flag(playerData, "ticks=" + dataStorage.eventSpeedTicks);
                }
            } else {
                dataStorage.eventSpeedVerbose = 0;
            }
            dataStorage.eventSpeedTicks = 0;
            dataStorage.totalEventSpeedTime = 0L;
        }
        dataStorage.lastEventSpeedTimestamp = System.currentTimeMillis();
    }

    public void checkMaximumSpeed(final PlayerMoveEvent event, final PlayerData playerData, final PlayerData.DataStorage dataStorage) {
        final Player player = playerData.getBukkitPlayer();

        if (dataStorage.speedLastLocation == null) {
            dataStorage.speedLastLocation = player.getLocation();return;
        }
        if (System.currentTimeMillis() - dataStorage.lastDamageTakenTime < 1500L) return;
        if (playerData.getBukkitPlayer().getWalkSpeed() > 0.2F) return;

        double speed = 0.0;

        final double deltaX = event.getTo().getX() - event.getFrom().getX();
        final double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
        final double deltaXZ = Math.hypot(deltaX, deltaZ);
        final double deltaY = event.getTo().getY() - event.getFrom().getY();

        dataStorage.speedAirTicks = !player.isOnGround() ? dataStorage.speedAirTicks + 1 : 0;
        dataStorage.speedGroundTicks = player.isOnGround() ? dataStorage.speedGroundTicks + 1 : 0;

        final List<Block> collidingBlocks = MovementUtil.getCollidingBlocks(dataStorage.speedLastLocation);

        dataStorage.speedSinceIceTicks = collidingBlocks.stream()
                .noneMatch(block -> block.getType().toString().contains("ICE")) ?
                dataStorage.speedSinceIceTicks + 1 :
                0;
        dataStorage.speedSinceSlimeTicks = collidingBlocks.stream()
                .noneMatch(block -> block.getType().toString().contains("SLIME")) ?
                dataStorage.speedSinceSlimeTicks + 1 :
                0;
        dataStorage.speedSinceUnderBlockTicks = collidingBlocks.stream()
                .anyMatch(block -> block.getLocation().getY() > player.getEyeLocation().getY() - 0.5)
                || playerData.getBukkitPlayer().getEyeLocation().clone().add(0, 1, 0).getBlock().getType() != Material.AIR ?
                dataStorage.speedSinceIceTicks + 1 :
                0;

        double maxGroundSpeed = getSpeed(0.287D, player);
        double maxAirSpeed = getSpeed(0.362D, player);
        double maxAfterJumpAirSpeed = getAfterJumpSpeed(player);

        final int airTicks = dataStorage.speedAirTicks;
        final int groundTicks = dataStorage.speedGroundTicks;

        final int sinceIceTicks = dataStorage.speedSinceIceTicks;
        final int sinceSlimeTicks = dataStorage.speedSinceSlimeTicks;
        final int sinceUnderBlockTicks = dataStorage.speedSinceUnderBlockTicks;

        final double realDXZ = dataStorage.speedLastDeltaXZ;
        final double realDY = dataStorage.speedLastDeltaY;

        //Handle jumping speed increase.
        if (realDY > 0.4199 && airTicks == 1) {
            speed = realDXZ/maxAfterJumpAirSpeed;
        }

        //Handle max air speed checking. (airTicks > 1 because of jumping increase first tick)
        //Handle max air speed increase based on edge cases. (e.g. sprint jump on ice, under block, slime block)
        if (airTicks > 1 || (airTicks > 0 && realDY < 0.4199)) {
            if (sinceUnderBlockTicks <= 15) maxAirSpeed += 0.3;
            if (sinceIceTicks <= 15 || sinceSlimeTicks <= 10) maxAirSpeed += 0.25;
            speed = realDXZ/maxAirSpeed;
        }

        //Handle max ground speed checking. (groundTicks > 1 because of landing speed increase)
        //Landing speed increase lasts for a few ticks (5-7) so check for that.
        if (groundTicks > 0) {
            if (groundTicks < 7) maxGroundSpeed += 0.17;
            if (sinceUnderBlockTicks <= 15) maxGroundSpeed += 0.15;
            if (sinceIceTicks <= 15 || sinceSlimeTicks <= 10) maxGroundSpeed += 0.2;
            speed = realDXZ/maxGroundSpeed;
        }

        final int shiftedSpeed = (int) Math.round(speed * 100);

        if (shiftedSpeed > 100) {
            if ((dataStorage.speedVerbose += shiftedSpeed > 150 ? 60 : 20) > 100 || shiftedSpeed > 200) {
                dataStorage.speedVerbose = Math.min(350, dataStorage.speedVerbose);
                flag(playerData, "speed=" + shiftedSpeed + "%");
                dataStorage.speedNiggerTPticks = 5;
            }
        } else {
            dataStorage.speedVerbose = Math.max(0, dataStorage.speedVerbose);
        }

        if (dataStorage.speedNiggerTPticks > 0) {
            dataStorage.speedNiggerTPticks--;
            player.teleport(event.getFrom());
        }

        dataStorage.speedLastDeltaXZ = deltaXZ;
        dataStorage.speedLastDeltaY = deltaY;
        dataStorage.speedLastLocation = player.getLocation();
    }


    private double getSpeed(double movement, final Player player) {
        if (PlayerUtil.getPotionLevel(player, PotionEffectType.SPEED) > 0) {
            movement *= 1.0D + 0.2D * (double)(PlayerUtil.getPotionLevel(player, PotionEffectType.SPEED));
        }
        return movement;
    }

    //Slightly inaccurate, maybe going to improve the math on this one more later.
    private double getAfterJumpSpeed(final Player player) {
        return 0.62 + 0.033 * (double) (PlayerUtil.getPotionLevel(player, PotionEffectType.SPEED));
    }
}

