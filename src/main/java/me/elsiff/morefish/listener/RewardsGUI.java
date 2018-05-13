package me.elsiff.morefish.listener;

import me.elsiff.morefish.MoreFish;
import me.elsiff.morefish.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RewardsGUI implements Listener {
    private final MoreFish plugin;
    private final Set<UUID> users = new HashSet<>();
    private final Map<UUID, Integer> editors = new HashMap<>();

    public RewardsGUI(MoreFish plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        String title = plugin.getFishConfiguration().getString("rewards-gui-title");
        Inventory inv = plugin.getServer().createInventory(player, (plugin.hasEconomy() ? 27 : 18), title);

        ItemStack[] rewards = plugin.getContestManager().getRewards();

        for (int i = 0; i < 8 && i < rewards.length; i ++) {
            inv.setItem(i, rewards[i]);
        }

        ItemStack iconGuide = new ItemBuilder(Material.SIGN)
                .setDisplayName(plugin.getFishConfiguration().getString("rewards-guide-icon-name"))
                .setLore(plugin.getFishConfiguration().getStringList("rewards-guide-icon-lore"))
                .build();

        inv.setItem(8, iconGuide);

        String[] cmdPrizes = plugin.getContestManager().getCmdRewards();

        for (int i = 0; i < 8 && i < cmdPrizes.length; i ++) {
            String ordinal = plugin.getOrdinal(i + 1);
            String number = Integer.toString(i + 1);

            ItemStack iconCommand = new ItemBuilder(Material.COMMAND)
                .setDisplayName(plugin.getFishConfiguration().getString("rewards-cmd-icon-name")
                    .replaceAll("%ordinal%", ordinal)
                    .replaceAll("%number%", number))
                .setLore(plugin.getFishConfiguration().getStringList("rewards-cmd-icon-lore"))
                .build();
            inv.setItem(9 + i, iconCommand);
        }

        ItemStack iconCmdGuide = new ItemBuilder(Material.SIGN)
            .setDisplayName(plugin.getFishConfiguration().getString("rewards-sign-icon-name"))
            .setLore(plugin.getFishConfiguration().getStringList("rewards-sign-icon-lore"))
            .build();

        inv.setItem(17, iconCmdGuide);

        if (plugin.hasEconomy()) {
            double[] cashPrizes = plugin.getContestManager().getCashPrizes();

            for (int i = 0; i < 8; i ++) {
                double amount = cashPrizes[i];

                String ordinal = plugin.getOrdinal(i + 1);
                String number = Integer.toString(i + 1);

                if (i == 7) {
                    String text = plugin.getFishConfiguration().getString("rewards-consolation");
                    ordinal = text;
                    number = text;
                }

                ItemStack iconEmerald = new ItemBuilder(Material.EMERALD)
                        .setDisplayName(plugin.getFishConfiguration().getString("rewards-emerald-icon-name")
                                .replaceAll("%ordinal%", ordinal)
                                .replaceAll("%number%", number)
                                .replaceAll("%amount%", Double.toString(amount)))
                        .setLore(plugin.getFishConfiguration().getStringList("rewards-emerald-icon-lore"))
                        .build();

                inv.setItem(18 + i, iconEmerald);
            }

            ItemStack iconMoneyGuide = new ItemBuilder(Material.SIGN)
                    .setDisplayName(plugin.getFishConfiguration().getString("rewards-sign-icon-name"))
                    .setLore(plugin.getFishConfiguration().getStringList("rewards-sign-icon-lore"))
                    .build();

            inv.setItem(26, iconMoneyGuide);
        }

        player.openInventory(inv);
        users.add(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (users.contains(event.getWhoClicked().getUniqueId())) {
            if (event.getRawSlot() < 9 || event.getInventory().getSize() < event.getRawSlot()) {
                return;
            }

            event.setCancelled(true);

            if (9 <= event.getRawSlot() && event.getRawSlot() <= 16) {
                editors.put(event.getWhoClicked().getUniqueId(), event.getRawSlot() - 9);

                event.getWhoClicked().closeInventory();

                for (String msg : plugin.getFishConfiguration().getStringList("enter-cmd-prize")) {
                    event.getWhoClicked().sendMessage(msg);
                }
            }

            if (18 <= event.getRawSlot() && event.getRawSlot() <= 25 && plugin.hasEconomy()) {
                editors.put(event.getWhoClicked().getUniqueId(), event.getRawSlot() - 9);

                event.getWhoClicked().closeInventory();

                for (String msg : plugin.getFishConfiguration().getStringList("enter-cash-prize")) {
                    event.getWhoClicked().sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        if (editors.containsKey(id)) {
            event.setCancelled(true);

            if ("cancel".equalsIgnoreCase(event.getMessage())) {
                editors.remove(id);
                openGUI(event.getPlayer());

                event.getPlayer().sendMessage(plugin.getFishConfiguration().getString("entered-cancel"));
                return;
            }

            int index = editors.get(id);

            if (index >= 18 && index <= 26) {
                double value;
                try {
                    value = Double.parseDouble(event.getMessage());
                } catch (NumberFormatException ex) {
                    event.getPlayer().sendMessage(
                        String.format(plugin.getFishConfiguration().getString("entered-not-number"), event.getMessage()));
                    return;
                }

                if (value < 0) {
                    event.getPlayer().sendMessage(plugin.getFishConfiguration().getString("entered-not-positive"));
                    return;
                }
                double[] cashPrizes = plugin.getContestManager().getCashPrizes();
                cashPrizes[index] = value;
                plugin.getContestManager().setCashPrizes(cashPrizes);
            } else {
                String[] cmdPrizes = plugin.getContestManager().getCmdRewards();
                cmdPrizes[index] = event.getMessage();
                plugin.getContestManager().setCmdRewards(cmdPrizes);
            }
            editors.remove(id);
            openGUI(event.getPlayer());

            event.getPlayer().sendMessage(plugin.getFishConfiguration().getString("entered-successfully"));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (users.contains(event.getPlayer().getUniqueId())) {
            users.remove(event.getPlayer().getUniqueId());

            ItemStack[] rewards = new ItemStack[8];

            for (int i = 0; i < 8; i ++) {
                ItemStack stack = event.getInventory().getItem(i);

                if (stack == null || stack.getType() == Material.AIR)
                    continue;

                rewards[i] = stack;
            }

            plugin.getContestManager().setRewards(rewards);

            if (!editors.containsKey(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage(plugin.getFishConfiguration().getString("saved-changes"));
            }
        }
    }
}
