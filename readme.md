# C++ File Organizer Plugin

A JetBrains Rider plugin that automatically organizes `.cpp` and `.h` files into dedicated editor windows for better code navigation and workflow management.

## Features

- **Automatic File Organization**: Separates C++ source files (`.cpp`) and header files (`.h`) into different editor windows
- **Improved Workflow**: Keep your implementation and header files organized in separate tabs for easier navigation
- **Seamless Integration**: Works automatically when you open C++ files in Rider

## Installation

### From JetBrains Marketplace
*(not planned)*

### Manual Installation
1. Download the latest plugin `.zip` file from the releases page
2. Open your JetBrains IDE (IntelliJ IDEA, CLion, etc.)
3. Go to `Settings/Preferences` → `Plugins`
4. Click the gear icon ⚙️ and select `Install Plugin from Disk...`
5. Select the downloaded `.zip` file
6. Restart the IDE

## Usage

The plugin works automatically once installed:

1. Open any `.cpp` or `.h` file in your project
2. The plugin will automatically organize the files into appropriate editor windows
3. Continue working with your files as usual - the organization happens in the background

## Requirements

- Rider IDE 2025.2 or later

## Building from Source
                              
* Open in IDEA 2025.2:
 * run the "Run Plugin" task to run the plugin in Rider IDE;
 * run the "Build Plugin" task to build the plugin .zip under the `build/distributions` directory. .