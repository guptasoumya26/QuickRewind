package com.quickrewind;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCapture {
    private final Robot robot;
    private final Rectangle screenBounds;
    private final BlockingQueue<BufferedImage> frameBuffer;
    private final AtomicBoolean isCapturing;
    private final int maxBufferSeconds;
    private final int framesPerSecond;
    private Thread captureThread;

    public ScreenCapture(int bufferSeconds) throws AWTException {
        this.robot = new Robot();
        this.screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        this.maxBufferSeconds = bufferSeconds;
        this.framesPerSecond = 2; // 2 FPS to reduce system load further
        this.frameBuffer = new LinkedBlockingQueue<>(maxBufferSeconds * framesPerSecond);
        this.isCapturing = new AtomicBoolean(false);
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
    }

    private void captureLoop() {
        long frameInterval = 1000 / framesPerSecond; // milliseconds per frame
        
        while (isCapturing.get()) {
            try {
                long startTime = System.currentTimeMillis();
                
                // Use lower quality capture to reduce memory usage and processing time
                BufferedImage screenshot = robot.createScreenCapture(screenBounds);
                
                // Scale down screenshot to reduce memory usage (75% of original size)
                int scaledWidth = (int)(screenshot.getWidth() * 0.75);
                int scaledHeight = (int)(screenshot.getHeight() * 0.75);
                BufferedImage scaledScreenshot = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = scaledScreenshot.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(screenshot, 0, 0, scaledWidth, scaledHeight, null);
                g2d.dispose();
                
                // Use scaled screenshot instead of original
                screenshot = scaledScreenshot;
                
                // Add delay to prevent overwhelming system
                Thread.sleep(50);
                
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
}