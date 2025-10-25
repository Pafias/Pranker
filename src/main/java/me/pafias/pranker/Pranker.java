package me.pafias.pranker;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import me.pafias.pranker.commands.PrankCommand;
import me.pafias.pranker.listeners.JoinQuitListener;
import me.pafias.pranker.pranks.PrankManager;
import me.pafias.pranker.utils.packets.*;
import me.pafias.putils.pUtils;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Pranker extends JavaPlugin {

    private static Pranker plugin;

    public static Pranker get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        pUtils.setPlugin(plugin);

        final ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isOlderThan(ServerVersion.V_1_17))
            throw new IllegalStateException("Failed to load packet handler: The server version is older than 1.17");
        else if (version.isOlderThan(ServerVersion.V_1_19_3))
            packetHandler = new PacketHandler1_17(); // 1.17 - 1.19.3
        else if (version.isOlderThan(ServerVersion.V_1_20_2))
            packetHandler = new PacketHandler1_19_3(); // 1.19.3 - 1.20.2
        else if (version.isOlderThan(ServerVersion.V_1_21_3))
            packetHandler = new PacketHandler1_20_2(); // 1.20.2 - 1.21.3
        else
            packetHandler = new PacketHandler1_21_3(); // 1.21.3 - latest

        register();
        getServer().getOnlinePlayers().forEach(p -> playerManager.addPlayer(p));
    }

    private PacketHandler packetHandler;
    private PlayerManager playerManager;
    private PrankManager prankManager;

    private void register() {
        final PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new JoinQuitListener(plugin), plugin);
        playerManager = new PlayerManager(plugin);
        prankManager = new PrankManager(plugin);
        getCommand("prank").setExecutor(new PrankCommand(plugin));
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(plugin);
        playerManager.getPlayers().forEach(Victim::stopPranks);
    }

}
