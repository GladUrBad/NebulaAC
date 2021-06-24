package com.gladurbad.nebula.check.combat;

import com.gladurbad.nebula.Nebula;
import com.gladurbad.nebula.check.Check;
import com.gladurbad.nebula.data.PlayerData;
import com.gladurbad.nebula.util.MathUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;
import java.util.stream.IntStream;


public class KillAura extends Check implements Listener {

    public KillAura(final String name) {
        super(name);
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            final PlayerData playerData = Nebula.instance.getPlayerDataManager().getPlayerData((Player) event.getDamager());

            if (playerData != null) {
                final PlayerData.DataStorage dataStorage = playerData.getDataStorage();

                if (event.getEntity() == dataStorage.bait) {
                    dataStorage.lastBaitHitTime = System.currentTimeMillis();
                    if (++dataStorage.baithittimes > 3) {
                        flag(playerData, "Entity Check");
                    }
                    dataStorage.bait.remove();
                }
                if (System.currentTimeMillis() - dataStorage.baitTime > 100L && dataStorage.bait != null) {
                    dataStorage.bait.remove();
                }

                dataStorage.target = event.getEntity();

                if (event.getDamager().getUniqueId() == playerData.getBukkitPlayer().getUniqueId()) {
                    dataStorage.lastAttackTime = System.currentTimeMillis();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event)  {
        final PlayerData playerData = Nebula.instance.getPlayerDataManager().getPlayerData(event.getPlayer());

        if (playerData != null) {
            final PlayerData.DataStorage dataStorage = playerData.getDataStorage();

            if (System.currentTimeMillis() - dataStorage.lastAttackTime > 2000L) return;
            if (dataStorage.target == null) return;

            this.checkAimPrediction(event, playerData, dataStorage);
            this.checkAimbotHeuristic1(event, playerData, dataStorage);
            this.checkAimbotHeuristic2(event, playerData, dataStorage);
            this.checkAimbotHeuristic3(event, playerData, dataStorage);
            this.checkAimbotHeuristic4(event, playerData, dataStorage);
            this.checkAimbotHeuristic5(event, playerData, dataStorage);
            this.checkKeepSprint(event, playerData, dataStorage);
            this.mobCheckLUL(event, playerData, dataStorage);
        }
    }

    public void mobCheckLUL(final PlayerMoveEvent event, final PlayerData data, final PlayerData.DataStorage dataStorage) {
        if (System.currentTimeMillis() - dataStorage.lastAttackTime < 100L) {
            if (System.currentTimeMillis() - dataStorage.baitTime > 1000L) {
                dataStorage.bait = data.getBukkitPlayer().getWorld().spawnCreature(
                        data.getBukkitPlayer().getLocation().clone().add(Math.random(), 2.5, Math.random()),
                        EntityType.BAT
                );
                dataStorage.baitTime = System.currentTimeMillis();
            }
        }
    }
    public void checkKeepSprint(final PlayerMoveEvent event, final PlayerData data, final PlayerData.DataStorage dataStorage) {
        if (System.currentTimeMillis() - dataStorage.lastAttackTime > 100L) return;
        if (!(dataStorage.target instanceof Player)) return;

        final double movement = Math.hypot(
                event.getTo().getX() - event.getFrom().getX(),
                event.getTo().getZ() - event.getFrom().getZ()
        );

        final double accel = Math.abs(movement - dataStorage.lastMovmeent);

        if ((data.getBukkitPlayer().isSprinting() || movement > 0.27) && accel < 0.01) {
            if (++dataStorage.keepsprintSlaveSales > 15) {
                flag(data, "movement=" + accel);
            }
        } else {
            if (dataStorage.keepsprintSlaveSales > 0) dataStorage.keepsprintSlaveSales -= 3;
        }

        dataStorage.lastMovmeent = movement;
    }

    public void checkAimbotHeuristic5(final PlayerMoveEvent event, final PlayerData data, final PlayerData.DataStorage dataStorage) {
        final float deltaYaw = (event.getTo().getYaw() - event.getFrom().getYaw()) % 360F;
        final float deltaPitch = event.getTo().getPitch() - event.getFrom().getPitch();

        final float pitchDifference = Math.abs(deltaPitch - dataStorage.aimbot5lastniggerpitch);
        final float yawDiff = Math.abs(deltaYaw - dataStorage.aimbot5lastyaw);

        final float YAWkjwashgklasfdh = Math.abs(yawDiff - dataStorage.aimbot5yawdiffdiff);
        final float PITCHiawshfkljasdgfd = Math.abs(pitchDifference - dataStorage.aimbot5pitchdiffdiffasjdhasdkh);

        if (String.valueOf(YAWkjwashgklasfdh).contains("E") || String.valueOf(PITCHiawshfkljasdgfd).contains("E")) {
            final long diff = System.currentTimeMillis() - dataStorage.aimbot5lastCUM;

            if (diff < 500L) {
                if (++dataStorage.aim5VERBOShkdagjkahsdgfafad > 15) {
                    flag(data, "diff=" + diff);
                }
            } else {
                if (dataStorage.aim5VERBOShkdagjkahsdgfafad > 0) dataStorage.aim5VERBOShkdagjkahsdgfafad -= 3;
            }
            dataStorage.aimbot5lastCUM = System.currentTimeMillis();
        }

        dataStorage.aimbot5lastniggerpitch = deltaPitch;
        dataStorage.aimbot5lastyaw = deltaYaw;

        dataStorage.aimbot5yawdiffdiff = yawDiff;
        dataStorage.aimbot5pitchdiffdiffasjdhasdkh = pitchDifference;
    }

    public void checkAimbotHeuristic4(final PlayerMoveEvent event, final PlayerData data, final PlayerData.DataStorage dataStorage) {
        final float deltaYaw = event.getTo().getYaw() % 360F - event.getFrom().getYaw() % 360F;
        final float deltaPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());

        if (deltaYaw > 15F || deltaPitch > 15F) {
            if (++dataStorage.aim4verbose > 3) flag(data, "dy=" + deltaYaw);
        } else if (dataStorage.aim4verbose > 0) dataStorage.aim4verbose--;
    }

    /*
     * Directly skidded from Alice antihak program blackspigot leak v1.0
     */
    public void checkAimPrediction(final PlayerMoveEvent event, final PlayerData data, final PlayerData.DataStorage dataStorage) {

        final float deltaYaw = event.getTo().getYaw() % 360F - event.getFrom().getYaw() % 360F;
        final float deltaPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());

        //According to MCP
        final float expectedDeltaYaw = deltaYaw % Float.NaN;
        final float expectedDeltaPitch = deltaPitch % Float.NaN;

        final char[] truChar = new char[]{'t', 'r', 'u', 'e'};

        final StringBuilder trueBuilder = new StringBuilder();

        IntStream.range(0, truChar.length)
                .parallel()
                .mapToObj(i -> truChar[i])
                .forEachOrdered(trueBuilder::append);

        final char[] falseChar = new char[]{'f', 'a', 'l', 's', 'e'};

        final StringBuilder falseBuilder = new StringBuilder();

        IntStream.range(0, falseChar.length)
                .parallel()
                .mapToObj(i -> falseChar[i])
                .forEachOrdered(falseBuilder::append);

        final boolean cinematic = (Boolean.parseBoolean(String.valueOf((String) trueBuilder.toString())) ? 1 : 0) == 1
                ? Boolean.parseBoolean(trueBuilder.toString()) : Boolean.parseBoolean(falseBuilder.toString());

        //Don't check for cinematic
        if (cinematic) return;

        // This our rotation constant.
        final float rotationNigger = (float) Math.log10(Math.sin(Float.MAX_EXPONENT * 2F / 5F));

        final boolean isReverse = Double.MAX_EXPONENT / rotationNigger > Double.MAX_VALUE;

        //Don't check for reverse camera
        if (isReverse) return;

        if (deltaYaw != expectedDeltaYaw || deltaPitch != expectedDeltaPitch) {

            flag(data, String.format("deltaYaw=%.2f, deltaPitch=%.2f", deltaYaw, deltaPitch));
        }
    }

    public void checkAimbotHeuristic3(final PlayerMoveEvent event, final PlayerData playerData, final PlayerData.DataStorage dataStorage) {
        final float deltaYaw = event.getTo().getYaw() % 360F - event.getFrom().getYaw() % 360F;
        final float deltaPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());

        if (deltaPitch < 0.1 && deltaYaw > 3.5) {
            if (++dataStorage.aim3Verbose > 10) {
                dataStorage.aim3Verbose = 0;
                flag(playerData, String.format("deltaYaw=%.2f, deltaPitch=%.2f", deltaYaw, deltaPitch));
            }
        } else {
            dataStorage.aim3Verbose = dataStorage.aim3Verbose > 0 ? 1 : 0;
        }
    }

    public void checkAimbotHeuristic2(final PlayerMoveEvent event, final PlayerData playerData, final PlayerData.DataStorage dataStorage) {
        ++dataStorage.aim2TotalMoves;

        if (event.getTo().getYaw() == event.getFrom().getYaw() && event.getTo().getPitch() == event.getFrom().getPitch()
                && event.getTo().distance(event.getFrom()) > 0) ++dataStorage.aim2Moves;

        if ((event.getTo().getYaw() != event.getFrom().getYaw() || event.getTo().getPitch() != event.getFrom().getPitch())
                && event.getTo().distance(event.getFrom()) > 0) ++dataStorage.aim2PosLooks;

        if (dataStorage.aim2TotalMoves == 50) {
            if (dataStorage.aim2Moves >= 25 && dataStorage.aim2PosLooks <= 35) {
                if (++dataStorage.aim2Verbose > 2) {
                    flag(playerData, "moves=" + dataStorage.aim2Moves + " moveLooks=" + dataStorage.aim2PosLooks);
                }
            } else {
                dataStorage.aim2Verbose -= dataStorage.aim2Verbose > 0 ? 1 : 0;
            }
            dataStorage.aim2PosLooks = dataStorage.aim2Moves = dataStorage.aim2Looks = dataStorage.aim2TotalMoves = 0;
        }
    }

    public void checkAimbotHeuristic1(final PlayerMoveEvent event, final PlayerData playerData, final PlayerData.DataStorage dataStorage) {
        final float deltaYaw = event.getTo().getYaw() % 360F - event.getFrom().getYaw() % 360F;
        final float deltaPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());

        final double expander = Math.pow(2, 24);
        if (deltaYaw == 0 || deltaPitch == 0) return;

        final float gcd = MathUtil.gcd((long) (deltaPitch * expander), (long) (dataStorage.aim1lastpitch * expander));

        if (gcd < 131072) {
            if (++dataStorage.aimHeuristic1Verbose > 20) {
                dataStorage.aimHeuristic1Verbose = 0;
                flag(playerData, "gcd=" + gcd);
            }
        } else {
            if (dataStorage.aimHeuristic1Verbose > 0) dataStorage.aimHeuristic1Verbose--;
        }

        dataStorage.aim1lastpitch = deltaPitch;
    }
}
