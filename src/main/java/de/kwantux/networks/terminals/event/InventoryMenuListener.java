package de.kwantux.networks.terminals.event;

import de.kwantux.networks.Sorter;
import de.kwantux.networks.terminals.inventory.InventoryMenu;
import de.kwantux.networks.terminals.inventory.InventoryMenuManager;
import de.kwantux.networks.terminals.TerminalsPlugin;
import de.kwantux.networks.utils.PositionedItemStack;
import de.kwantux.networks.utils.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

import static de.kwantux.networks.terminals.util.Keys.NETWORKS_MENU_ICON;

public class InventoryMenuListener implements Listener {

    public InventoryMenuListener(TerminalsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void scheduleUpdate(InventoryMenu menu) {
        menu.updateInventory();
        Bukkit.getScheduler().runTaskLater(TerminalsPlugin.instance, menu::renderInventory, 1);
    }
    
    private boolean handleClick(Player player, Inventory inventory, InventoryMenu menu, InventoryAction action, ItemStack currentItem, ItemStack cursor) {

        if (currentItem != null) {
            // Handle menu controls
            if (currentItem.getItemMeta() != null && currentItem.getItemMeta().getPersistentDataContainer().has(NETWORKS_MENU_ICON, PersistentDataType.INTEGER)) {
                switch (currentItem.getItemMeta().getPersistentDataContainer().get(NETWORKS_MENU_ICON, PersistentDataType.INTEGER)) {
                    case 1:
                        menu.toFirstPage();
                        return true;

                    case 2:
                        menu.decrementPage();
                        return true;

                    case 3:
                        menu.incrementPage();
                        return true;

                    case 4:
                        menu.toLastPage();
                        return true;
                    case null, default:
                        return true;
                }
            }
        }

        // Handle item actions
        switch (action) {
            case PLACE_ONE, PLACE_SOME,PLACE_ALL:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert cursor != null;
                Set<Transaction> transactions = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(cursor, null, 0)));
                for (Transaction transaction : transactions) {
                    Sorter.addItem(transaction);
                    scheduleUpdate(menu);
                    return false;
                }
                return true;

            case COLLECT_TO_CURSOR, PICKUP_ONE, PICKUP_ALL, DROP_ALL_CURSOR, DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
//                ItemStack transferred;
//                if (action == InventoryAction.PICKUP_HALF) {
//                    System.out.println("Pickup half");
//                    transferred = currentItem.clone();
//                    System.out.println("clone success");
//                    transferred.setAmount(Math.ceilDiv(currentItem.getAmount(), 2));
//                } else {
//                    transferred = currentItem;
//                }
                Set<Transaction> transactions1 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                for (Transaction transaction : transactions1) {
                    Sorter.removeItem(transaction);
                    scheduleUpdate(menu);
                    return false;
                }
                return true;

            case MOVE_TO_OTHER_INVENTORY:
                assert currentItem != null;
                if (inventory.equals(player.getInventory())) {
                    Set<Transaction> transactions2 = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                    for (Transaction transaction : transactions2) {
                        Sorter.addItem(transaction);
                        scheduleUpdate(menu);
                        return false;
                    }
                }
                if (inventory.equals(menu.getInventory())) {
                    Set<Transaction> transactions3 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                    for (Transaction transaction : transactions3) {
                        Sorter.removeItem(transaction);
                        scheduleUpdate(menu);
                        return false;
                    }
                }
                return true;

            case SWAP_WITH_CURSOR:
                Transaction addition = null;
                Transaction removal = null;

                Set<Transaction> transactions4 = Sorter.tryDonation(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(cursor, null, 0)));
                for (Transaction transaction : transactions4) {
                    addition = transaction;
                }

                Set<Transaction> transactions5 = Sorter.tryRequest(menu.getNetwork(), menu.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                for (Transaction transaction : transactions5) {
                    removal = transaction;
                }

                if (addition != null && removal != null) {
                    Sorter.removeItem(removal);
                    Sorter.addItem(addition);
                    scheduleUpdate(menu);
                    return false;
                }

                return true;

            case HOTBAR_SWAP:
                return true; // Not yet implemented

            case NOTHING, CLONE_STACK:
                return false; // No need to do anything

            default:
                return true; // For safety in case Mojang adds new actions that aren't handled
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        InventoryMenu menu = InventoryMenuManager.getMenuForPlayer(player);
        if (menu == null) return;
        if (event.getInventory() != menu.getInventory()) return;
        event.setCancelled(true);

        // Too buggy, maybe fix later
//        ItemStack oldCursor = event.getOldCursor();
//        ItemStack newCursor = event.getCursor();
//        int newCursorAmount = newCursor == null ? 0 : newCursor.getAmount();
//        ItemStack difference = new ItemStack(oldCursor);
//        difference.setAmount(oldCursor.getAmount() - newCursorAmount);
//        handleClick(player, event.getInventory(), menu, InventoryAction.PLACE_SOME, null, difference);
//        scheduleUpdate(menu);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClicked(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        InventoryMenu menu = InventoryMenuManager.getMenuForPlayer(player);
        if (menu == null) return;
        try {
            event.setCancelled(handleClick(player, event.getClickedInventory(), menu, event.getAction(), event.getCurrentItem(), event.getCursor()));
        } catch (Exception ignored) {
            // safety mechanism
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (InventoryMenuManager.isInventoryMenu(event.getInventory())) {
            InventoryMenuManager.removeInventoryMenu((Player) event.getPlayer());
        }
    }
}
