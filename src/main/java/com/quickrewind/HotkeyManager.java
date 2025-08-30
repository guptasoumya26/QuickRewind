package com.quickrewind;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HotkeyManager implements NativeKeyListener {
    private final QuickRewind mainApp;
    private boolean ctrlPressed = false;
    private boolean shiftPressed = false;
    
    public HotkeyManager(QuickRewind mainApp) {
        this.mainApp = mainApp;
        setupGlobalHook();
    }
    
    private void setupGlobalHook() {
        try {
            // Disable logging for JNativeHook to reduce console spam
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            logger.setUseParentHandlers(false);
            
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            
        } catch (NativeHookException e) {
            System.err.println("Failed to register global hook: " + e.getMessage());
        }
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        switch (e.getKeyCode()) {
            case NativeKeyEvent.VC_CONTROL:
                ctrlPressed = true;
                break;
            case NativeKeyEvent.VC_SHIFT:
                shiftPressed = true;
                break;
            case NativeKeyEvent.VC_G:
                if (ctrlPressed && shiftPressed) {
                    // Trigger capture
                    SwingUtilities.invokeLater(() -> mainApp.captureGif());
                }
                break;
        }
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        switch (e.getKeyCode()) {
            case NativeKeyEvent.VC_CONTROL:
                ctrlPressed = false;
                break;
            case NativeKeyEvent.VC_SHIFT:
                shiftPressed = false;
                break;
        }
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used
    }
    
    public void dispose() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            System.err.println("Failed to unregister global hook: " + e.getMessage());
        }
    }
}