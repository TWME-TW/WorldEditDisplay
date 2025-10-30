# WorldEditDisplay

<div align="center">

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg)](https://www.minecraft.net/)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache2.0-blue.svg)](LICENSE)

A server-side WorldEdit selection visualization plugin | WorldEdit 選區視覺化伺服器端插件

[English](#english) | [繁體中文](#繁體中文)

</div>

---

## English

### About

WorldEditDisplay is a server-side plugin for Minecraft that adds visual selection rendering for WorldEdit. It intercepts WorldEdit's CUI (Client User Interface) protocol packets and renders selections using Display Entities on the server side. This means players can see their WorldEdit selections without installing any client-side mods.

### Features

**Selection Types**
- Cuboid - Box selections with edge lines and grid display
- Polygon - 2D polygon boundaries with vertical columns
- Ellipsoid - Smooth elliptical selections
- Cylinder - Circular selections with adjustable height
- Polyhedron - Complex 3D polyhedral structures

**Visual Rendering**
- Uses Minecraft's Display Entity system
- Customizable block materials for all visual elements
- Adjustable line thickness and grid density
- Grid overlay for easier size calculation

**Multi-language Support**

The interface is available in multiple languages with automatic client language detection:
- Traditional Chinese (zh_tw)
- English (en_us)

**Player Settings**

Players can customize their own rendering preferences:
- Choose preferred block materials
- Adjust line thickness within server limits
- Control grid spacing and density
- Changes apply immediately

**Configuration System**
- Server-wide defaults in `config.yml`
- Per-player overrides in `player_config/*.yml`
- Hot reload without server restart
### Requirements

- Minecraft 1.21.x (Paper or Spigot)
- Java 21 or higher
- Required plugins:
  - [WorldEdit](https://dev.bukkit.org/projects/worldedit) (latest version)
  - [PacketEvents](https://github.com/retrooper/packetevents) 2.9.5+

### Installation

1. Make sure WorldEdit and PacketEvents are installed on your server
2. Download `WorldEditDisplay.jar`
3. Place it in your server's `plugins` folder
4. Restart the server
5. Edit `plugins/WorldEditDisplay/config.yml` if needed
6. Use `/wedisplayreload` to reload the configuration

### Commands

**Admin Commands**

`/wedisplayreload` - Reload plugin configuration
- Permission: `worldeditdisplay.reload` (default: op)

**Player Commands**

`/wedisplay set <renderer> <setting> <value>` - Configure personal rendering settings
- Example: `/wedisplay set cuboid line_material GOLD_BLOCK`
- Permission: `worldeditdisplay.use.settings`

`/wedisplay reset <renderer> [setting]` - Reset to server defaults
- Example: `/wedisplay reset cuboid` (reset all settings)
- Example: `/wedisplay reset cuboid line_material` (reset specific setting)

`/wedisplay show [renderer]` - Show current settings
- Example: `/wedisplay show` (show all)
- Example: `/wedisplay show cuboid` (show specific renderer)

`/wedisplay reloadplayer` - Reload your personal configuration file

`/wedisplay lang <language>` - Set interface language
- Example: `/wedisplay lang en_us`
- Supported: zh_tw, en_us

### Configuration

The main configuration file is `config.yml`:

```yaml
language:
  default: "zh_tw"              # Default language
  auto_detect: true             # Auto-detect player's client language
  allow_player_change: true     # Allow players to change language manually

renderer:
  cuboid:
    line_material: GOLD_BLOCK   # Material for edges
    line_thickness: 0.05        # Line thickness
    grid_spacing: 8             # Grid spacing
    # ... more settings

player_limits:
  thickness:
    min: 0.01                   # Minimum thickness
    max: 0.5                    # Maximum thickness players can set
  # ... more limits
```

Each renderer type has its own configuration section:
- **cuboid** - 13 configurable parameters
- **polygon** - 8 configurable parameters
- **ellipsoid** - 11 configurable parameters
- **cylinder** - 9 configurable parameters
- **polyhedron** - 5 configurable parameters

### How It Works

**Core Components**
- Packet listener intercepts WorldEdit CUI protocol messages
- Render engine creates Display Entities to visualize selections
- Configuration manager handles server-wide and per-player settings
- Language manager provides multi-language support

**Rendering Technology**

The plugin uses Minecraft's Display Entity system:
- Display entities render the visual elements
- More efficient than particle effects
- Works with vanilla clients (no mods required)

**Performance**
- Only updates when selections change
- Server admins can limit grid density and segment counts
- Each player's settings are independent

### Permissions

```yaml
worldeditdisplay.use:          # Use visualization features (default: true)
worldeditdisplay.use.settings: # Manage personal settings (default: true)
worldeditdisplay.reload:       # Reload configuration (default: op)
```

### Usage

**Basic Usage**
1. Use WorldEdit's `//wand` command to get the selection tool
2. Left/right click blocks to define your selection
3. The selection is automatically visualized (no extra steps needed)
4. Use `/wedisplay` commands to customize the appearance

**Customization Examples**
```bash
# Change cuboid edges to diamond blocks
/wedisplay set cuboid line_material DIAMOND_BLOCK

# Make lines thicker
/wedisplay set cuboid line_thickness 0.08

# Change grid spacing
/wedisplay set cuboid grid_spacing 16

# Reset all settings
/wedisplay reset cuboid
```

### Building

To build the plugin from source:

```bash
mvn clean package
```

The compiled jar will be in `target/worldeditdisplay-1.0.0.jar`

**Project Structure**
```
src/main/java/dev/twme/worldeditdisplay/
├── event/          # CUI event system
├── display/        # Render engine
│   └── renderer/   # Renderer implementations
├── config/         # Configuration management
├── lang/           # Language system
├── command/        # Command handlers
├── listener/       # Packet listeners
└── player/         # Player data management
```

### Contributing

Issues and pull requests are welcome.

### License

See [LICENSE](LICENSE) file for details.

### Credits

- **TWME-TW** - Developer

### Related Projects

- [WorldEdit](https://github.com/EngineHub/WorldEdit) - The core editing tool
- [WorldEditCUI](https://github.com/EngineHub/WorldEditCUI) - Client-side CUI mod (protocol reference)
- [PacketEvents](https://github.com/retrooper/packetevents) - Packet handling library
- [EntityLib](https://github.com/Tofaa2/EntityLib) - Entity manipulation library

---

## 繁體中文

### 關於

WorldEditDisplay 是一個 Minecraft 伺服器端插件，為 WorldEdit 增加視覺化選區渲染功能。它會攔截 WorldEdit 的 CUI（Client User Interface）協議封包，並在伺服器端使用 Display Entity 進行渲染。這表示玩家不需要安裝任何客戶端模組就能看到他們的 WorldEdit 選區。

### 功能

**選區類型**
- 長方體 - 方框選區，包含邊框線和網格顯示
- 多邊形 - 2D 多邊形邊界與垂直柱體
- 橢圓體 - 平滑的橢圓形選區
- 圓柱體 - 圓形選區，可調整高度
- 多面體 - 複雜的 3D 多面體結構

**視覺渲染**
- 使用 Minecraft 的 Display Entity
- 所有視覺元素都可以自訂方塊材質
- 可調整線條粗細和網格密度
- 網格覆蓋層方便計算大小

**多語言支援**

介面支援多種語言，並可自動偵測客戶端語言：
- 繁體中文 (zh_tw)
- 英文 (en_us)

**玩家設定**

玩家可以自訂渲染偏好：
- 選擇喜歡的方塊材質
- 在伺服器限制內調整線條粗細
- 控制網格間距和密度
- 變更後立即生效

**配置系統**
- 伺服器預設值設定在 `config.yml`
- 玩家個人覆寫設定在 `player_config/*.yml`
- 無需重啟伺服器即可熱重載

### 需求

- Minecraft 1.21.x（Paper 或 Spigot）
- Java 21 或更高版本
- 必要插件：
  - [WorldEdit](https://dev.bukkit.org/projects/worldedit)（最新版本）
  - [PacketEvents](https://github.com/retrooper/packetevents) 2.9.5+

### 安裝

1. 確保伺服器已安裝 WorldEdit 和 PacketEvents
2. 下載 `WorldEditDisplay.jar`
3. 放入伺服器的 `plugins` 資料夾
4. 重啟伺服器
5. 如需要可編輯 `plugins/WorldEditDisplay/config.yml`
6. 使用 `/wedisplayreload` 重新載入配置

### 指令

**管理員指令**

`/wedisplayreload` - 重新載入插件配置
- 權限：`worldeditdisplay.reload`（預設：op）

**玩家指令**

`/wedisplay set <renderer> <setting> <value>` - 設定個人渲染設定
- 範例：`/wedisplay set cuboid line_material GOLD_BLOCK`
- 權限：`worldeditdisplay.use.settings`

`/wedisplay reset <renderer> [setting]` - 重置為伺服器預設值
- 範例：`/wedisplay reset cuboid`（重置所有設定）
- 範例：`/wedisplay reset cuboid line_material`（重置特定設定）

`/wedisplay show [renderer]` - 顯示當前設定
- 範例：`/wedisplay show`（顯示全部）
- 範例：`/wedisplay show cuboid`（顯示特定渲染器）

`/wedisplay reloadplayer` - 重新載入你的個人配置檔案

`/wedisplay lang <language>` - 設定介面語言
- 範例：`/wedisplay lang en_us`
- 可用語言：zh_tw, en_us

### 配置

主要配置檔案是 `config.yml`：

```yaml
language:
  default: "zh_tw"              # 預設語言
  auto_detect: true             # 自動偵測玩家客戶端語言
  allow_player_change: true     # 允許玩家手動變更語言

renderer:
  cuboid:
    line_material: GOLD_BLOCK   # 邊緣材質
    line_thickness: 0.05        # 線條粗細
    grid_spacing: 8             # 網格間距
    # ... 更多設定

player_limits:
  thickness:
    min: 0.01                   # 最小粗細
    max: 0.5                    # 玩家可設定的最大粗細
  # ... 更多限制
```

每種渲染器類型都有自己的配置區段：
- **cuboid** - 13 個可配置參數
- **polygon** - 8 個可配置參數
- **ellipsoid** - 11 個可配置參數
- **cylinder** - 9 個可配置參數
- **polyhedron** - 5 個可配置參數

### 運作原理

**核心組件**
- 封包監聽器攔截 WorldEdit CUI 協議訊息
- 渲染引擎建立 Display Entity 來視覺化選區
- 配置管理器處理伺服器端和玩家端設定
- 語言管理器提供多語言支援

**渲染技術**

插件使用 Minecraft 的 Display Entity 系統：
- Display 實體渲染視覺元素
- 比粒子效果更有效率
- 原版客戶端即可使用（不需要模組）

**效能**
- 只在選區變更時更新
- 伺服器管理員可限制網格密度和分段數
- 每個玩家的設定互相獨立

### 權限

```yaml
worldeditdisplay.use:          # 使用視覺化功能（預設：true）
worldeditdisplay.use.settings: # 管理個人設定（預設：true）
worldeditdisplay.reload:       # 重新載入配置（預設：op）
```

### 使用方式

**基本使用**
1. 使用 WorldEdit 的 `//wand` 指令取得選取工具
2. 左鍵/右鍵點擊方塊來定義選區
3. 選區會自動視覺化（不需要額外步驟）
4. 使用 `/wedisplay` 指令來自訂外觀

**自訂範例**
```bash
# 將長方體邊緣改為鑽石塊
/wedisplay set cuboid line_material DIAMOND_BLOCK

# 增加線條粗細
/wedisplay set cuboid line_thickness 0.08

# 變更網格間距
/wedisplay set cuboid grid_spacing 16

# 重置所有設定
/wedisplay reset cuboid
```

### 建置

從原始碼建置插件：

```bash
mvn clean package
```

編譯後的 jar 檔會在 `target/worldeditdisplay-1.0.0.jar`

**專案結構**
```
src/main/java/dev/twme/worldeditdisplay/
├── event/          # CUI 事件系統
├── display/        # 渲染引擎
│   └── renderer/   # 渲染器實作
├── config/         # 配置管理
├── lang/           # 語言系統
├── command/        # 指令處理器
├── listener/       # 封包監聽器
└── player/         # 玩家資料管理
```

### 貢獻

歡迎提交 Issue 和 Pull Request。

### 授權

詳見 [LICENSE](LICENSE) 文件。

### 製作

- **TWME-TW** - 開發者

### 相關專案

- [WorldEdit](https://github.com/EngineHub/WorldEdit) - 核心編輯工具
- [WorldEditCUI](https://github.com/EngineHub/WorldEditCUI) - 客戶端 CUI 模組（協議參考）
- [PacketEvents](https://github.com/retrooper/packetevents) - 封包處理函式庫
- [EntityLib](https://github.com/Tofaa2/EntityLib) - 實體操作函式庫

---

<div align="center">

**Made with ❤️ by TWME-TW**

</div>
