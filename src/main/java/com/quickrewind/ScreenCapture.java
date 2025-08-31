package com.quickrewind;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScreenCapture {
    private final Robot robot;
    private final Rectangle screenBounds;
    private final BlockingQueue<BufferedImage> frameBuffer;
    private final AtomicBoolean isCapturing;
    private final AtomicBoolean isActiveRecording;
    private final AtomicLong activeRecordingStartTime;
    private final ConcurrentLinkedQueue<BufferedImage> activeRecordingFrames;
    private final int maxBufferSeconds;
    private final int maxActiveRecordingMinutes;
    private final int framesPerSecond;
    private final int activeRecordingFPS;
    private Thread captureThread;
    private Thread activeRecordingThread;

    public ScreenCapture(int bufferSeconds) throws AWTException {
        this.robot = new Robot();
        this.screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        this.maxBufferSeconds = bufferSeconds;
        this.maxActiveRecordingMinutes = 10; // Maximum 10 minutes for active recording
        this.framesPerSecond = 2; // 2 FPS to reduce system load for buffer
        this.activeRecordingFPS = 10; // Higher FPS for active recording
        this.frameBuffer = new LinkedBlockingQueue<>(maxBufferSeconds * framesPerSecond);
        this.activeRecordingFrames = new ConcurrentLinkedQueue<>();
        this.isCapturing = new AtomicBoolean(false);
        this.isActiveRecording = new AtomicBoolean(false);
        this.activeRecordingStartTime = new AtomicLong(0);
    }

    public void startCapture() {
        if (isCapturing.get()) return;
        
        isCapturing.set(true);
        captureThread = new Thread(this::captureLoop);
        captureThread.setDaemon(true);
        captureThread.setPriority(Thread.MIN_PRIORITY); // Run at lowest priority to reduce system impact
        captureThread.start();
    }

    public void stopCapture() {
        isCapturing.set(false);
        if (captureThread != null) {
            captureThread.interrupt();
        }
        stopActiveRecording(); // Also stop active recording if running
    }

    private void captureLoop() {
        long frameInterval = 1000 / framesPerSecond; // milliseconds per frame
        BufferedImage reusableImage = null;
        
        while (isCapturing.get()) {
            try {
                long startTime = System.currentTimeMillis();
                
                // Capture screen with optimized settings
                BufferedImage screenshot = captureScreenOptimized();
                
                // Add to buffer, removing oldest frame if buffer is full
                if (frameBuffer.remainingCapacity() == 0) {
                    frameBuffer.poll(); // Remove oldest frame
                }
                frameBuffer.offer(screenshot);
                
                // Sleep to maintain frame rate
                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = frameInterval - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error capturing screen: " + e.getMessage());
            }
        }
    }
    
    private BufferedImage captureScreenOptimized() {
        // Use lower quality capture to reduce memory usage and processing time
        BufferedImage screenshot = robot.createScreenCapture(screenBounds);
        
        // Scale down screenshot to reduce memory usage (60% of original size for better performance)
        int scaledWidth = (int)(screenshot.getWidth() * 0.6);
        int scaledHeight = (int)(screenshot.getHeight() * 0.6);
        BufferedImage scaledScreenshot = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledScreenshot.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.drawImage(screenshot, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return scaledScreenshot;
    }

    public BufferedImage[] getBufferedFrames() {
        return frameBuffer.toArray(new BufferedImage[0]);
    }

    public boolean isCapturing() {
        return isCapturing.get();
    }

    public Rectangle getScreenBounds() {
        return screenBounds;
    }

    public void setBufferSeconds(int seconds) {
        // Note: This would require restarting capture to take effect
        // For now, just store the value for next restart
    }
    
    // Active Recording Methods
    public void startActiveRecording() {
        if (isActiveRecording.get()) return;
        
        System.out.println("Starting active recording...");
        isActiveRecording.set(true);
        activeRecordingStartTime.set(System.currentTimeMillis());
        activeRecordingFrames.clear();
        
        activeRecordingThread = new Thread(this::activeRecordingLoop);
        activeRecordingThread.setDaemon(true);
        activeRecordingThread.setPriority(Thread.NORM_PRIORITY);
        activeRecordingThread.start();
    }
    
    public void stopActiveRecording() {
        if (!isActiveRecording.get()) return;
        
        System.out.println("Stopping active recording...");
        isActiveRecording.set(false);
        if (activeRecordingThread != null) {
            activeRecordingThread.interrupt();
        }
    }
    
    private void activeRecordingLoop() {
        long frameInterval = 1000 / activeRecordingFPS;
        long maxRecordingTime = maxActiveRecordingMinutes * 60 * 1000L; // 10 minutes in milliseconds
        
        while (isActiveRecording.get()) {
            try {
                long currentTime = System.currentTimeMillis();
                long recordingDuration = currentTime - activeRecordingStartTime.get();
                
                // Check if we've reached the maximum recording time
                if (recordingDuration >= maxRecordingTime) {
                    System.out.println("Maximum recording time reached (10 minutes), stopping...");
                    break;
                }
                
                long frameStartTime = System.currentTimeMillis();
                
                // Capture screen for active recording (higher quality)
                BufferedImage screenshot = captureScreenForActiveRecording();
                activeRecordingFrames.offer(screenshot);
                
                // Memory management: if we have too many frames, remove some old ones
                if (activeRecordingFrames.size() > maxActiveRecordingMinutes * 60 * activeRecordingFPS) {
                    for (int i = 0; i < activeRecordingFPS && !activeRecordingFrames.isEmpty(); i++) {
                        activeRecordingFrames.poll();
                    }
                }
                
                // Sleep to maintain frame rate
                long elapsed = System.currentTimeMillis() - frameStartTime;
                long sleepTime = frameInterval - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error in active recording: " + e.getMessage());
            }
        }
        
        isActiveRecording.set(false);
    }
    
    private BufferedImage captureScreenForActiveRecording() {
        BufferedImage screenshot = robot.createScreenCapture(screenBounds);
        
        // For active recording, use better quality (80% scaling)
        int scaledWidth = (int)(screenshot.getWidth() * 0.8);
        int scaledHeight = (int)(screenshot.getHeight() * 0.8);
        BufferedImage scaledScreenshot = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledScreenshot.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(screenshot, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        
        return scaledScreenshot;
    }
    
    public boolean isActiveRecording() {
        return isActiveRecording.get();
    }
    
    public long getActiveRecordingDuration() {
        if (!isActiveRecording.get()) return 0;
        return System.currentTimeMillis() - activeRecordingStartTime.get();
    }
    
    public int getActiveRecordingFrameCount() {
        return activeRecordingFrames.size();
    }
    
    public BufferedImage[] getActiveRecordingFrames() {
        return activeRecordingFrames.toArray(new BufferedImage[0]);
    }
    
    public void clearActiveRecordingFrames() {
        activeRecordingFrames.clear();
    }
}