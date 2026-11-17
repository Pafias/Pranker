package me.pafias.pranker.commands;

import me.pafias.pranker.Pranker;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Pranks;
import me.pafias.pranker.pranks.modules.Morph;
import me.pafias.pranker.pranks.modules.Pose;
import me.pafias.putils.CC;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PrankCommand implements CommandExecutor, TabExecutor {

    private final Pranker plugin;

    public PrankCommand(Pranker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(CC.tf("&c/%s <player> <prank> [options|stop]", label));
            return true;
        }
        if (args.length == 1) {
            sender.sendMessage(CC.tf("&c/%s <player> <prank> [options|stop]", label));
            String pranks = Arrays.stream(Pranks.values())
                    .map(p -> p.getArgsUsage() == null ? p.getId() : p.getId() + " &7" + p.getArgsUsage() + "&b")
                    .collect(Collectors.joining(", "));
            sender.sendMessage(CC.tf("&6Available pranks: &b%s", pranks));
            return true;
        }

        Victim target = plugin.getPlayerManager().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(CC.t("&cInvalid player."));
            return true;
        }

        if (args[1].equalsIgnoreCase("stop")) {
            target.stopPranks();
            sender.sendMessage(CC.tf("&cStopped all pranks on &b%s", target.getPlayer().getName()));
            return true;
        }

        Pranks prankType = plugin.getPrankManager().getPrankType(args[1]);
        if (prankType == null) {
            sender.sendMessage(CC.t("&cInvalid prank."));
            return true;
        }

        if (plugin.getPrankManager().hasPrank(target, prankType)) {
            if (args.length >= 3 && args[2].equalsIgnoreCase("stop")) {
                try {
                    plugin.getPrankManager().removePlayer(target, prankType);
                    sender.sendMessage(CC.tf("&cStopped prank %s on &b%s", prankType.getName(), target.getPlayer().getName()));
                } catch (Exception ex) {
                    sender.sendMessage(CC.t("&cFailed to stop the prank ripbozo"));
                }
            } else {
                sender.sendMessage(CC.t("&cPlayer already has that prank active."));
            }
        } else {
            try {
                if (args.length >= 3)
                    plugin.getPrankManager().addPlayer(target, prankType, Arrays.copyOfRange(args, 2, args.length));
                else
                    plugin.getPrankManager().addPlayer(target, prankType);
                sender.sendMessage(CC.tf("&aActivated prank &b%s &aon &d%s", prankType.getName(), target.getPlayer().getName()));
            } catch (Exception e) {
                sender.sendMessage(CC.t("&cFailed to activate the prank: " + e.getMessage()));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> sender instanceof Player player && player.canSee(p))
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            List<String> list = new ArrayList<>();
            list.add("stop");
            list.addAll(java.util.Arrays.stream(Pranks.values())
                    .map(Pranks::getId)
                    .filter(i -> i.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList());
            return list;
        } else if (args.length >= 3) {
            final Pranks prankType = plugin.getPrankManager().getPrankType(args[1]);
            if (prankType == null)
                return Collections.emptyList();
            final Victim target = plugin.getPlayerManager().getPlayer(args[0]);
            if (target != null && plugin.getPrankManager().hasPrank(target, prankType))
                return args.length == 3 ? Collections.singletonList("stop") : Collections.emptyList();
            if (prankType == Pranks.CAGE && args.length == 3)
                return Arrays.stream(Material.values())
                        .filter(m -> !m.isLegacy() && m.isBlock() && m.isSolid())
                        .map(m -> m.name().toLowerCase())
                        .filter(m -> m.startsWith(args[2].toLowerCase()))
                        .toList();
            if (prankType == Pranks.MORPH && args.length == 3)
                return Arrays.stream(EntityType.values())
                        .filter(e -> e != EntityType.UNKNOWN && e != EntityType.PLAYER && e.isAlive())
                        .map(e -> e.name().toLowerCase())
                        .filter(e -> e.startsWith(args[2].toLowerCase()))
                        .toList();
            if (prankType == Pranks.MORPH && args.length >= 4)
                return Morph.options(args[2], args[args.length - 1]);
            if (prankType == Pranks.POSE && args.length == 3)
                return Pose.poses(args[2]);
            if (prankType == Pranks.FORCEFIELD && args.length >= 4)
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .toList();
            if (prankType.getArgsUsage() != null && args.length == 3)
                return Collections.singletonList(prankType.getArgsUsage());
            return Collections.emptyList();
        }
        return null;
    }

}
