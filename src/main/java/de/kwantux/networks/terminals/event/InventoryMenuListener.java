package de.kwantux.networks.terminals.event;

import de.kwantux.networks.Sorter;
import de.kwantux.networks.terminals.inventory.InventoryMenu;
import de.kwantux.networks.terminals.inventory.InventoryMenuManager;
import de.kwantux.networks.terminals.TerminalsPlugin;
import de.kwantux.networks.utils.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

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
                List<Transaction> transactions = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), List.of(event.getCursor()));
                if (!transactions.isEmpty()) {
                    if (transactions.getFirst().stack().equals(event.getCursor())) {
                        if (!event.isCancelled()) {
                            event.setCancelled(false);
                            Sorter.addItem(transactions.getFirst());
                            menu.updateInventory();
                        }
                        return;
                    }
                }
                event.setCancelled(true);
                return;

            case COLLECT_TO_CURSOR, PICKUP_ONE, PICKUP_HALF, PICKUP_SOME, PICKUP_ALL, DROP_ALL_CURSOR, DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT:
                if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) return;
                List<Transaction> transactions1 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), List.of(event.getCurrentItem()));
                if (!transactions1.isEmpty()) {
                    if (transactions1.getFirst().stack().equals(event.getCurrentItem())) {
                        if (!event.isCancelled()) {
                            event.setCancelled(false);
                            Sorter.removeItem(transactions1.getFirst());
                            menu.updateInventory();
                        }
                        return;
                    }
                }
                event.setCancelled(true);
                return;

            case SWAP_WITH_CURSOR, HOTBAR_SWAP:
                event.setCancelled(true);
                return;

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
