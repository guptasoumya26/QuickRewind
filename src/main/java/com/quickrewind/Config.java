package com.quickrewind;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private static final String CONFIG_FILE = "quickrewind-config.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private String outputFolder;
    private int bufferSeconds;
    private String hotkeyCombo;
    private int activeRecordingFPS;
    private int maxRecordingMinutes;
    
    public Config() {
        // Default values
        this.outputFolder = System.getProperty("user.home") + File.separator + "QuickRewind";
        this.bufferSeconds = 30;
        this.hotkeyCombo = "Ctrl+Shift+G";
        this.activeRecordingFPS = 10;
        this.maxRecordingMinutes = 10;
    }
    
    public String getOutputFolder() {
        return outputFolder;
    }
    
    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }
    
    public int getBufferSeconds() {
        return bufferSeconds;
    }
    
    public void setBufferSeconds(int bufferSeconds) {
        this.bufferSeconds = Math.max(10, Math.min(60, bufferSeconds)); // Clamp between 10-60 seconds
    }
    
    public String getHotkeyCombo() {
        return hotkeyCombo;
    }
    
    public void setHotkeyCombo(String hotkeyCombo) {
        this.hotkeyCombo = hotkeyCombo;
    }
    
    public int getActiveRecordingFPS() {
        return activeRecordingFPS;
    }
    
    public void setActiveRecordingFPS(int activeRecordingFPS) {
        this.activeRecordingFPS = Math.max(5, Math.min(30, activeRecordingFPS)); // Clamp between 5-30 FPS
    }
    
    public int getMaxRecordingMinutes() {
        return maxRecordingMinutes;
    }
    
    public void setMaxRecordingMinutes(int maxRecordingMinutes) {
        this.maxRecordingMinutes = Math.max(1, Math.min(15, maxRecordingMinutes)); // Clamp between 1-15 minutes
    }
    
    public static Config load() {
        Path configPath = getConfigPath();
        
        if (!Files.exists(configPath)) {
            Config defaultConfig = new Config();
            defaultConfig.save();
            return defaultConfig;
        }
        
        try {
            String json = Files.readString(configPath);
            Config config = objectMapper.readValue(json, Config.class);
            
            // Ensure output folder exists
            File outputDir = new File(config.getOutputFolder());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            return config;
        } catch (IOException e) {
            System.err.println("Failed to load config, using defaults: " + e.getMessage());
            return new Config();
        }
    }
    
    public void save() {
        try {
            Path configPath = getConfigPath();
            Files.createDirectories(configPath.getParent());
            
            // Ensure output folder exists
            File outputDir = new File(this.outputFolder);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
    
    private static Path getConfigPath() {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            return Paths.get(appData, "QuickRewind", CONFIG_FILE);
        } else {
            // Fallback to user home
            return Paths.get(System.getProperty("user.home"), ".quickrewind", CONFIG_FILE);
        }
    }
}