# QuickRewind - Instant GIF Evidence Recorder

A Java-based Windows application that continuously captures your screen and instantly creates GIFs when you need evidence of bugs or issues.

## Features

- **Continuous Recording**: Keeps 30-60 seconds of screen activity in memory
- **Instant Capture**: Press `Ctrl+Shift+G` or double-click the tray icon to save the last 30-60 seconds as a GIF
- **System Tray Integration**: Runs quietly in the background with status indicator
- **Automatic Markdown Links**: Copies markdown-formatted links to clipboard for easy pasting into Jira/Notion/Slack
- **Configurable Settings**: Adjust buffer length and output folder
- **Primary Monitor Only**: Captures only the primary monitor for better performance

## Usage

1. **Run the Application**: Double-click `quick-rewind-1.0.0.jar` to start
2. **Capture GIF**: Press `Ctrl+Shift+G` or double-click the tray icon
3. **Find Your GIF**: Files are saved to the configured output folder (default: `~/QuickRewind/`)
4. **Paste Link**: The markdown link is automatically copied to clipboard

## System Tray

- **Green Circle**: Ready/Recording buffer
- **Red Circle**: Processing/Saving GIF
- **Right-click**: Access settings and exit options

## Settings

Right-click the tray icon → Settings to configure:
- **Output Folder**: Where GIF files are saved
- **Buffer Length**: 30-60 seconds of recording history

## Requirements

- Windows 10 or later
- Java 11 or higher
- System tray support

## Building from Source

```bash
mvn clean package
```

The executable JAR will be created in the `target/` directory.

## File Naming

GIF files are automatically named with timestamps: `quickrewind-YYYYMMDD-HHMMSS.gif`

## Performance

- Captures at 10 FPS for optimal size/quality balance
- GIF files are typically under 10MB
- Minimal CPU usage when idle