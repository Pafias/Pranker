package me.pafias.pranker.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MeowCommand implements CommandExecutor, TabExecutor {

    private static final long COOLDOWN_MILLIS = 30_000L;
    private static final double MAX_RANGE_SQUARED = 10 * 10;

    private static final TextColor PINK = TextColor.color(0xFC9EEC);
    private static final TextColor CYAN = TextColor.color(0x54FCFC);

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can meow.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("/" + label + " <player>", NamedTextColor.RED));
            return true;
        }

        final Player target = player.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Invalid player.", NamedTextColor.RED));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("You can't meow at yourself.", NamedTextColor.RED));
            return true;
        }

        final boolean bypass = player.hasPermission("meow.bypass");

        if (!bypass) {
            final Long lastMeow = cooldowns.get(player.getUniqueId());
            if (lastMeow != null && System.currentTimeMillis() - lastMeow < COOLDOWN_MILLIS) {
                player.sendMessage(Component.text("Your meowing powers are still recharging...", NamedTextColor.RED));
                return true;
            }

            if (!player.getWorld().equals(target.getWorld())
                    || player.getLocation().distanceSquared(target.getLocation()) > MAX_RANGE_SQUARED) {
                player.sendMessage(Component.text("That player is too far away to hear you meow...", NamedTextColor.RED));
                return true;
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURREOW, 1F, 1F);
        target.playSound(target.getLocation(), Sound.ENTITY_CAT_PURREOW, 1F, 1F);

        player.sendMessage(Component.text("You meow at ", PINK)
                .append(Component.text(target.getName(), CYAN))
                .append(Component.text("! :3", PINK)));

        target.sendMessage(Component.text(player.getName(), CYAN)
                .append(Component.text(" meows at you! :3", PINK)));

        if (!bypass)
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1)
            return Collections.emptyList();
        return sender.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
    }

}
