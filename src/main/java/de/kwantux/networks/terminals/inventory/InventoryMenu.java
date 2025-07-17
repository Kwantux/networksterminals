package de.kwantux.networks.terminals.inventory;

import de.kwantux.networks.Network;
import de.kwantux.networks.terminals.component.TerminalComponent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static de.kwantux.networks.terminals.util.Keys.NETWORKS_MENU_ICON;

public class InventoryMenu {

    Player player;
    Network network;
    private final Inventory inventory;
    private final ArrayList<ArrayList<ItemStack>> contents = new ArrayList<>();
    private int page;
    private BossBar bossBar;
    private final TerminalComponent component;

    public InventoryMenu(Player player, Network network) {

        this.player = player;
        this.network = network;

        component = new TerminalComponent(player);
        network.addComponent(component);

        inventory = Bukkit.createInventory(player, 54, Component.text("Content of network " + network.name()));
        inventory.setMaxStackSize(127);

        bossBar = BossBar.bossBar(Component.text("Network: " + network.name()), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        bossBar.addViewer(player);

        updateInventory();
        renderInventory();
        player.openInventory(inventory);
    }

    public void close() {
        bossBar.removeViewer(player);
        network.removeComponent(component);
    }

    public Inventory getInventory() {return inventory;}

    public Network getNetwork() {return network;}

    public TerminalComponent getComponent() {return component;}

    private void addControls() {
        ItemStack first = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta itemMeta = first.getItemMeta();
        itemMeta.displayName(Component.text("<<--"));
        itemMeta.getPersistentDataContainer().set(NETWORKS_MENU_ICON, PersistentDataType.INTEGER, 1);
        first.setItemMeta(itemMeta);

        ItemStack back = new ItemStack(Material.ARROW);
        itemMeta = back.getItemMeta();
        itemMeta.displayName(Component.text("<-"));
        itemMeta.getPersistentDataContainer().set(NETWORKS_MENU_ICON, PersistentDataType.INTEGER, 2);
        back.setItemMeta(itemMeta);

        ItemStack forward = new ItemStack(Material.ARROW);
        itemMeta = forward.getItemMeta();
        itemMeta.displayName(Component.text("->"));
        itemMeta.getPersistentDataContainer().set(NETWORKS_MENU_ICON, PersistentDataType.INTEGER, 3);
        forward.setItemMeta(itemMeta);

        ItemStack last = new ItemStack(Material.SPECTRAL_ARROW);
        itemMeta = last.getItemMeta();
        itemMeta.displayName(Component.text("-->>"));
        itemMeta.getPersistentDataContainer().set(NETWORKS_MENU_ICON, PersistentDataType.INTEGER, 4);
        last.setItemMeta(itemMeta);

        inventory.setItem(45, first);
        inventory.setItem(46, back);
        inventory.setItem(52, forward);
        inventory.setItem(53, last);
    }

    public void updateInventory() {
        contents.clear();
        ArrayList<ItemStack> currentPage = new ArrayList<>();
        ArrayList<ItemStack> items = network.items();

        for (ItemStack item : items) {

            if (item != null) {
                currentPage.add(item);
            }

            if (currentPage.size() == 45) {
                contents.add(currentPage);
                currentPage = new ArrayList<>();
            }
        }

        contents.add(currentPage);
    }

    public void renderInventory() {
        inventory.setContents(contents.get(page).toArray(new ItemStack[54]));
        addControls();
        bossBar.progress((float) (page+1) / contents.size());
    }

    public void toFirstPage() {
        page = 0;
        renderInventory();
    }

    public void incrementPage() {
        if (page < contents.size()-1) page++;
        renderInventory();
    }

    public void decrementPage() {
        if (page > 0) page--;
        renderInventory();
    }

    public void toLastPage() {
        page = contents.size()-1;
        renderInventory();
    }

}
