package com.quickrewind;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class SystemTrayManager {
    private TrayIcon trayIcon;
    private boolean isRecording = false;
    private final QuickRewind mainApp;
    
    public SystemTrayManager(QuickRewind mainApp) {
        this.mainApp = mainApp;
        setupSystemTray();
    }
    
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported");
            return;
        }
        
        SystemTray tray = SystemTray.getSystemTray();
        Image trayImage = createTrayIcon(false);
        
        PopupMenu popup = new PopupMenu();
        MenuItem captureItem = new MenuItem("Capture GIF");
        MenuItem settingsItem = new MenuItem("Settings");
        MenuItem exitItem = new MenuItem("Exit");
        
        captureItem.addActionListener(e -> mainApp.captureGif());
        settingsItem.addActionListener(e -> mainApp.showSettings());
        exitItem.addActionListener(e -> mainApp.exit());
        
        popup.add(captureItem);
        popup.addSeparator();
        popup.add(settingsItem);
        popup.addSeparator();
        popup.add(exitItem);
        
        trayIcon = new TrayIcon(trayImage, "QuickRewind - Screen Capture", popup);
        trayIcon.setImageAutoSize(true);
        
        // Double-click to capture
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    mainApp.captureGif();
                }
            }
        });
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added: " + e.getMessage());
        }
    }
    
    private Image createTrayIcon(boolean recording) {
        // Create a simple colored circle icon
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background circle
        g2d.setColor(recording ? Color.RED : Color.GREEN);
        g2d.fillOval(2, 2, size - 4, size - 4);
        
        // Border
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(2, 2, size - 4, size - 4);
        
        // Recording indicator (small white circle)
        if (recording) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(6, 6, 4, 4);
        }
        
        g2d.dispose();
        return image;
    }
    
    public void updateRecordingStatus(boolean recording) {
        if (trayIcon != null && this.isRecording != recording) {
            this.isRecording = recording;
            trayIcon.setImage(createTrayIcon(recording));
            
            String tooltip = recording ? 
                "QuickRewind - Recording..." : 
                "QuickRewind - Ready";
            trayIcon.setToolTip(tooltip);
        }
    }
    
    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        }
    }
    
    public void dispose() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}