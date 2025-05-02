package com.knoxhack.ezi;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    public static final KeyMapping OPEN_GUI =
            new KeyMapping("key.ezi.open", GLFW.GLFW_KEY_R, "key.categories.inventory");

    public static void onRegister(RegisterKeyMappingsEvent evt) {
        evt.register(OPEN_GUI);
    }
}
