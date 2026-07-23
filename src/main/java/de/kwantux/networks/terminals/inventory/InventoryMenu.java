package de.kwantux.networks.terminals.inventory;

import de.kwantux.networks.Main;
import de.kwantux.networks.Network;
import de.kwantux.networks.Sorter;
import de.kwantux.networks.terminals.TerminalsPlugin;
import de.kwantux.networks.terminals.component.TerminalComponent;
import de.kwantux.networks.utils.PositionedItemStack;
import de.kwantux.networks.utils.Transaction;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static de.kwantux.networks.component.module.BaseModule.spaceFree;
import static de.kwantux.networks.terminals.util.Keys.NETWORKS_INDEX_CLICK;
import static de.kwantux.networks.terminals.util.Keys.NETWORKS_MENU_COMMAND;
import static de.kwantux.networks.utils.DevelopmentUtils.devlog;

public class InventoryMenu implements CustomInventoryHolder {
    Player player;
    Network network;
    private final Inventory inventory;
    private final List<NetworkItemStackDisplay> items = new ArrayList<>();
    private final List<List<ItemStack>> contents = new ArrayList<>();
    private int page;
    private final BossBar bossBar;
    private final TerminalComponent component;
    private final String filter;
    private ScheduledTask actionBarTask = null;

    public InventoryMenu(Player player, Network network, String filter) {

        this.player = player;
        this.network = network;
        this.filter = filter != null ? filter.toLowerCase() : null;

        component = new TerminalComponent(player);
        network.addComponent(component);

        inventory = Bukkit.createInventory(this, 54, Component.text("Content of network " + network.name()));

        bossBar = BossBar.bossBar(Component.text("Network: " + network.name()), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        bossBar.addViewer(player);

        updateInventory();
        renderInventory();
        player.openInventory(inventory);

//        devlog("[Terminals] Matched filter: " + player.getClientOption(ClientOption.LOCALE));
    }

    public @NonNull Inventory getInventory() {return inventory;}

    public Network getNetwork() {return network;}

    public TerminalComponent getComponent() {return component;}

    private ItemStack makeControlButton(Material mat, String name, int command) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.itemName(Component.text(name));
            meta.getPersistentDataContainer().set(NETWORKS_MENU_COMMAND, PersistentDataType.INTEGER, command);
        });
        return item;
    }

    private void addControls() {
        inventory.setItem(45, makeControlButton(Material.SPECTRAL_ARROW, "<<--", 1));
        inventory.setItem(46, makeControlButton(Material.ARROW, "<-", 2));
        inventory.setItem(52, makeControlButton(Material.ARROW, "->", 3));
        inventory.setItem(53, makeControlButton(Material.SPECTRAL_ARROW, "-->>", 4));
    }

    public void updateInventory() {
        contents.clear();
        items.clear();
        List<ItemStack> currentPage = new ArrayList<>();

        network.items().forEach(i -> {
            for (NetworkItemStackDisplay ni : items)
                if (ni.getItemStack().isSimilar(i)) {
                    ni.increaseAmount(i.getAmount());
                    return;
                }
            items.add(new NetworkItemStackDisplay(i));
        });

        items.sort(Comparator.comparing(ni -> ni.getItemStack().getType()));
        items.sort(Comparator.comparing(NetworkItemStackDisplay::getAmount).reversed());

        for (int i = 0; i < items.size(); i++) {
            NetworkItemStackDisplay item = items.get(i);
            if (item != null && matchesFilter(item)) {
                currentPage.add(item.display(i));
            }

            if (currentPage.size() == 45) {
                contents.add(currentPage);
                currentPage = new ArrayList<>();
            }
        }

        contents.add(currentPage);
    }

    private boolean matchesFilter(NetworkItemStackDisplay ni) {
        if (filter == null) return true;
        ItemStack item = ni.getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasCustomName())
                if (PlainTextComponentSerializer.plainText()
                        .serialize(Objects.requireNonNull(meta.customName()))
                        .toLowerCase()
                        .contains(filter))
                    return true;
            if (PlainTextComponentSerializer.plainText()
                    .serialize(meta.itemName())
                    .toLowerCase()
                    .contains(filter))
                return true;
        }
        String materialName = item.getType().name().replace('_', ' ').toLowerCase();
        return materialName.contains(filter);
    }

    public void renderInventory() {
        inventory.setContents(contents.get(page).toArray(new ItemStack[54]));
        if (!network.allComponentsReady()) {
            Consumer<ScheduledTask> task = (scheduledTask) ->
                player.sendActionBar(Main.lang.getFinal("not_all_components_loaded"));
            actionBarTask = Main.regionScheduler.runAtFixedRate(TerminalsPlugin.instance, player.getLocation(), task, 1, 20);
        }
        addControls();
        bossBar.progress((float) page / (contents.size()-1));
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


    private void scheduleUpdate() {
        this.updateInventory();
        Main.regionScheduler.execute(TerminalsPlugin.instance, this.player.getLocation(), this::renderInventory);
    }

    private ItemStack getRealCurrentItem(ItemStack currentItem) {
        return getRealCurrentItem(currentItem, null);
    }

    private ItemStack getRealCurrentItem(ItemStack currentItem, @Nullable Integer amount) {
        ItemMeta meta = currentItem.getItemMeta();

        if (meta == null || !meta.getPersistentDataContainer().has(NETWORKS_INDEX_CLICK))
            return currentItem;

        Integer index = currentItem.getItemMeta().getPersistentDataContainer().get(NETWORKS_INDEX_CLICK, PersistentDataType.INTEGER);

        if (index == null)
            return currentItem;

        return items.get(index).getOriginalStack(amount);
    }

    private boolean handleClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null) {
            // Handle menu controls
            if (currentItem.getItemMeta() != null && currentItem.getItemMeta().getPersistentDataContainer().has(NETWORKS_MENU_COMMAND, PersistentDataType.INTEGER)) {
                switch (currentItem.getItemMeta().getPersistentDataContainer().get(NETWORKS_MENU_COMMAND, PersistentDataType.INTEGER)) {
                    case 1 ->
                            this.toFirstPage();
                    case 2 ->
                            this.decrementPage();
                    case 3 ->
                            this.incrementPage();
                    case 4 ->
                            this.toLastPage();
                    case null, default ->
                            throw new IllegalStateException("Unexpected value: " + currentItem.getItemMeta().getPersistentDataContainer().get(NETWORKS_MENU_COMMAND, PersistentDataType.INTEGER));
                }
                return true;
            }
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        InventoryAction action = event.getAction();
        ItemStack cursor = event.getCursor();

        devlog("[Terminals] Inventory Action: " + action);

        // Handle item actions
        Collection<Transaction> transactions;
        switch (action) {
            case PLACE_ONE:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                ItemStack toTransmit = cursor.clone();
                toTransmit.setAmount(1);
                transactions = Sorter.tryDonation(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(toTransmit, null, 0)));
                for (Transaction transaction : transactions) {
                    Sorter.addItem(transaction);
                    scheduleUpdate();
                    return false;
                }
                return true;

            case PLACE_ALL:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                transactions = Sorter.tryDonation(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(cursor, null, 0)));
                for (Transaction transaction : transactions) {
                    Sorter.addItem(transaction);
                    scheduleUpdate();
                    return false;
                }
                return true;

            case PICKUP_ALL, DROP_ALL_SLOT:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                transactions = Sorter.tryRequest(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                for (Transaction transaction : transactions) {
                    currentItem.setAmount(transaction.stack().getAmount());
                    event.setCurrentItem(currentItem);
                    Sorter.removeItem(transaction);
                    scheduleUpdate();
                    return false;
                }
                return true;

            case PICKUP_HALF: // Redefined as picking up one item
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                ItemStack toTransmit1 = currentItem.clone();
                toTransmit1.setAmount(1);
                transactions = Sorter.tryRequest(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(toTransmit1, null, 0)));
                for (Transaction transaction : transactions) {
                    currentItem.setAmount(transaction.stack().getAmount());
                    event.setCurrentItem(currentItem);
                    Sorter.removeItem(transaction);
                    scheduleUpdate();
                    return false;
                }
                return true;

            case PICKUP_ONE, DROP_ONE_SLOT:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                ItemStack toTransmit2 = currentItem.clone();
                toTransmit2.setAmount(1);
                transactions = Sorter.tryRequest(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(toTransmit2, null, 0)));
                for (Transaction transaction : transactions) {
                    currentItem.setAmount(transaction.stack().getAmount());
                    event.setCurrentItem(currentItem);
                    Sorter.removeItem(transaction);
                    scheduleUpdate();
                    return false;
                }
                return true;

            case MOVE_TO_OTHER_INVENTORY:
                assert currentItem != null;
                assert inventory != null;
                if (inventory.equals(player.getInventory())) {
                    if (!spaceFree(this.getInventory(), currentItem)) return true;
                    transactions = Sorter.tryDonation(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                    for (Transaction transaction : transactions) {
                        Sorter.addItem(transaction);
                        scheduleUpdate();
                        return false;
                    }
                }
                if (inventory.equals(this.getInventory())) {
                    currentItem = getRealCurrentItem(currentItem, event.isShiftClick() && event.isRightClick() ? 1 : null); // Behavior similar to AE2
                    if (!spaceFree(player.getInventory(), currentItem)) return true;
                    transactions = Sorter.tryRequest(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                    for (Transaction transaction : transactions) {
                        currentItem.setAmount(transaction.stack().getAmount());
                        event.setCurrentItem(currentItem);
                        Sorter.removeItem(transaction);
                        scheduleUpdate();
                        return false;
                    }
                }
                return true;

            case COLLECT_TO_CURSOR, SWAP_WITH_CURSOR:
                return !(inventory != null && inventory.equals(player.getInventory()));

//            case SWAP_WITH_CURSOR:
//                assert currentItem != null;
//
//                transactions = Sorter.tryDonation(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(cursor, null, 0)));
//                for (Transaction transaction : transactions) {
//                    event.setCurrentItem(null);
//                    Sorter.addItem(transaction);
//                    counter += transaction.stack().getAmount();
//                    devlog("INV: " + counter + "(+" + transaction.stack().getAmount() + ")");
//                    scheduleUpdate();
//                    return false;
//                }
//
//                return true;

            case HOTBAR_SWAP:
                assert inventory != null;
                if (!inventory.equals(this.getInventory()))
                    return false;

                boolean isSuccessful = false;

                if (currentItem != null) {
                    currentItem = getRealCurrentItem(currentItem);

                    transactions = Sorter.tryRequest(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(currentItem, null, 0)));
                    for (Transaction transaction : transactions) {
                        currentItem.setAmount(transaction.stack().getAmount());
                        event.setCurrentItem(currentItem);
                        Sorter.removeItem(transaction);
                        isSuccessful = true;
                    }
                }

                ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());

                if (hotbar != null) {
                    transactions = Sorter.tryDonation(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(hotbar, null, 0)));
                    for (Transaction transaction : transactions) {
                        Sorter.addItem(transaction);
                        isSuccessful = true;
                    }
                }

                if (isSuccessful) {
                    scheduleUpdate();
                    return false;
                }

                return true;

            case CLONE_STACK:
                if (inventory != null && inventory.equals(this.getInventory())) {
                    assert currentItem != null;
                    currentItem = getRealCurrentItem(currentItem);
                    event.setCurrentItem(currentItem);
                    Bukkit.getScheduler().runTaskLater(TerminalsPlugin.instance, this::renderInventory, 0);
                }
                return false;

            case NOTHING, DROP_ALL_CURSOR, DROP_ONE_CURSOR:
                return false; // No need to do anything

            default:
                return true; // For safety in case Mojang adds new actions that aren't handled
        }
    }

    public void onInventoryDrag(InventoryDragEvent event) {
        int totalAmount = event.getNewItems()
                .entrySet()
                .stream()
                .filter(entry -> Objects.equals(event.getView().getInventory(entry.getKey()), this.getInventory()))
                .map(entry -> entry.getValue().getAmount())
                .reduce(0, Integer::sum);

        ItemStack donation = event.getOldCursor();
        donation.setAmount(totalAmount);

        Set<Transaction> transactions = Sorter.tryDonation(this.getNetwork(), this.getComponent(), Set.of(new PositionedItemStack(donation, null, 0)));
        for (Transaction transaction : transactions) {
            Sorter.addItem(transaction);
            scheduleUpdate();
            return;
        }

        event.setCancelled(true);
    }

    public void onInventoryClicked(InventoryClickEvent event) {
        try {
            event.setCancelled(handleClick(event));
        } catch (Exception ignored) {
            // safety mechanism
            event.setCancelled(true);
        }
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        bossBar.removeViewer(player);
        network.removeComponent(component);
        if (actionBarTask != null) {
            player.sendActionBar(Component.empty());
            actionBarTask.cancel();
        }
    }
}
