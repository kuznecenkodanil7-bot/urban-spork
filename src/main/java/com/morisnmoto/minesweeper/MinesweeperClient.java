package com.morisnmoto.minesweeper;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class MinesweeperClient implements ClientModInitializer {
    public static final String MOD_ID = "ingame_minesweeper";

    private static KeyMapping openMinesweeperKey;

    @Override
    public void onInitializeClient() {
        openMinesweeperKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key." + MOD_ID + ".open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON, // Russian Ж key on a standard ЙЦУКЕН keyboard layout.
                "category." + MOD_ID
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMinesweeperKey.consumeClick()) {
                client.setScreen(new MinesweeperScreen());
            }
        });
    }
}
