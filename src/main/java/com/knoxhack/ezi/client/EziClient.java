package com.knoxhack.ezi.client;

import com.knoxhack.ezi.RecipeRegistry;
import com.knoxhack.ezi.client.widgets.ItemListWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class EziClient {
    private static final int PANEL_WIDTH   = 150;
    private static final int PANEL_PADDING = 2;

    // one widget instance, rebuilt each time you open an inventory
    private static ItemListWidget widget;

    public static void init() {
        // rebuild the widget as soon as any container screen initializes
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class,   EziClient::onScreenInit);
        // draw it after the screen renders
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, EziClient::onScreenRender);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, EziClient::onMousePressed);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseScrolled.Pre.class,      EziClient::onMouseScrolled);
    }

    private static void onScreenInit(ScreenEvent.Init.Post evt) {
        if (!(evt.getScreen() instanceof AbstractContainerScreen<?>)) return;

        var mc     = Minecraft.getInstance();
        var window = mc.getWindow();

        // full‐height panel (minus padding top + bottom)
        int panelH = window.getGuiScaledHeight() - PANEL_PADDING * 2;

        widget = new ItemListWidget(PANEL_WIDTH, panelH, PANEL_PADDING);

        // position it flush against the **screen**’s right edge
        widget.setPosition(
                window.getGuiScaledWidth() - PANEL_WIDTH - PANEL_PADDING,
                PANEL_PADDING
        );

        evt.addListener(widget);

        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, event -> {
            if (Minecraft.getInstance().level != null) {
                RecipeRegistry.bootstrap();
            }
        });
    }

    private static void onScreenRender(ScreenEvent.Render.Post evt) {
        RecipeRegistry.bootstrap();
        if (widget == null || !(evt.getScreen() instanceof AbstractContainerScreen<?>)) return;

        var mc     = Minecraft.getInstance();
        var window = mc.getWindow();
        int screenWpx = window.getScreenWidth();   // raw pixel width
        int screenHpx = window.getScreenHeight();  // raw pixel height

        GuiGraphics gui = evt.getGuiGraphics();

        // draw **after** the inventory background/slots
        gui.pose().pushPose();

        // 1) lift any vanilla scissor (so our panel spans outside the slot area)
        RenderSystem.disableScissor();

        // 2) render the panel (background + search + items)
        widget.render(gui, evt.getMouseX(), evt.getMouseY(), evt.getPartialTick());

        // 3) restore a full‐window scissor so tooltips etc still clip correctly
        RenderSystem.enableScissor(
                0,
                0,
                screenWpx,
                screenHpx
        );

        gui.pose().popPose();
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (widget == null || !(evt.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (widget.mouseClicked(evt.getMouseX(), evt.getMouseY(), evt.getButton())) {
            evt.setCanceled(true);  // consumes the click so your slots don’t grab it
        }
    }

    private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre evt) {
        if (widget == null || !(evt.getScreen() instanceof AbstractContainerScreen<?>)) return;
        // scrollDeltaY is the wheel movement
        if (widget.mouseScrolled(evt.getMouseX(), evt.getMouseY(), evt.getScrollDeltaX(), evt.getScrollDeltaY())) {
            evt.setCanceled(true);
        }
    }
}