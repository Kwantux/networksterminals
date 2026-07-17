package de.kwantux.networks.terminals.util;

import net.kyori.adventure.text.Component;
import java.text.NumberFormat;

public class NumberFormatter {
    public static Component formatFull(int value) {
        NumberFormat formatter = NumberFormat.getInstance();
        return Component.text(formatter.format(value));
    }

    public static Component formatCompact(int value) {
        if (Math.abs(value) < 10_000) {
            return formatFull(value);
        }

        String[] suffixes = new String[]{"", "k", "M", "G"};
        double absValue = Math.abs((double) value);

        int exp = (int) (Math.log10(absValue) / 3);
        exp = Math.min(exp, suffixes.length - 1);

        double scaled = value / Math.pow(10, exp * 3);

        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(0);

        String formattedNumber = formatter.format(scaled);
        return Component.text(formattedNumber + suffixes[exp]);
    }
}