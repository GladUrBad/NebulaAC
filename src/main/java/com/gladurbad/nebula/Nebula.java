package com.gladurbad.nebula;

import com.gladurbad.nebula.check.combat.KillAura;
import com.gladurbad.nebula.check.combat.Reach;
import com.gladurbad.nebula.check.movement.Fly;
import com.gladurbad.nebula.check.movement.Speed;
import com.gladurbad.nebula.listener.JoinQuitListener;
import com.gladurbad.nebula.manager.PlayerDataManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class Nebula extends JavaPlugin {

    public static Nebula instance;

    private final PlayerDataManager playerDataManager = new PlayerDataManager();

    @Override
    public void onEnable() {
        instance = this;

        final PluginManager pluginManager = Bukkit.getPluginManager();

        /*
        Parallel streams will always be faster, I did testing with it on Kauri.

        ~ Dawson Hessler - 1992
         */
        Bukkit.getServer().getOnlinePlayers().parallelStream().forEach(playerDataManager::add);

        pluginManager.registerEvents(new JoinQuitListener(), this);
        pluginManager.registerEvents(new Reach("Reach"), this);
        pluginManager.registerEvents(new Speed("Speed"), this);
        pluginManager.registerEvents(new Fly("Fly"), this);
        pluginManager.registerEvents(new KillAura("KillAura"), this);
    }

    @Override
    public void onDisable() {
    }
}
