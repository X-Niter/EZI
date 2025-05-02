package com.knoxhack.ezi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Caches crafting‐ and smelting‐type recipes by their output Item.
 */
public final class RecipeRegistry {
    // only these two for now; add SMOKING, BLASTING, etc if you like
    private static final List<RecipeType<?>> SUPPORTED = List.of(
            RecipeType.CRAFTING,
            RecipeType.SMELTING
    );

    private static final Map<RecipeType<?>, Map<Item, List<Recipe<?>>>> BY_TYPE =
            new EnumMap<>(RecipeType.class);

    /**
     * A tiny one‐slot container that also implements RecipeInput,
     * so we can call assemble(...) without generic‐bound complaints.
     */
    private static class RecipeInputContainer extends SimpleContainer implements RecipeInput {
        public RecipeInputContainer(int capacity) {
            super(capacity);
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /** Call on mod‐load and whenever datapacks/reload happen. */
    public static void bootstrap() {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;

        RecipeManager mgr    = world.getServer().getRecipeManager();
        RegistryAccess access = world.registryAccess();

        BY_TYPE.clear();

        for (RecipeType<?> type : SUPPORTED) {
            // getAllRecipesFor returns List<T extends Recipe<?>>
            List<? extends Recipe<?>> all = mgr.getAllRecipesFor(type);

            Map<Item, List<Recipe<?>>> map = all.stream()
                    .map(r -> {
                        // preview the output via assemble, using our RecipeInputContainer
                        ItemStack out = r.assemble(new RecipeInputContainer(1), access);
                        return out.isEmpty() ? null : Map.entry(r, out);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(
                            entry -> entry.getValue().getItem(),
                            () -> new EnumMap<>(Item.class),
                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                    ));

            BY_TYPE.put(type, map);
        }
    }

    /**
     * @param type  CRAFTING vs SMELTING
     * @param item  the output Item
     * @return      all matching recipes, never null
     */
    public static Collection<Recipe<?>> recipesFor(RecipeType<?> type, Item item) {
        return BY_TYPE
                .getOrDefault(type, Collections.emptyMap())
                .getOrDefault(item, Collections.emptyList());
    }
}