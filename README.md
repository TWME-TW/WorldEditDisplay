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
- Uses Minecraft's Display Entity (TextDisplay) system
- Customizable hex colors (#RRGGBBAA) for all visual elements
- Adjustable line thickness and grid density
- Translucent face filling for Cuboid, Polygon, and Polyhedron regions
- Grid overlay for easier size calculation

**Multi-language Support**

The interface is available in multiple languages with automatic client language detection:
- Traditional Chinese (zh_tw)
- English (en_us)

**Player Settings**

Players can customize their own rendering preferences:
- Choose preferred hex colors for elements
- Enable or disable face filling
- Adjust line thickness within server limits
- Control grid spacing and density
- Changes apply immediately

**Selection Sharing**
- Share your WorldEdit selection with other players in real-time
- Invite, accept, revoke, and unwatch shares via `/wedisplay share`
- Each shared selection is displayed in a unique auto-generated color
- Share relationships persist across server restarts

**View All Mode** *(op-only by default)*
- Toggle monitoring of all online players' selections via `/wedisplay view`
- Hide specific players from your view, toggle name labels, and list monitored players

**Configuration System**
- Server-wide defaults in `config.yml`
- Per-player overrides in `player_config/*.yml`
- Hot reload without server restart

### Requirements

- Paper 1.19.4 ~ 26.2; Folia / Spigot 1.20 ~ 1.21.x
- Java 17 or higher (subject to the Minecraft server version's own Java requirement)
- Required plugins:
  - [PacketEvents](https://github.com/retrooper/packetevents) 2.11.1+

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
- Example: `/wedisplay set cuboid edge_color #FFD700FF`
- Permission: `worldeditdisplay.use.settings`

`/wedisplay reset <renderer> [setting]` - Reset to server defaults
- Example: `/wedisplay reset cuboid` (reset all settings)
- Example: `/wedisplay reset cuboid edge_color` (reset specific setting)

`/wedisplay show [renderer]` - Show current settings
- Example: `/wedisplay show` (show all)
- Example: `/wedisplay show cuboid` (show specific renderer)

`/wedisplay toggle` - Toggle rendering on/off
- Quick command to enable or disable selection visualization
- Works independently for each player

`/wedisplay reloadplayer` - Reload your personal configuration file

`/wedisplay lang <language>` - Set interface language
- Example: `/wedisplay lang en_us`
- Supported: zh_tw, en_us

`/wedisplay share invite <player>` - Invite a player to view your selection
- Permission: `worldeditdisplay.use.share.invite`

`/wedisplay share accept <player>` - Accept a pending share invitation
- Permission: `worldeditdisplay.use.share.accept`

`/wedisplay share revoke <player>` - Revoke a share you previously granted
- Permission: `worldeditdisplay.use.share.revoke`

`/wedisplay share unwatch <player>` - Stop watching another player's selection
- Permission: `worldeditdisplay.use.share.unwatch`

`/wedisplay share list [page]` - List current share relationships
- Permission: `worldeditdisplay.use.share.list`

**View All Sub-commands** *(op-only by default)*

`/wedisplay view` - Toggle view-all mode (see all online players' selections)
- Permission: `worldeditdisplay.use.view`

`/wedisplay view hide <player>` - Exclude a player from your view-all mode
- Permission: `worldeditdisplay.use.view.hide`

`/wedisplay view hideall` - Exclude all online players from view-all mode
- Permission: `worldeditdisplay.use.view.hide`

`/wedisplay view unhide <player>` - Remove exclusion for a player
- Permission: `worldeditdisplay.use.view.hide`

`/wedisplay view label` - Toggle name label display for watched selections
- Permission: `worldeditdisplay.use.view.label`

`/wedisplay view list [page]` - Show view-all monitoring status
- Permission: `worldeditdisplay.use.view.list`

### Configuration

The main configuration file is `config.yml`:

```yaml
language:
  default: "en-us"              # Default language
  auto_detect: true             # Auto-detect player's client language
  allow_player_change: true     # Allow players to change language manually

renderer:
  cuboid:
    edge_color: "#FFD700FF"     # Color for edges in #RRGGBBAA
    edge_thickness: 0.05        # Line thickness
    grid_spacing: 8             # Grid spacing
    fill_enabled: true          # Enable face filling
    fill_color: "#FF000040"     # Face fill color
    # ... more settings

player_limits:
  thickness:
    min: 0.01                   # Minimum thickness
    max: 0.5                    # Maximum thickness players can set
  # ... more limits
```

Each renderer type has its own configuration section:
- **cuboid**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/5d72f189305f54de130e3cd1918a82f06dbbee53.png" />
- **polygon**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/1f5ff5afe4a2ec302268a2d5ac65e80fadd02c8e.png" />
- **ellipsoid**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/2641d5fc53d19b7bbeb8e3ac82266648c60c1bc2.png" />
- **cylinder**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/8dad5d894ce5b09282311ea3fd61227c8603672b.png" />
- **polyhedron**
<img width="2879" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/b902289ee7020086b93f6591fd3f8f30d29bf156.png" />

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
worldeditdisplay.use:                    # Use visualization features (default: true)
worldeditdisplay.use.settings:           # Manage personal rendering settings (default: true)
worldeditdisplay.use.share:              # General share feature toggle (default: true)
worldeditdisplay.use.share.invite:       # Send share invitations (default: true)
worldeditdisplay.use.share.accept:       # Accept share invitations (default: true)
worldeditdisplay.use.share.revoke:       # Revoke granted shares (default: true)
worldeditdisplay.use.share.unwatch:      # Stop watching a shared selection (default: true)
worldeditdisplay.use.share.list:         # List share relationships (default: true)
worldeditdisplay.use.view:               # Toggle view-all mode (default: op)
worldeditdisplay.use.view.defaultenable: # Auto-enable view-all on join (default: op)
worldeditdisplay.use.view.hide:          # Hide players from view-all (default: op)
worldeditdisplay.use.view.list:          # List view-all monitoring status (default: true)
worldeditdisplay.use.view.label:         # Toggle name label display (default: true)
worldeditdisplay.use.view.label.defaultenable: # Auto-enable labels on join (default: true)
worldeditdisplay.reload:                 # Reload configuration (default: op)
worldeditdisplay.render.auto-enable:     # Automatically enable rendering on join (default: true)
```

**About Auto-Enable Permission**

The `worldeditdisplay.render.auto-enable` permission controls whether rendering is automatically enabled when a player joins the server:
- **With permission**: Rendering is enabled by default when logging in
- **Without permission**: Players must manually use `/wedisplay toggle` to enable rendering

This allows server admins to control which player groups have rendering enabled by default.

### Usage

**Basic Usage**
1. Use WorldEdit's `//wand` command to get the selection tool
2. Left/right click blocks to define your selection
3. The selection is automatically visualized (no extra steps needed)
4. Use `/wedisplay` commands to customize the appearance

**Customization Examples**
```bash
# Change cuboid edges to solid diamond blue
/wedisplay set cuboid edge_color #55FF55FF

# Make lines thicker
/wedisplay set cuboid edge_thickness 0.08

# Change grid spacing
/wedisplay set cuboid height_grid_division 2

# Enable face filling with half-transparent red
/wedisplay set cuboid fill_enabled true
/wedisplay set cuboid fill_color #FF000080

# Toggle rendering on/off
/wedisplay toggle

# Reset all settings
/wedisplay reset cuboid
```

### Building

To build the plugin from source:

```bash
mvn clean package
```

The compiled jar will be in `target/WorldEditDisplay-version-platform.jar`

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
- [TextDisplayShapes](https://github.com/TWME-TW/TextDisplayShapes) - Shape rendering library used by the plugin

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
- 使用 Minecraft 的 Display Entity (TextDisplay) 系統
- 所有視覺元素都可以自訂 HEX 顏色 (#RRGGBBAA)
- 支援長方體、多邊形與多面體的半透明表面填充
- 可調整線條粗細和網格密度
- 網格覆蓋層方便計算大小

**多語言支援**

介面支援多種語言，並可自動偵測客戶端語言：
- 繁體中文 (zh_tw)
- 英文 (en_us)

**玩家設定**

玩家可以自訂渲染偏好：
- 選擇喜歡的 HEX 顏色
- 開啟或關閉半透明表面填充
- 在伺服器限制內調整線條粗細
- 控制網格間距和密度
- 變更後立即生效

**選區分享**
- 即時與其他玩家分享你的 WorldEdit 選區
- 透過 `/wedisplay share` 進行邀請、接受、撤銷及停止觀看等操作
- 每個共享選區會自動分配一個獨特的顯示顏色
- 分享關係在伺服器重啟後仍會保持

**全覽模式** *（預設僅 op）*
- 透過 `/wedisplay view` 切換監看所有線上玩家選區的模式
- 可隱藏特定玩家的選區、切換名稱標籤，以及查看監看清單

**配置系統**
- 伺服器預設值設定在 `config.yml`
- 玩家個人覆寫設定在 `player_config/*.yml`
- 無需重啟伺服器即可熱重載

### 需求

- Paper 1.19.4 ~ 26.2；Folia / Spigot 1.20 ~ 1.21.x
- Java 17 或更高版本（仍須符合 Minecraft 伺服器版本本身的 Java 需求）
- 必要插件：
  - [PacketEvents](https://github.com/retrooper/packetevents) 2.11.1+

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
- 範例：`/wedisplay set cuboid edge_color #FFD700FF`
- 權限：`worldeditdisplay.use.settings`

`/wedisplay reset <renderer> [setting]` - 重置為伺服器預設值
- 範例：`/wedisplay reset cuboid`（重置所有設定）
- 範例：`/wedisplay reset cuboid edge_color`（重置特定設定）

`/wedisplay show [renderer]` - 顯示當前設定
- 範例：`/wedisplay show`（顯示全部）
- 範例：`/wedisplay show cuboid`（顯示特定渲染器）

`/wedisplay toggle` - 切換渲染開關
- 快速指令來啟用或停用選區視覺化
- 每個玩家獨立運作

`/wedisplay reloadplayer` - 重新載入你的個人配置檔案

`/wedisplay lang <language>` - 設定介面語言
- 範例：`/wedisplay lang en_us`
- 可用語言：zh_tw, en_us

`/wedisplay share invite <player>` - 邀請玩家觀看你的選區
- 權限：`worldeditdisplay.use.share.invite`

`/wedisplay share accept <player>` - 接受待處理的分享邀請
- 權限：`worldeditdisplay.use.share.accept`

`/wedisplay share revoke <player>` - 撤銷你已授予的分享
- 權限：`worldeditdisplay.use.share.revoke`

`/wedisplay share unwatch <player>` - 停止觀看另一位玩家的選區
- 權限：`worldeditdisplay.use.share.unwatch`

`/wedisplay share list [page]` - 列出目前的分享關係
- 權限：`worldeditdisplay.use.share.list`

**全覽子指令** *（預設僅 op）*

`/wedisplay view` - 切換全覽模式（查看所有線上玩家的選區）
- 權限：`worldeditdisplay.use.view`

`/wedisplay view hide <player>` - 從全覽模式中排除特定玩家
- 權限：`worldeditdisplay.use.view.hide`

`/wedisplay view hideall` - 從全覽模式中排除所有線上玩家
- 權限：`worldeditdisplay.use.view.hide`

`/wedisplay view unhide <player>` - 移除玩家的排除設定
- 權限：`worldeditdisplay.use.view.hide`

`/wedisplay view label` - 切換被觀看選區的名稱標籤顯示
- 權限：`worldeditdisplay.use.view.label`

`/wedisplay view list [page]` - 顯示全覽監看狀態
- 權限：`worldeditdisplay.use.view.list`

### 配置

主要配置檔案是 `config.yml`：

```yaml
language:
  default: "zh_tw"              # 預設語言
  auto_detect: true             # 自動偵測玩家客戶端語言
  allow_player_change: true     # 允許玩家手動變更語言

renderer:
  cuboid:
    edge_color: "#FFD700FF"     # 邊緣顏色 (#RRGGBBAA)
    edge_thickness: 0.05        # 線條粗細
    grid_spacing: 8             # 網格間距
    fill_enabled: true          # 是否啟用表面填充
    fill_color: "#FF000040"     # 表面填充顏色
    # ... 更多設定

player_limits:
  thickness:
    min: 0.01                   # 最小粗細
    max: 0.5                    # 玩家可設定的最大粗細
  # ... 更多限制
```

每種渲染器類型都有自己的配置區段：
- **cuboid**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/5d72f189305f54de130e3cd1918a82f06dbbee53.png" />
- **polygon**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/1f5ff5afe4a2ec302268a2d5ac65e80fadd02c8e.png" />
- **ellipsoid**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/2641d5fc53d19b7bbeb8e3ac82266648c60c1bc2.png" />
- **cylinder**
<img width="2880" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/8dad5d894ce5b09282311ea3fd61227c8603672b.png" />
- **polyhedron**
<img width="2879" height="1750" alt="image" src="https://cdn.modrinth.com/data/eVvfozdY/images/b902289ee7020086b93f6591fd3f8f30d29bf156.png" />

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
worldeditdisplay.use:                    # 使用視覺化功能（預設：true）
worldeditdisplay.use.settings:           # 管理個人渲染設定（預設：true）
worldeditdisplay.use.share:              # 分享功能總開關（預設：true）
worldeditdisplay.use.share.invite:       # 發送分享邀請（預設：true）
worldeditdisplay.use.share.accept:       # 接受分享邀請（預設：true）
worldeditdisplay.use.share.revoke:       # 撤銷已授予的分享（預設：true）
worldeditdisplay.use.share.unwatch:      # 停止觀看共享選區（預設：true）
worldeditdisplay.use.share.list:         # 列出分享關係（預設：true）
worldeditdisplay.use.view:               # 切換全覽模式（預設：op）
worldeditdisplay.use.view.defaultenable: # 登入時自動啟用全覽模式（預設：op）
worldeditdisplay.use.view.hide:          # 從全覽模式隱藏玩家（預設：op）
worldeditdisplay.use.view.list:          # 列出全覽監看狀態（預設：true）
worldeditdisplay.use.view.label:         # 切換名稱標籤顯示（預設：true）
worldeditdisplay.use.view.label.defaultenable: # 登入時自動啟用名稱標籤（預設：true）
worldeditdisplay.reload:                 # 重新載入配置（預設：op）
worldeditdisplay.render.auto-enable:     # 登入時自動啟用渲染（預設：true）
```

**關於自動啟用權限**

`worldeditdisplay.render.auto-enable` 權限控制玩家加入伺服器時是否自動啟用渲染：
- **擁有權限**：登入時預設啟用渲染
- **沒有權限**：玩家必須手動使用 `/wedisplay toggle` 來啟用渲染

這讓伺服器管理員可以控制哪些玩家群組預設啟用渲染。

### 使用方式

**基本使用**
1. 使用 WorldEdit 的 `//wand` 指令取得選取工具
2. 左鍵/右鍵點擊方塊來定義選區
3. 選區會自動視覺化（不需要額外步驟）
4. 使用 `/wedisplay` 指令來自訂外觀

**自訂範例**
```bash
# 將長方體邊緣改為不透明的鑽石藍色
/wedisplay set cuboid edge_color #55FF55FF

# 增加線條粗細
/wedisplay set cuboid edge_thickness 0.08

# 變更高度網格分割
/wedisplay set cuboid height_grid_division 2

# 啟用半透明的紅色表面填充
/wedisplay set cuboid fill_enabled true
/wedisplay set cuboid fill_color #FF000080

# 切換渲染開關
/wedisplay toggle

# 重置所有設定
/wedisplay reset cuboid
```

### 建置

從原始碼建置插件：

```bash
mvn clean package
```

編譯後的 jar 檔會在 `target/WorldEditDisplay-version-platform.jar`

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
- [TextDisplayShapes](https://github.com/TWME-TW/TextDisplayShapes) - 此插件使用的形狀渲染函式庫

---

<div align="center">

**Made with ❤️ by TWME-TW**

</div>
