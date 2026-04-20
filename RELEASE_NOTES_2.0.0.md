# Version 2.0.0 - Major Update

## Title
🎨 WorldEditDisplay 2.0.0 - Redesigned Rendering with Customizable Colors & Enhanced Performance

## Description

We're thrilled to announce **WorldEditDisplay 2.0.0**, a major release featuring a complete redesign of the rendering system with extensive improvements to visual customization and performance optimization.

### ✨ New Features

- **Migrated to TextDisplayShapes Framework** - Completely redesigned rendering system for more reliable and customizable display entity rendering
- **Customizable Colors** - Support for custom hex colors (#RRGGBBAA) for all visual elements across all selection types
- **Region Face Filling** - Enable visual face filling for Cuboid, Polygon, and Polyhedron selections to better visualize 3D regions
- **Dirty Region Optimization** - Intelligent dirty region tracking to improve rendering performance and reduce unnecessary updates
- **Enhanced Renderer Management** - Improved architecture for managing multiple renderer instances with better resource allocation
- **Single Region Rendering** - New capability to render or clear individual regions without affecting others
- **CUI Color Override Support** - Uses WorldEdit's CUI (Client User Interface) colors to enhance fill surface rendering
- **Debug Mode Enhancement** - Added detailed WECUI message display in debug mode for easier troubleshooting

### 🚀 Improvements

- Overall rendering architecture redesign for better performance and maintainability
- Enhanced fill color processing for more accurate visual representation
- Better multi-renderer management system
- Improved region clearing and rendering efficiency
- More robust packet handling and event listeners

### 🔧 Technical Updates

- **Java Requirement:** Updated to Java 21
- **Dependencies:** Updated to latest stable versions
- **Framework:** Migrated to TextDisplayShapes library for enhanced display entity rendering

### 📋 Breaking Changes

- Requires Java 21 (previously Java 17)
- Rendering system has been completely redesigned - configurations may need adjustment for optimal appearance

### ⚠️ Important Configuration Update

**You MUST regenerate your `config.yml` file before upgrading to 2.0.0**

Due to significant architectural changes in the rendering system, the configuration format has changed substantially. 

**Action Required:**
1. Backup your current `config.yml` file
2. Delete the old `config.yml` on your server
3. Start the plugin - it will generate a new `config.yml` with the updated format
4. Re-apply any custom settings from your backup to the new configuration file

Attempting to use the old configuration format may cause unexpected behavior or errors.

### 🐛 Stability

This release includes multiple fixes to ensure stability and better compatibility with the latest Minecraft versions.

## Download

Available on [GitHub Actions](https://github.com/TWME-TW/WorldEditDisplay/releases), [Modrinth](https://modrinth.com/plugin/worldeditdisplay)

## Support

For issues, feature requests, or questions, please open an issue on our [GitHub repository](https://github.com/TWME-TW/WorldEditDisplay/issues).
