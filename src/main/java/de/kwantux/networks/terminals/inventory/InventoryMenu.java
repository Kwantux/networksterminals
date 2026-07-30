package de.kwantux.networks.terminals.inventory;

import de.kwantux.networks.Main;
import de.kwantux.networks.Network;
import de.kwantux.networks.Sorter;
import de.kwantux.networks.terminals.TerminalsPlugin;
import de.kwantux.networks.terminals.component.TerminalComponent;
import de.kwantux.networks.utils.PositionedItemStack;
import de.kwantux.networks.utils.Transaction;
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

import static de.kwantux.networks.terminals.util.Keys.NETWORKS_INDEX_CLICK;
import static de.kwantux.networks.terminals.util.Keys.NETWORKS_MENU_COMMAND;
import static de.kwantux.networks.terminals.util.ChestSortCompatibility.comparator;
import static de.kwantux.networks.utils.DevelopmentUtils.devlog;

public class InventoryMenu implements CustomInventoryHolder {
    private static final Map<UUID, InventoryMenu> activeMenus = new HashMap<>();

    Player player;
    Network network;
    private final Inventory inventory;
    private final List<NetworkItemStackDisplay> items = new ArrayList<>();
    private final List<List<ItemStack>> contents = new ArrayList<>();
    private int page;
    private final BossBar bossBar;
    private final TerminalComponent component;
    private final String filter;
    private boolean closed;

    public InventoryMenu(Player player, Network network, String filter) {

        this.player = player;
        this.network = network;
        this.filter = filter != null ? filter.toLowerCase() : null;

        component = new TerminalComponent(player);
        network.addComponent(component);

        inventory = Bukkit.createInventory(this, 54, Component.text("Content of network " + network.name()));

        bossBar = BossBar.bossBar(Component.text("Network: " + network.name()), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

        // A second /nts can arrive before the client sends InventoryCloseEvent.
        // Keep exactly one terminal (and therefore one BossBar) per player.
        InventoryMenu previous = activeMenus.put(player.getUniqueId(), this);
        if (previous != null) {
            previous.close();
        }
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

        // Generate the virtual terminal in the same category-aware order as
        // ChestSort Plus. Never let a generic chest sorter rewrite these GUI
        // slots directly: they are views backed by real network transactions.
        items.sort((left, right) -> {
            int itemOrder = comparator().compare(left.getItemStack(), right.getItemStack());
            if (itemOrder != 0) return itemOrder;
            return Long.compare(right.getAmount(), left.getAmount());
        });

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
            player.sendActionBar(Main.lang.getFinal("not_all_components_loaded"));
        }
        addControls();
        bossBar.progress(contents.size() <= 1 ? 1f : (float) page / (contents.size() - 1));
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

    private boolean donateToNetwork(ItemStack donation) {
        donation = stripTerminalDisplayMetadata(donation);
        for (Transaction transaction : Sorter.tryDonation(
                network,
                component,
                Set.of(new PositionedItemStack(donation, null, 0)))) {
            // Networks' own spaceFree check can report true when only part of
            // the stack fits. Sorter.addItem then discards Inventory.addItem's
            // leftovers, so verify full capacity before clearing the cursor.
            if (capacityFor(transaction.target().inventory(), donation) < donation.getAmount()) {
                continue;
            }
            if (Sorter.addItem(transaction)) {
                audit("deposit", donation, donation.getAmount());
                return true;
            }
        }
        return false;
    }

    private ItemStack stripTerminalDisplayMetadata(ItemStack stack) {
        ItemStack clean = stack.clone();
        ItemMeta meta = clean.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(NETWORKS_INDEX_CLICK)) {
            meta.getPersistentDataContainer().remove(NETWORKS_INDEX_CLICK);
            meta.itemName(null);
            meta.setMaxStackSize(null);
            clean.setItemMeta(meta);
        }
        return clean;
    }

    private int capacityFor(Inventory target, ItemStack stack) {
        long capacity = 0;
        for (ItemStack existing : target.getStorageContents()) {
            if (existing == null || existing.getType().isAir()) {
                capacity += stack.getMaxStackSize();
            } else if (existing.isSimilar(stack)) {
                capacity += Math.max(0, Math.min(existing.getMaxStackSize(), stack.getMaxStackSize())
                        - existing.getAmount());
            }
            if (capacity >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) capacity;
    }

    private void audit(String action, ItemStack stack, int amount) {
        TerminalsPlugin.instance.getLogger().info(
                "[transaction] player=" + player.getName()
                        + " network=" + network.name()
                        + " action=" + action
                        + " item=" + stack.getType().getKey()
                        + " amount=" + amount);
    }

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }

    private boolean withdrawToCursor(InventoryClickEvent event, ItemStack requested, int limit) {
        ItemStack cursor = event.getCursor();
        ItemStack deliverable = stripTerminalDisplayMetadata(requested);
        int capacity;

        if (isEmpty(cursor)) {
            capacity = deliverable.getMaxStackSize();
        } else if (cursor.isSimilar(deliverable)) {
            capacity = Math.max(0, cursor.getMaxStackSize() - cursor.getAmount());
        } else {
            return false;
        }

        int amount = Math.min(requested.getAmount(), Math.min(limit, capacity));
        if (amount <= 0) return false;

        ItemStack exactRequest = requested.clone();
        exactRequest.setAmount(amount);
        ItemStack supplied = requestFromNetwork(exactRequest);
        if (supplied == null) return false;
        supplied = stripTerminalDisplayMetadata(supplied);

        if (isEmpty(cursor)) {
            event.setCursor(supplied);
        } else {
            ItemStack updatedCursor = cursor.clone();
            updatedCursor.setAmount(cursor.getAmount() + supplied.getAmount());
            event.setCursor(updatedCursor);
        }

        audit("withdraw-cursor", supplied, supplied.getAmount());
        scheduleUpdate();
        return true;
    }

    private boolean withdrawToInventory(ItemStack requested) {
        ItemStack deliverable = stripTerminalDisplayMetadata(requested);
        int capacity = capacityFor(player.getInventory(), deliverable);
        int amount = Math.min(requested.getAmount(), capacity);
        if (amount <= 0) return false;

        ItemStack exactRequest = requested.clone();
        exactRequest.setAmount(amount);
        ItemStack supplied = requestFromNetwork(exactRequest);
        if (supplied == null) return false;
        supplied = stripTerminalDisplayMetadata(supplied);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(supplied);
        if (!leftovers.isEmpty()) {
            // Capacity was checked immediately before insertion, but another
            // plugin could still change the inventory. Never destroy a race
            // leftover: return it to the network or drop it at the player.
            for (ItemStack leftover : leftovers.values()) {
                if (!donateToNetwork(leftover)) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    audit("withdraw-overflow-drop", leftover, leftover.getAmount());
                }
            }
        }

        int leftoverAmount = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
        int delivered = supplied.getAmount() - leftoverAmount;
        if (delivered > 0) audit("withdraw-inventory", supplied, delivered);
        scheduleUpdate();
        return true;
    }

    private boolean withdrawAndDrop(ItemStack requested, int limit) {
        ItemStack exactRequest = requested.clone();
        exactRequest.setAmount(Math.min(requested.getAmount(), limit));
        ItemStack supplied = requestFromNetwork(exactRequest);
        if (supplied == null) return false;
        supplied = stripTerminalDisplayMetadata(supplied);
        player.getWorld().dropItemNaturally(player.getLocation(), supplied);
        audit("withdraw-drop", supplied, supplied.getAmount());
        scheduleUpdate();
        return true;
    }

    /**
     * The terminal presents one aggregate entry for matching stacks, whereas a
     * Networks supplier can only provide the contents of one physical slot per
     * transaction. Split the requested amount across matching supplier slots.
     */
    private @Nullable ItemStack requestFromNetwork(ItemStack requested) {
        int remaining = requested.getAmount();
        int supplied = 0;

        while (remaining > 0) {
            boolean removed = false;

            for (ItemStack available : network.items()) {
                if (available == null || !available.isSimilar(requested) || available.getAmount() <= 0) {
                    continue;
                }

                ItemStack portion = available.clone();
                portion.setAmount(Math.min(remaining, available.getAmount()));

                for (Transaction transaction : Sorter.tryRequest(
                        network,
                        component,
                        Set.of(new PositionedItemStack(portion, null, 0)))) {
                    if (Sorter.removeItem(transaction)) {
                        supplied += portion.getAmount();
                        remaining -= portion.getAmount();
                        removed = true;
                        break;
                    }
                }

                if (removed) {
                    break;
                }
            }

            if (!removed) {
                break;
            }
        }

        if (supplied == 0) {
            return null;
        }

        ItemStack result = requested.clone();
        result.setAmount(supplied);
        return result;
    }

    private boolean handleClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null) {
            // Handle menu controls
            if (this.inventory.equals(event.getClickedInventory())
                    && currentItem.getItemMeta() != null
                    && currentItem.getItemMeta().getPersistentDataContainer().has(NETWORKS_MENU_COMMAND, PersistentDataType.INTEGER)) {
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

        // ChestSort Plus activates on an empty slot. Refresh using the safe,
        // category-sorted backing list and cancel the generic sorter before it
        // can rearrange the terminal's virtual slots and navigation controls.
        if (inventory != null && inventory.equals(this.inventory)
                && currentItem == null
                && action == InventoryAction.NOTHING) {
            updateInventory();
            renderInventory();
            return true;
        }

        // Only shift-clicking a player item is a terminal action (deposit).
        // All other player-inventory actions must retain vanilla behaviour.
        if (inventory != null && inventory.equals(player.getInventory())) {
            // Vanilla double-click collection would also scan the terminal's
            // virtual top inventory and can copy/remove display stacks without
            // a Networks transaction. Disable only that cross-inventory action.
            if (action == InventoryAction.COLLECT_TO_CURSOR) return true;
            if (action != InventoryAction.MOVE_TO_OTHER_INVENTORY) return false;
        }

        devlog("[Terminals] Inventory Action: " + action);

        // Handle item actions
        switch (action) {
            case PLACE_ONE:
                if (inventory == null || !inventory.equals(this.inventory) || cursor == null) return true;
                ItemStack toTransmit = cursor.clone();
                toTransmit.setAmount(1);
                if (donateToNetwork(toTransmit)) {
                    if (cursor.getAmount() == 1) {
                        event.setCursor(null);
                    } else {
                        ItemStack remaining = cursor.clone();
                        remaining.setAmount(cursor.getAmount() - 1);
                        event.setCursor(remaining);
                    }
                    scheduleUpdate();
                    return true;
                }
                return true;

            case PLACE_ALL:
                if (inventory == null || !inventory.equals(this.inventory) || cursor == null) return true;
                if (donateToNetwork(cursor.clone())) {
                    event.setCursor(null);
                    scheduleUpdate();
                    return true;
                }
                return true;

            case PICKUP_ALL:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                withdrawToCursor(event, currentItem, currentItem.getMaxStackSize());
                return true;

            case COLLECT_TO_CURSOR:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                withdrawToCursor(event, currentItem, currentItem.getMaxStackSize());
                return true;

            case DROP_ALL_SLOT:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                withdrawAndDrop(currentItem, currentItem.getMaxStackSize());
                return true;

            case PICKUP_HALF:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                ItemStack toTransmit1 = currentItem.clone();
                toTransmit1.setAmount(Math.ceilDiv(currentItem.getAmount(), 2));
                withdrawToCursor(event, toTransmit1, toTransmit1.getAmount());
                return true;

            case PICKUP_ONE:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                withdrawToCursor(event, currentItem, 1);
                return true;

            case DROP_ONE_SLOT:
                if (inventory != null && inventory.equals(player.getInventory())) return false;
                assert currentItem != null;
                currentItem = getRealCurrentItem(currentItem);
                withdrawAndDrop(currentItem, 1);
                return true;

            case MOVE_TO_OTHER_INVENTORY:
                assert currentItem != null;
                assert inventory != null;
                if (inventory.equals(player.getInventory())) {
                    if (donateToNetwork(currentItem.clone())) {
                        event.setCurrentItem(null);
                        scheduleUpdate();
                        return true;
                    }
                    return true;
                }
                if (inventory.equals(this.getInventory())) {
                    currentItem = getRealCurrentItem(currentItem, event.isShiftClick() && event.isRightClick() ? 1 : null); // Behavior similar to AE2
                    withdrawToInventory(currentItem);
                }
                return true;

            case SWAP_WITH_CURSOR:
                if (inventory == null || !inventory.equals(this.inventory) || cursor == null) return false;
                if (donateToNetwork(cursor.clone())) {
                    event.setCursor(null);
                    scheduleUpdate();
                    return true;
                }
                return true;

            case HOTBAR_SWAP:
                // A number-key swap needs an atomic two-way transaction.
                // Networks does not provide one, so block it instead of risking
                // one side succeeding and the other side losing an item.
                return true;

            case CLONE_STACK:
                if (inventory != null && inventory.equals(this.getInventory())) {
                    assert currentItem != null;
                    currentItem = getRealCurrentItem(currentItem);
                    if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                        currentItem.setAmount(currentItem.getMaxStackSize());
                        event.setCursor(currentItem);
                    }
                }
                return true;

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

        // A drag entirely inside the player's own inventory is not a terminal
        // operation and must be left to vanilla untouched.
        if (totalAmount == 0) {
            return;
        }

        ItemStack oldCursor = event.getOldCursor();
        if (oldCursor == null || oldCursor.getAmount() < totalAmount) {
            event.setCancelled(true);
            return;
        }

        ItemStack donation = oldCursor.clone();
        donation.setAmount(totalAmount);

        if (donateToNetwork(donation)) {
            int remaining = oldCursor.getAmount() - totalAmount;
            if (remaining == 0) {
                event.setCursor(null);
            } else {
                ItemStack cursor = oldCursor.clone();
                cursor.setAmount(remaining);
                event.setCursor(cursor);
            }
            scheduleUpdate();
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    public void onInventoryClicked(InventoryClickEvent event) {
        if (!event.getView().getTopInventory().equals(inventory)) {
            return;
        }
        try {
            event.setCancelled(handleClick(event));
        } catch (Exception ignored) {
            // safety mechanism
            event.setCancelled(true);
        }
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        close();
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        bossBar.removeViewer(player);
        network.removeComponent(component);
        activeMenus.remove(player.getUniqueId(), this);
    }

    public static void closeAll() {
        for (InventoryMenu menu : List.copyOf(activeMenus.values())) {
            menu.close();
        }
    }
}
