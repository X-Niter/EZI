package com.knoxhack.ezi.client.widgets;

import com.knoxhack.ezi.client.RecipeViewScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A scrollable grid widget for displaying ItemStacks.
 * Supports live filtering and click-to-open recipe screen.
 */
public class ItemListWidget implements Renderable, GuiEventListener, NarratableEntry {
    // for deferred tooltip rendering
    private ItemStack hoveredStack;
    private int hoveredX, hoveredY;

    private int offsetX, offsetY;
    private final int width, height, pad;
    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> filtered = new ArrayList<>();
    private final EditBox searchBox;
    private int currentPage = 0;

    private final Button prevButton;
    private final Button nextButton;

    public ItemListWidget(int width, int height, int pad) {
        this.width = width;
        this.height = height;
        this.pad = pad;
        Minecraft mc = Minecraft.getInstance();

        // load + sort items alphabetically
        BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(s -> !s.isEmpty())
                .sorted(Comparator.comparing(s -> s.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER))
                .forEach(allItems::add);
        filtered.addAll(allItems);

        // search box at bottom
        this.searchBox = new EditBox(mc.font, 0, 0, width - pad * 2, 18, Component.literal("Search…"));
        this.searchBox.setResponder(this::onSearchChanged);

        // pagination arrows
        this.prevButton = Button.builder(Component.literal("<"), btn -> prevPage())
                .bounds(0, 0, 20, 20).build();
        this.nextButton = Button.builder(Component.literal(">"), btn -> nextPage())
                .bounds(0, 0, 20, 20).build();
    }

    /**
     * Reposition children when panel moves or resizes.
     */
    public void setPosition(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        // search box bottom-left
        searchBox.setX(x + pad);
        searchBox.setY(y + height - pad - searchBox.getHeight());
        // arrows top-left / top-right
        prevButton.setPosition(x + pad, y + pad);
        nextButton.setPosition(x + width - pad - nextButton.getWidth(), y + pad);
    }

    private void onSearchChanged(String text) {
        filtered.clear();
        String lc = text.toLowerCase(Locale.ROOT);
        for (ItemStack s : allItems) {
            if (s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(lc)) {
                filtered.add(s);
            }
        }
        currentPage = 0;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float pt) {
        // draw icons clipped
        drawItemGrid(gui, mouseX, mouseY);
        // draw overlays
        searchBox.render(gui, mouseX, mouseY, pt);
        prevButton.render(gui, mouseX, mouseY, pt);
        nextButton.render(gui, mouseX, mouseY, pt);
        // page indicator
        int fy = offsetY + pad + (prevButton.getHeight() - Minecraft.getInstance().font.lineHeight) / 2;
        String num = (currentPage + 1) + "/" + getTotalPages();
        int tw = Minecraft.getInstance().font.width(num);
        gui.drawString(Minecraft.getInstance().font, num, offsetX + (width - tw) / 2, fy, 0xFFFFFF);
        // finally tooltip un-clipped
        if (hoveredStack != null) {
            gui.renderTooltip(Minecraft.getInstance().font, hoveredStack.getHoverName(), hoveredX, hoveredY);
        }
    }

    private void drawItemGrid(GuiGraphics gui, int mx, int my) {
        hoveredStack = null;
        int cell = 20;
        int cols = Math.max(1, width / cell);
        // reserve header (arrows + 2px gap) and footer (search + pad)
        int headerH = pad + prevButton.getHeight() + 2;
        int footerH = pad + searchBox.getHeight() + pad;
        int availableH = height - headerH - footerH;
        int rows = availableH / cell;
        int yStart = offsetY + headerH;
        int perPage = cols * rows;
        int start = currentPage * perPage;
        int end = Math.min(filtered.size(), start + perPage);

        // raw-pixel scissor region
        var window = Minecraft.getInstance().getWindow();
        int scale = (int) window.getGuiScale();
        int rawX = offsetX * scale;
        int rawY = window.getScreenHeight() - ((yStart + rows * cell) * scale);
        int rawW = width * scale;
        int rawH = (rows * cell) * scale;
        RenderSystem.enableScissor(rawX, rawY, rawW, rawH);

        for (int i = start; i < end; i++) {
            int idx = i - start;
            int r = idx / cols, c = idx % cols;
            int ix = offsetX + c * cell + pad;
            int iy = yStart + r * cell;
            ItemStack s = filtered.get(i);
            gui.renderItem(s, ix, iy);
            gui.renderItemDecorations(Minecraft.getInstance().font, s, ix, iy);
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                hoveredStack = s;
                hoveredX = mx;
                hoveredY = my;
            }
        }
        RenderSystem.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // … your existing pagination & search‐box handling …

        // now the item‐grid click logic:
        int cell    = 20;
        int cols    = Math.max(1, width / cell);
        int headerH = pad + searchBox.getHeight() + 2;
        int rows    = (height - headerH - pad) / cell;
        int yStart  = offsetY + headerH;
        int perPage = cols * rows;
        int start   = (currentPage - 1) * perPage;

        for (int idx = 0; idx < perPage; idx++) {
            int row = idx / cols, col = idx % cols;
            int x = offsetX + col * cell;
            int y = yStart  + row * cell;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                int clicked = start + idx;
                if (clicked < filtered.size()) {
                    ItemStack clickedStack = filtered.get(clicked);

                    // Determine which recipe type to show
                    Screen parentScreen = Minecraft.getInstance().screen;
                    RecipeType<?> type;
                    if (parentScreen instanceof CraftingScreen) {
                        type = RecipeType.CRAFTING;
                    } else if (parentScreen instanceof AbstractFurnaceScreen<?>) {
                        type = RecipeType.SMELTING;
                    } else {
                        type = RecipeType.CRAFTING; // fallback
                    }

                    // Open the viewer
                    Minecraft.getInstance().setScreen(
                            new RecipeViewScreen(parentScreen, clickedStack, type)
                    );
                    return true;
                }
            }
        }

        return false;
    }

    @Override public boolean keyPressed(int k, int s, int m) { return searchBox.keyPressed(k, s, m); }

    @Override public boolean charTyped(char c, int m)    { return searchBox.charTyped(c, m); }

    public boolean mouseScrolled(double x, double y, double d) {
        if (d < 0) nextPage(); else if (d > 0) prevPage();
        return true;
    }

    private void prevPage() { if (currentPage > 0) currentPage--; }
    private void nextPage() { if (currentPage < getTotalPages() - 1) currentPage++; }
    private int getTotalPages() {
        int cell = 20;
        int cols = Math.max(1, width / cell);
        int headerH = pad + prevButton.getHeight() + 2;
        int footerH = pad + searchBox.getHeight() + pad;
        int rows = (height - headerH - footerH) / cell;
        int ipp = cols * rows;
        return Math.max(1, (filtered.size() + ipp - 1) / ipp);
    }

    @Override public void setFocused(boolean f) { searchBox.setFocused(f); }
    @Override public boolean isFocused()         { return searchBox.isFocused(); }
    @Override public @NotNull NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override public void updateNarration(@NotNull NarrationElementOutput out) {}
}