package de.kwantux.networks.terminals.inventory;

import de.kwantux.networks.terminals.util.NumberFormatter;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.kwantux.networks.terminals.util.Keys.NETWORKS_INDEX_CLICK;

public class NetworkItemStackDisplay {
    private final ItemStack itemStack;
    private int amount;

    public NetworkItemStackDisplay(ItemStack is) {
        Objects.requireNonNull(is);
        this.amount = is.getAmount();
        this.itemStack = is.clone();
        this.itemStack.setAmount(1);
    }

    public ItemStack display(int indexClick) {
        ItemStack display = itemStack.clone();

        display.editMeta(meta -> {
            meta.getPersistentDataContainer().set(NETWORKS_INDEX_CLICK, PersistentDataType.INTEGER, indexClick);
            meta.setMaxStackSize(99);
            meta.customName(null);
            meta.itemName(NumberFormatter
                    .formatCompact(amount)
                    .style(Style
                            .style()
                            .color(NamedTextColor.WHITE)
                            .build())
                    .append(Component.text("x "))
                    .append(itemStack.displayName())
            );

            if (Math.abs(amount) >= 10_000) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(NumberFormatter.formatFull(amount)
                        .color(NamedTextColor.GRAY)
                        .append(Component.text("x")));
                meta.lore(lore);
            }
        });
        display.setAmount(Math.min(amount, 99));

        return display;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void increaseAmount(int amount) {
        this.setAmount((int) Math.min(Integer.MAX_VALUE, (long) this.amount + amount));
    }

    public void decreaseAmount(int amount) {
        this.setAmount(Math.max(0, this.amount - amount));
    }

    public ItemStack getOriginalStack() {
        return getOriginalStack(null);
    }

    @SuppressWarnings("UnstableApiUsage")
    public ItemStack getOriginalStack(@Nullable Integer amount) {
        ItemStack is = itemStack.clone();
        if (amount == null)
            amount = itemStack.getDataOrDefault(DataComponentTypes.MAX_STACK_SIZE, itemStack.getType().getMaxStackSize());
        is.setAmount(Math.min(this.amount, amount));
        return is;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NetworkItemStackDisplay that = (NetworkItemStackDisplay) o;
        return itemStack.isSimilar(that.itemStack);
    }

    @Override
    public int hashCode() {
        return itemStack.hashCode();
    }

    @Override
    public String toString() {
        return itemStack.toString();
    }
}
