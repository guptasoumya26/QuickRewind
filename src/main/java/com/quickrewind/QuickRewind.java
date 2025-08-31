package com.quickrewind;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuickRewind {
    private Config config;
    private ScreenCapture screenCapture;
    private SystemTrayManager trayManager;
    private HotkeyManager hotkeyManager;
    private SimpleSettingsDialog settingsDialog;
    
    public QuickRewind() {
  
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set system look and feel: " + e.getMessage());
        }
        
        // Check if system tray is supported
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, 
                "System tray is not supported on this platform.\nQuickRewind requires system tray support.",
                "System Tray Not Supported", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        initializeApplication();
    }
    
    private void initializeApplication() {
        // Load configuration
        config = Config.load();
        
        try {
            // Initialize screen capture with current buffer setting
            screenCapture = new ScreenCapture(config.getBufferSeconds());
            
            // Initialize system tray
            trayManager = new SystemTrayManager(this);
            
            // Initialize global hotkey listener
            hotkeyManager = new HotkeyManager(this);
            
            // Start screen capture
            screenCapture.startCapture();
            trayManager.updateRecordingStatus(true);
            
            System.out.println("QuickRewind started successfully!");
            System.out.println("Output folder: " + config.getOutputFolder());
            System.out.println("Buffer length: " + config.getBufferSeconds() + " seconds");
            System.out.println("Hotkey: " + config.getHotkeyCombo());
            System.out.println("Right-click tray icon for settings");
            
            trayManager.showNotification("QuickRewind Started", 
                "Press " + config.getHotkeyCombo() + " or double-click tray icon to capture GIF",
                TrayIcon.MessageType.INFO);
                
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, 
                "Failed to initialize screen capture: " + e.getMessage(),
                "Initialization Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    public void captureGif() {
        captureGifFromBuffer();
    }
    
    public void captureGifFromBuffer() {
        CompletableFuture.runAsync(() -> {
            try {
                // Get current frames from buffer
                BufferedImage[] frames = screenCapture.getBufferedFrames();
                
                if (frames.length == 0) {
                    SwingUtilities.invokeLater(() -> 
                        trayManager.showNotification("Capture Failed", 
                            "No frames available in buffer", 
                            TrayIcon.MessageType.WARNING));
                    return;
                }
                
                processAndSaveGif(frames, "buffer", 500);
                        
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    trayManager.showNotification("Capture Failed", 
                        "Error creating GIF: " + e.getMessage(), 
                        TrayIcon.MessageType.ERROR));
            }
        });
    }
    
    public void startActiveRecording() {
        if (screenCapture.isActiveRecording()) {
            trayManager.showNotification("Already Recording", 
                "Active recording is already in progress", 
                TrayIcon.MessageType.WARNING);
            return;
        }
        
        screenCapture.startActiveRecording();
        trayManager.updateRecordingStatus(true);
        trayManager.showNotification("Recording Started", 
            "Active recording started. Click 'Stop Recording' to save.", 
            TrayIcon.MessageType.INFO);
    }
    
    public void stopActiveRecording() {
        if (!screenCapture.isActiveRecording()) {
            trayManager.showNotification("No Active Recording", 
                "No active recording in progress", 
                TrayIcon.MessageType.WARNING);
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                // Get frames from active recording
                BufferedImage[] frames = screenCapture.getActiveRecordingFrames();
                long duration = screenCapture.getActiveRecordingDuration();
                
                // Stop the recording
                screenCapture.stopActiveRecording();
                trayManager.updateRecordingStatus(false);
                
                if (frames.length == 0) {
                    SwingUtilities.invokeLater(() -> 
                        trayManager.showNotification("Recording Failed", 
                            "No frames captured during recording", 
                            TrayIcon.MessageType.WARNING));
                    return;
                }
                
                // Calculate appropriate delay based on recording duration
                int delayMs = Math.max(100, (int)(duration / frames.length));
                delayMs = Math.min(delayMs, 1000); // Cap at 1 second per frame
                
                processAndSaveGif(frames, "recording", delayMs);
                screenCapture.clearActiveRecordingFrames(); // Clean up memory
                        
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    trayManager.showNotification("Recording Failed", 
                        "Error saving recording: " + e.getMessage(), 
                        TrayIcon.MessageType.ERROR));
            }
        });
    }
    
    private void processAndSaveGif(BufferedImage[] frames, String prefix, int delayMs) {
        try {
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = "quickrewind-" + prefix + "-" + timestamp + ".gif";
            File outputFile = new File(config.getOutputFolder(), filename);
            
            System.out.println("Saving GIF to: " + outputFile.getAbsolutePath());
            
            // Show processing notification
            SwingUtilities.invokeLater(() -> 
                trayManager.showNotification("Processing...", 
                    "Creating optimized GIF from " + frames.length + " frames", 
                    TrayIcon.MessageType.INFO));
            
            // Try multiple encoding options with fallbacks
            List<BufferedImage> frameList = Arrays.asList(frames);
            
            try {
                // First try: Java ImageIO GIF encoder
                SimpleGifEncoder.encodeGif(frameList, outputFile, delayMs);
            } catch (Exception gifError) {
                System.err.println("GIF encoding failed: " + gifError.getMessage());
                
                try {
                    // Second try: PNG sequence
                    PngSequenceEncoder.encodePngSequence(frameList, outputFile);
                    SwingUtilities.invokeLater(() -> 
                        trayManager.showNotification("Created PNG Sequence", 
                            "GIF failed, saved as PNG sequence instead", 
                            TrayIcon.MessageType.WARNING));
                    return;
                } catch (Exception pngError) {
                    System.err.println("PNG sequence failed: " + pngError.getMessage());
                    
                    // Last resort: Single PNG screenshot
                    PngSequenceEncoder.encodeSinglePng(frameList, outputFile);
                    SwingUtilities.invokeLater(() -> 
                        trayManager.showNotification("Saved Screenshot", 
                            "Animation failed, saved last frame as PNG", 
                            TrayIcon.MessageType.WARNING));
                    return;
                }
            }
            
            // Copy markdown link to clipboard
            ClipboardHelper.copyMarkdownLink(outputFile);
            
            // Show success notification
            SwingUtilities.invokeLater(() -> 
                trayManager.showNotification("GIF Saved!", 
                    "Saved: " + filename + "\nMarkdown link copied to clipboard", 
                    TrayIcon.MessageType.INFO));
                    
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> 
                trayManager.showNotification("Save Failed", 
                    "Error saving GIF: " + e.getMessage(), 
                    TrayIcon.MessageType.ERROR));
        }
    }
    
    public void showSettings() {
        SwingUtilities.invokeLater(() -> {
            if (settingsDialog == null) {
                settingsDialog = new SimpleSettingsDialog(config, this);
            }
            settingsDialog.setVisible(true);
            settingsDialog.toFront();
            settingsDialog.requestFocus();
        });
    }
    
    public void onSettingsChanged() {
        // Restart screen capture with new buffer size if it changed
        if (screenCapture != null) {
            boolean wasCapturing = screenCapture.isCapturing();
            screenCapture.stopCapture();
            
            try {
                screenCapture = new ScreenCapture(config.getBufferSeconds());
                if (wasCapturing) {
                    screenCapture.startCapture();
                }
            } catch (AWTException e) {
                System.err.println("Failed to restart screen capture: " + e.getMessage());
            }
        }
        
        trayManager.showNotification("Settings Updated", 
            "Buffer: " + config.getBufferSeconds() + "s, Output: " + config.getOutputFolder(),
            TrayIcon.MessageType.INFO);
    }
    
    public void exit() {
        if (screenCapture != null) {
            screenCapture.stopCapture();
        }
        if (hotkeyManager != null) {
            hotkeyManager.dispose();
        }
        if (trayManager != null) {
            trayManager.dispose();
        }
        
        System.exit(0);
    }
    
    public static void main(String[] args) {
        // Ensure EDT for Swing
        SwingUtilities.invokeLater(() -> {
            new QuickRewind();
        });
    }
}