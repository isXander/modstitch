package com.example.test.neoforge.utils;

import com.mojang.blaze3d.platform.Window;

public class WindowUtils {
    public static int getWindowedX(Window window) {
        return window.windowedX;
    }

    public static int getWindowedY(Window window) {
        return window.windowedY;
    }

    public static int getWindowedWidth(Window window) {
        return window.windowedWidth;
    }

    public static int getWindowedHeight(Window window) {
        return window.windowedHeight;
    }

    public static void refreshFramebufferSize(Window window) {
        window.refreshFramebufferSize();
    }
}
