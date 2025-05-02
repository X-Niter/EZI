package com.knoxhack.ezi.client;

import com.knoxhack.ezi.RecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen to display crafting recipes for a specific ItemStack.
 * Shows one recipe at a time, with navigation buttons.
 */
public class RecipeViewScreen extends Screen {
    private static final int GRID_SIZE    = 3;
    private static final int CELL_SIZE    = 18;
    private static final int PADDING      = 10;
    private static final int BUTTON_SIZE  = 20;

    private final Screen parent;
    private final ItemStack target;
    private final RecipeType<?> recipeType;
    private final List<Recipe<?>> recipes;
    private int index = 0;
    private int left, top;

    public RecipeViewScreen(Screen parent, ItemStack stack, RecipeType<?> type) {
        super(Component.literal(
                (type == RecipeType.SMELTING ? "Smelting " : "Crafting ")
                        + "recipes for " + stack.getHoverName().getString()
        ));
        this.parent     = parent;
        this.target     = stack;
        this.recipeType = type;
        this.recipes    = new ArrayList<>(RecipeRegistry.recipesFor(type, stack.getItem()));
    }

    @Override
    protected void init() {
        super.init();
        int totalWidth  = GRID_SIZE * CELL_SIZE + PADDING * 3 + CELL_SIZE + BUTTON_SIZE;
        int totalHeight = GRID_SIZE * CELL_SIZE + PADDING * 3 + BUTTON_SIZE;
        this.left = (this.width  - totalWidth)  / 2;
        this.top  = (this.height - totalHeight) / 2;

        // Prev button
        addRenderableWidget(Button.builder(Component.literal("<"), b -> prevRecipe())
                .bounds(left,
                        top + GRID_SIZE * CELL_SIZE + PADDING,
                        BUTTON_SIZE,
                        BUTTON_SIZE)
                .build());

        // Next button
        addRenderableWidget(Button.builder(Component.literal(">"), b -> nextRecipe())
                .bounds(left + GRID_SIZE * CELL_SIZE + PADDING + CELL_SIZE - BUTTON_SIZE,
                        top + GRID_SIZE * CELL_SIZE + PADDING,
                        BUTTON_SIZE,
                        BUTTON_SIZE)
                .build());
    }

    private void prevRecipe() {
        if (!recipes.isEmpty()) {
            index = (index - 1 + recipes.size()) % recipes.size();
        }
    }

    private void nextRecipe() {
        if (!recipes.isEmpty()) {
            index = (index + 1) % recipes.size();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui, mouseX, mouseY, partialTicks);
        super.render(gui, mouseX, mouseY, partialTicks);

        if (recipes.isEmpty()) {
            gui.drawCenteredString(font, "No recipes found", width / 2, height / 2, 0xFF5555);
            return;
        }

        Recipe<?> recipe = recipes.get(index);

        // Build a list of input Ingredients depending on type
        List<Ingredient> ingredients = new ArrayList<>();
        if (recipeType == RecipeType.CRAFTING && recipe instanceof CraftingRecipe crafting) {
            ingredients = crafting.getIngredients();
        } else if (recipeType == RecipeType.SMELTING && recipe instanceof AbstractCookingRecipe<?, ?> cooking) {
            ingredients = List.of(cooking.getIngredient());
        }

        // Draw the input grid
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            int cx = left + (i % GRID_SIZE) * CELL_SIZE;
            int cy = top  + (i / GRID_SIZE) * CELL_SIZE;
            if (i < ingredients.size()) {
                for (ItemStack stack : ingredients.get(i).getItems()) {
                    gui.renderItem(stack, cx, cy);
                    gui.renderItemDecorations(font, stack, cx, cy);
                    break;
                }
            }
        }

        // Draw the output via assemble()
        ItemStack result = recipe.assemble(
                new SimpleContainer(1),
                Minecraft.getInstance().level.registryAccess()
        );
        int rx = left + GRID_SIZE * CELL_SIZE + PADDING;
        int ry = top + CELL_SIZE;
        gui.renderItem(result, rx, ry);
        gui.drawString(font, result.getHoverName(), rx, ry + CELL_SIZE + 2, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}