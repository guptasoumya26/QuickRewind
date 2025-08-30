package com.quickrewind;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class SimpleGifEncoder {
    
    public static void encodeGif(List<BufferedImage> frames, File outputFile, int delayMs) throws IOException {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("No frames to encode");
        }
        
        // Get GIF writer
        ImageWriter writer = null;
        java.util.Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (writers.hasNext()) {
            writer = writers.next();
        }
        
        if (writer == null) {
            throw new IOException("No GIF writer found");
        }
        
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);
            
            System.out.println("Creating GIF with " + frames.size() + " frames...");
            
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage frame = frames.get(i);
                
                // Convert to indexed color for better GIF compatibility
                BufferedImage indexedFrame = convertToIndexedColor(frame);
                
                // Create metadata for animation
                IIOMetadata metadata = createMetadata(writer, indexedFrame, delayMs, i == 0);
                
                // Write the frame
                writer.writeToSequence(new IIOImage(indexedFrame, null, metadata), null);
                
                if (i % 10 == 0) {
                    System.out.println("Written frame " + (i + 1) + "/" + frames.size());
                }
            }
            
            writer.endWriteSequence();
            
            long fileSize = outputFile.length();
            System.out.println("GIF created successfully: " + formatFileSize(fileSize));
            
        } finally {
            writer.dispose();
        }
    }
    
    private static BufferedImage convertToIndexedColor(BufferedImage src) {
        // Create a new indexed color image
        BufferedImage indexed = new BufferedImage(
            src.getWidth(), 
            src.getHeight(), 
            BufferedImage.TYPE_BYTE_INDEXED
        );
        
        Graphics2D g2d = indexed.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        
        return indexed;
    }
    
    private static IIOMetadata createMetadata(ImageWriter writer, BufferedImage image, int delayMs, boolean isFirst) throws IOException {
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(image.getType());
        IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
        
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
        
        // Set up animation properties
        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", String.valueOf(delayMs / 10)); // Convert to centiseconds
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
        
        // Set loop count on first frame
        if (isFirst) {
            IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
            IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
            child.setAttribute("applicationID", "NETSCAPE");
            child.setAttribute("authenticationCode", "2.0");
            
            // Play once only (no loop)
            byte[] playOnce = {0x1, 0x1, 0x0};
            child.setUserObject(playOnce);
            appExtensionsNode.appendChild(child);
        }
        
        metadata.setFromTree(metaFormatName, root);
        return metadata;
    }
    
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        
        // Node doesn't exist, create it
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }
    
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}