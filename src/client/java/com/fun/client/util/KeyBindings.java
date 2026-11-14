package com.fun.client.util;

import com.fun.client.render.RadialMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class KeyBindings {
    private static final KeyMapping.Category THINK_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("think", "main"));

    public static KeyMapping openRadialMenu;

    public static void register() {
        openRadialMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.think.radial",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_V,
                THINK_CATEGORY));
    }

    public static void tick() {
        while (openRadialMenu.consumeClick()) {
            Minecraft.getInstance().gui.setScreen(new RadialMenuScreen());
        }
    }
}