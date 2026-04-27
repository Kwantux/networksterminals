package de.kwantux.networks.terminals.inventory;

import de.kwantux.networks.Network;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class InventoryMenuManager {

    static private final ArrayList<InventoryMenu> list = new ArrayList<>();

    static public void addInventoryMenu(Player player, Network network, String filter) {
        list.add(new InventoryMenu(player, network, filter));
    }

    static public void removeInventoryMenu(Player player) {

        InventoryMenu menu = getMenuForPlayer(player);

        if (menu != null) {
            menu.close();
            list.remove(menu);
        }
    }

    static public InventoryMenu getMenuForPlayer(Player player) {
        for (InventoryMenu inventoryMenu : list) {
            if (inventoryMenu.player.equals(player)) {
                return inventoryMenu;
            }
        }
        return null;
    }

    static public boolean isInventoryMenu(Inventory inventory) {
        for (InventoryMenu menu : list) {
            if (menu.getInventory().equals(inventory)) return true;
        }
        return false;
    }
}
