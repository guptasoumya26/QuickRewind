package com.quickrewind;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class SimpleSettingsDialog extends JFrame {
    private final Config config;
    private JTextField outputFolderField;
    private JSlider bufferSlider;
    private JLabel bufferValueLabel;
    private JSlider recordingFpsSlider;
    private JLabel recordingFpsValueLabel;
    private JSlider maxRecordingSlider;
    private JLabel maxRecordingValueLabel;
    private final QuickRewind mainApp;
    
    public SimpleSettingsDialog(Config config, QuickRewind mainApp) {
        super("QuickRewind Settings");
        this.config = config;
        this.mainApp = mainApp;
        
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(650, 550);
        setLocationRelativeTo(null);
        
        initComponents();
        loadCurrentSettings();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Output folder section
        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        folderPanel.add(new JLabel("Output Folder:"), BorderLayout.NORTH);
        
        outputFolderField = new JTextField();
        outputFolderField.setEditable(false);
        outputFolderField.setPreferredSize(new Dimension(400, 25));
        
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(this::browseForFolder);
        
        JPanel folderInputPanel = new JPanel(new BorderLayout(5, 0));
        folderInputPanel.add(outputFolderField, BorderLayout.CENTER);
        folderInputPanel.add(browseButton, BorderLayout.EAST);
        
        folderPanel.add(folderInputPanel, BorderLayout.CENTER);
        
        // Buffer section
        JPanel bufferPanel = new JPanel(new BorderLayout(5, 5));
        bufferPanel.add(new JLabel("Buffer Length:"), BorderLayout.NORTH);
        
        bufferSlider = new JSlider(10, 60, 30);
        bufferSlider.setMajorTickSpacing(10);
        bufferSlider.setMinorTickSpacing(5);
        bufferSlider.setPaintTicks(true);
        bufferSlider.setPaintLabels(true);
        bufferSlider.addChangeListener(e -> updateBufferLabel());
        
        bufferValueLabel = new JLabel("30 seconds", JLabel.CENTER);
        
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(bufferSlider, BorderLayout.CENTER);
        sliderPanel.add(bufferValueLabel, BorderLayout.SOUTH);
        
        bufferPanel.add(sliderPanel, BorderLayout.CENTER);
        
        // Active Recording FPS section
        JPanel recordingFpsPanel = new JPanel(new BorderLayout(5, 5));
        recordingFpsPanel.add(new JLabel("Active Recording FPS:"), BorderLayout.NORTH);
        
        recordingFpsSlider = new JSlider(5, 30, 10);
        recordingFpsSlider.setMajorTickSpacing(5);
        recordingFpsSlider.setMinorTickSpacing(1);
        recordingFpsSlider.setPaintTicks(true);
        recordingFpsSlider.setPaintLabels(true);
        recordingFpsSlider.addChangeListener(e -> updateRecordingFpsLabel());
        
        recordingFpsValueLabel = new JLabel("10 FPS", JLabel.CENTER);
        
        JPanel recordingFpsSliderPanel = new JPanel(new BorderLayout());
        recordingFpsSliderPanel.add(recordingFpsSlider, BorderLayout.CENTER);
        recordingFpsSliderPanel.add(recordingFpsValueLabel, BorderLayout.SOUTH);
        
        recordingFpsPanel.add(recordingFpsSliderPanel, BorderLayout.CENTER);
        
        // Max Recording Time section
        JPanel maxRecordingPanel = new JPanel(new BorderLayout(5, 5));
        maxRecordingPanel.add(new JLabel("Max Recording Time:"), BorderLayout.NORTH);
        
        maxRecordingSlider = new JSlider(1, 15, 10);
        maxRecordingSlider.setMajorTickSpacing(2);
        maxRecordingSlider.setMinorTickSpacing(1);
        maxRecordingSlider.setPaintTicks(true);
        maxRecordingSlider.setPaintLabels(true);
        maxRecordingSlider.addChangeListener(e -> updateMaxRecordingLabel());
        
        maxRecordingValueLabel = new JLabel("10 minutes", JLabel.CENTER);
        
        JPanel maxRecordingSliderPanel = new JPanel(new BorderLayout());
        maxRecordingSliderPanel.add(maxRecordingSlider, BorderLayout.CENTER);
        maxRecordingSliderPanel.add(maxRecordingValueLabel, BorderLayout.SOUTH);
        
        maxRecordingPanel.add(maxRecordingSliderPanel, BorderLayout.CENTER);
        
        // Current location info
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Usage Information"));
        
        JTextArea infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setBackground(getBackground());
        infoText.setText(
            "Buffer Mode:\\n" +
            "• Hotkey: Ctrl+Shift+G\\n" +
            "• Double-click tray icon to capture buffer\\n" +
            "\\n" +
            "Active Recording Mode:\\n" +
            "• Right-click tray → Start Recording\\n" +
            "• Right-click tray → Stop Recording\\n" +
            "• Recording auto-stops at max time limit\\n" +
            "\\n" +
            "Output:\\n" +
            "• GIFs saved with timestamp\\n" +
            "• Markdown links copied to clipboard"
        );
        
        infoPanel.add(infoText, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save & Apply");
        JButton cancelButton = new JButton("Cancel");
        JButton testButton = new JButton("Open Output Folder");
        
        saveButton.addActionListener(this::saveSettings);
        cancelButton.addActionListener(e -> setVisible(false));
        testButton.addActionListener(this::openOutputFolder);
        
        buttonPanel.add(testButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        // Add all panels
        mainPanel.add(folderPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(bufferPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(recordingFpsPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(maxRecordingPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(infoPanel);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadCurrentSettings() {
        outputFolderField.setText(config.getOutputFolder());
        bufferSlider.setValue(config.getBufferSeconds());
        recordingFpsSlider.setValue(config.getActiveRecordingFPS());
        maxRecordingSlider.setValue(config.getMaxRecordingMinutes());
        updateBufferLabel();
        updateRecordingFpsLabel();
        updateMaxRecordingLabel();
    }
    
    private void updateBufferLabel() {
        int value = bufferSlider.getValue();
        bufferValueLabel.setText(value + " seconds");
    }
    
    private void updateRecordingFpsLabel() {
        int value = recordingFpsSlider.getValue();
        recordingFpsValueLabel.setText(value + " FPS");
    }
    
    private void updateMaxRecordingLabel() {
        int value = maxRecordingSlider.getValue();
        maxRecordingValueLabel.setText(value + " minutes");
    }
    
    private void browseForFolder(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(config.getOutputFolder()));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            outputFolderField.setText(selectedFolder.getAbsolutePath());
        }
    }
    
    private void openOutputFolder(ActionEvent e) {
        try {
            File folder = new File(config.getOutputFolder());
            if (!folder.exists()) {
                folder.mkdirs();
            }
            Desktop.getDesktop().open(folder);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Cannot open folder: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveSettings(ActionEvent e) {
        String newOutputFolder = outputFolderField.getText().trim();
        int newBufferSeconds = bufferSlider.getValue();
        int newRecordingFps = recordingFpsSlider.getValue();
        int newMaxRecordingMinutes = maxRecordingSlider.getValue();
        
        // Validate and create output folder
        File outputDir = new File(newOutputFolder);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                JOptionPane.showMessageDialog(this, 
                    "Cannot create output folder: " + newOutputFolder,
                    "Invalid Folder", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        // Update config
        config.setOutputFolder(newOutputFolder);
        config.setBufferSeconds(newBufferSeconds);
        config.setActiveRecordingFPS(newRecordingFps);
        config.setMaxRecordingMinutes(newMaxRecordingMinutes);
        config.save();
        
        // Notify main app
        mainApp.onSettingsChanged();
        
        JOptionPane.showMessageDialog(this, 
            "Settings saved successfully!\\n" + 
            "Output: " + newOutputFolder + "\\n" + 
            "Buffer: " + newBufferSeconds + " seconds\\n" +
            "Recording FPS: " + newRecordingFps + "\\n" +
            "Max Recording: " + newMaxRecordingMinutes + " minutes", 
            "Settings Saved", 
            JOptionPane.INFORMATION_MESSAGE);
        
        setVisible(false);
    }
}