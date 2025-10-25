package me.pafias.pranker.commands;

import me.pafias.pranker.Pranker;
import me.pafias.pranker.Victim;
import me.pafias.pranker.pranks.Prank;
import me.pafias.putils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
            sender.sendMessage(CC.tf("&c/%s <player> <prank> [stop]", label));
            return true;
        }
        if (args.length == 1) {
            sender.sendMessage(CC.tf("&c/%s <player> <prank> [stop]", label));
            StringBuilder sb = new StringBuilder();
            for (Prank p : plugin.getPrankManager().getPranks())
                sb.append(p.getName()).append(", ");
            String pranks = sb.toString();
            sender.sendMessage(CC.tf("&6Available pranks: &b%s", pranks));
            return true;
        } else {
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
            Prank prank = plugin.getPrankManager().getPrank(args[1]);
            if (prank == null) {
                sender.sendMessage(CC.t("&cInvalid prank."));
                return true;
            }
            if (prank.getPlayers().contains(target)) {
                if (args.length >= 3 && args[2].equalsIgnoreCase("stop")) {
                    try {
                        plugin.getPrankManager().removePlayer(target, prank);
                        sender.sendMessage(CC.tf("&cStopped prank %s on &b%s", prank.getName(), target.getPlayer().getName()));
                    } catch (Exception ex) {
                        sender.sendMessage(CC.t("&cFailed to stop the prank ripbozo"));
                    }
                } else {
                    sender.sendMessage(CC.t("&cPlayer already has that prank active."));
                }
            } else {
                try {
                    plugin.getPrankManager().addPlayer(target, prank);
                    sender.sendMessage(CC.tf("&aActivated prank &b%s &aon &d%s", prank.getName(), target.getPlayer().getName()));
                } catch (Exception e) {
                    sender.sendMessage(CC.t("&cFailed to activate the prank"));
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> ((Player) sender).canSee(p))
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> list = new ArrayList<>();
            list.add("stop");
            list.addAll(plugin.getPrankManager().getPranks().stream().map(Prank::getID).filter(i -> i.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toSet()));
            return list;
        } else if (args.length == 3)
            return Collections.singletonList("stop");
        return null;
    }

}
