package me.pafias.pranker;

import me.pafias.pranker.commands.PrankCommand;
import me.pafias.pranker.listeners.JoinQuitListener;
import me.pafias.pranker.pranks.PrankManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pranker extends JavaPlugin {

    private static Pranker plugin;

    public static Pranker get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        register();
        getServer().getOnlinePlayers().forEach(p -> playerManager.addPlayer(p));
    }

    private PlayerManager playerManager;

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    private PrankManager prankManager;

    public PrankManager getPrankManager() {
        return prankManager;
    }

    private void register() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new JoinQuitListener(plugin), plugin);
        playerManager = new PlayerManager(plugin);
        prankManager = new PrankManager(plugin);
        PrankCommand prankCommand = new PrankCommand(plugin);
        getCommand("prank").setExecutor(prankCommand);
        getCommand("prank").setTabCompleter(prankCommand);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(plugin);
        playerManager.getPlayers().forEach(Victim::stopPranks);
        plugin = null;
    }

}
