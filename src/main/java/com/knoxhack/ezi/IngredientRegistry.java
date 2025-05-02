package com.knoxhack.ezi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Caches every creative-visible item for search filtering. */
public final class IngredientRegistry {
    private static final List<ItemStack> ALL_ITEMS = new ArrayList<>();

    public static void bootstrap() {
        ALL_ITEMS.clear();
        // Iterate the global item registry directly—no server lookup needed.
        for (Item item : BuiltInRegistries.ITEM) {
            ALL_ITEMS.add(new ItemStack(item));
        }
    }

    public static List<ItemStack> allItems() {
        return Collections.unmodifiableList(ALL_ITEMS);
    }
}