package de.kwantux.networks.terminals.inventory;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public interface CustomInventoryHolder extends InventoryHolder {
    default void onInventoryDrag(InventoryDragEvent event) {}
    default void onInventoryClicked(InventoryClickEvent event) {}
    default void onInventoryClose(InventoryCloseEvent event) {}
}
