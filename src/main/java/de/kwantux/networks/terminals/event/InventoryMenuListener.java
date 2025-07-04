package de.kwantux.networks.terminals.event;

import de.kwantux.networks.Sorter;
import de.kwantux.networks.terminals.inventory.InventoryMenu;
import de.kwantux.networks.terminals.inventory.InventoryMenuManager;
import de.kwantux.networks.terminals.TerminalsPlugin;
import de.kwantux.networks.utils.PositionedItemStack;
import de.kwantux.networks.utils.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

import static de.kwantux.networks.terminals.util.Keys.NETWORKS_MENU_ICON;

public class InventoryMenuListener implements Listener {

    public InventoryMenuListener(TerminalsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClicked(InventoryClickEvent event) {

        Player player = (Player) event.getWhoClicked();
        InventoryMenu menu = InventoryMenuManager.getMenuForPlayer(player);
        if (menu == null) return;

        if (event.getCurrentItem() != null) {
            // Handle menu controls
            if (event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(NETWORKS_MENU_ICON, PersistentDataType.INTEGER)) {
                event.setCancelled(true);
                switch (event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NETWORKS_MENU_ICON, PersistentDataType.INTEGER)) {
                    case 0:
                        player.closeInventory();
                        return;

                    case 1:
                        menu.toFirstPage();
                        return;

                    case 2:
                        menu.decrementPage();
                        return;

                    case 3:
                        menu.incrementPage();
                        return;

                    case 4:
                        menu.toLastPage();
                        return;
                    case null, default:
                        return;
                }
            }
        }


        // Handle item actions
        switch (event.getAction()) {
            case PLACE_ONE,PLACE_SOME, PLACE_ALL:
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) return;
                assert event.getCursor() != null;
                Set<Transaction> transactions = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(event.getCursor(), null, 0)));
                for (Transaction transaction : transactions) {
                    if (!event.isCancelled()) {
                        event.setCancelled(false);
                        Sorter.addItem(transaction);
                        menu.updateInventory();
                        return;
                    }
                }
                event.setCancelled(true);
                return;

            case COLLECT_TO_CURSOR, PICKUP_ONE, PICKUP_HALF, PICKUP_SOME, PICKUP_ALL, DROP_ALL_CURSOR, DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT:
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) return;
                Set<Transaction> transactions1 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(event.getCurrentItem(), null, 0)));
                for (Transaction transaction : transactions1) {
                    if (!event.isCancelled()) {
                        event.setCancelled(false);
                        Sorter.removeItem(transaction);
                        menu.updateInventory();
                        return;
                    }
                }
                event.setCancelled(true);
                return;

            case MOVE_TO_OTHER_INVENTORY:
                if (event.getClickedInventory().equals(player.getInventory())) {
                    Set<Transaction> transactions2 = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(event.getCurrentItem(), null, 0)));
                    for (Transaction transaction : transactions2) {
                        if (!event.isCancelled()) {
                            event.setCancelled(false);
                            Sorter.addItem(transaction);
                            menu.updateInventory();
                            return;
                        }
                    }
                }
                if (event.getClickedInventory().equals(menu.getInventory())) {
                    Set<Transaction> transactions3 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(event.getCurrentItem(), null, 0)));
                    for (Transaction transaction : transactions3) {
                        if (!event.isCancelled()) {
                            event.setCancelled(false);
                            Sorter.removeItem(transaction);
                            menu.updateInventory();
                            return;
                        }
                    }
                }
                event.setCancelled(true);
                return;

            case SWAP_WITH_CURSOR:
                Transaction addition = null;
                Transaction removal = null;

                Set<Transaction> transactions4 = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(event.getCursor(), null, 0)));
                for (Transaction transaction : transactions4) {
                    addition = transaction;
                }

                Set<Transaction> transactions5 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(event.getCurrentItem(), null, 0)));
                for (Transaction transaction : transactions5) {
                    removal = transaction;
                }

                if (!event.isCancelled() && addition != null && removal != null) {
                    event.setCancelled(false);
                    Sorter.removeItem(removal);
                    Sorter.addItem(addition);
                    menu.updateInventory();
                    return;
                }

                event.setCancelled(true);
                return;

            case HOTBAR_SWAP:
                event.setCancelled(true); // Not yet implemented

            case NOTHING, CLONE_STACK:
                return; // No need to do anything

            default:
                event.setCancelled(true); // For safety in case Mojang adds new actions that aren't handled
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (InventoryMenuManager.isInventoryMenu(event.getInventory())) {
            InventoryMenuManager.removeInventoryMenu((Player) event.getPlayer());
        }
    }
}
