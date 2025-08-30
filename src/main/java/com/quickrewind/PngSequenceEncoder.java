package com.quickrewind;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class PngSequenceEncoder {
    
    public static void encodePngSequence(List<BufferedImage> frames, File baseOutputFile) throws IOException {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("No frames to encode");
        }
        
        // Create directory for PNG sequence
        String baseName = baseOutputFile.getName();
        if (baseName.endsWith(".gif")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        File parentDir = baseOutputFile.getParentFile();
        File sequenceDir = new File(parentDir, baseName + "_sequence");
        sequenceDir.mkdirs();
        
        System.out.println("Creating PNG sequence with " + frames.size() + " frames in: " + sequenceDir.getAbsolutePath());
        
        // Save each frame as PNG
        for (int i = 0; i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);
            File frameFile = new File(sequenceDir, String.format("frame_%03d.png", i));
            ImageIO.write(frame, "png", frameFile);
            
            if (i % 10 == 0) {
                System.out.println("Written frame " + (i + 1) + "/" + frames.size());
            }
        }
        
        // Create a summary text file
        File summaryFile = new File(sequenceDir, "README.txt");
        try (java.io.PrintWriter pw = new java.io.PrintWriter(summaryFile)) {
            pw.println("QuickRewind Screen Capture Sequence");
            pw.println("===================================");
            pw.println("Total frames: " + frames.size());
            pw.println("Capture time: ~" + (frames.size() * 0.5) + " seconds");
            pw.println();
            pw.println("To view:");
            pw.println("- Open frames in any image viewer");
            pw.println("- Use file navigation to step through frames");
            pw.println("- Or import into video editing software");
        }
        
        long totalSize = 0;
        File[] files = sequenceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                totalSize += file.length();
            }
        }
        
        System.out.println("PNG sequence created: " + formatFileSize(totalSize) + " in " + files.length + " files");
    }
    
    public static void encodeSinglePng(List<BufferedImage> frames, File outputFile) throws IOException {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("No frames to encode");
        }
        
        // Just save the last frame as a single PNG
        BufferedImage lastFrame = frames.get(frames.size() - 1);
        
        // Change extension to .png
        String filename = outputFile.getName();
        if (filename.endsWith(".gif")) {
            filename = filename.substring(0, filename.length() - 4) + ".png";
            outputFile = new File(outputFile.getParent(), filename);
        }
        
        ImageIO.write(lastFrame, "png", outputFile);
        
        long fileSize = outputFile.length();
        System.out.println("PNG screenshot saved: " + formatFileSize(fileSize));
    }
    
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}