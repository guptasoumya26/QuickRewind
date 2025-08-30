package com.quickrewind;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;

public class ClipboardHelper {
    
    public static void copyMarkdownLink(File gifFile, String baseUrl) {
        String fileName = gifFile.getName();
        String markdownLink;
        
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            // If a base URL is provided, create a web-accessible link
            markdownLink = String.format("![%s](%s/%s)", fileName, baseUrl.trim(), fileName);
        } else {
            // Create a local file link
            String absolutePath = gifFile.getAbsolutePath().replace("\\", "/");
            markdownLink = String.format("![%s](file:///%s)", fileName, absolutePath);
        }
        
        copyToClipboard(markdownLink);
    }
    
    public static void copyMarkdownLink(File gifFile) {
        copyMarkdownLink(gifFile, null);
    }
    
    public static void copyToClipboard(String text) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
        } catch (Exception e) {
            System.err.println("Failed to copy to clipboard: " + e.getMessage());
        }
    }
    
    public static String createMarkdownLink(File gifFile) {
        String fileName = gifFile.getName();
        String absolutePath = gifFile.getAbsolutePath().replace("\\", "/");
        return String.format("![%s](file:///%s)", fileName, absolutePath);
    }
}