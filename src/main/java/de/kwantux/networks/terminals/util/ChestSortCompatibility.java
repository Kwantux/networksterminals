package de.kwantux.networks.terminals.util;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.util.Comparator;

/**
 * Reads ChestSort Plus' public comparator without making it a hard runtime
 * dependency of the terminal addon. This lets the terminal generate its own
 * safe virtual view in the same category order as ordinary chests.
 */
public final class ChestSortCompatibility {
    private static Comparator<ItemStack> comparator;

    private ChestSortCompatibility() {}

    public static Comparator<ItemStack> comparator() {
        if (comparator != null) {
            return comparator;
        }

        Plugin chestSort = Bukkit.getPluginManager().getPlugin("chestsort-plus");
        if (chestSort != null && chestSort.isEnabled()) {
            try {
                Class<?> comparatorClass = Class.forName(
                        "com.uravgcode.chestsortplus.comparator.ItemComparator",
                        true,
                        chestSort.getClass().getClassLoader());
                Constructor<?> constructor = comparatorClass.getConstructor();
                Object instance = constructor.newInstance();
                if (instance instanceof Comparator<?> rawComparator) {
                    @SuppressWarnings("unchecked")
                    Comparator<ItemStack> itemComparator = (Comparator<ItemStack>) rawComparator;
                    comparator = itemComparator;
                    return comparator;
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Fall through to a deterministic built-in order.
            }
        }

        comparator = Comparator
                .comparing((ItemStack item) -> item.getType().name())
                .thenComparing(item -> PlainTextComponentSerializer.plainText()
                        .serialize(item.displayName()), String.CASE_INSENSITIVE_ORDER);
        return comparator;
    }
}
