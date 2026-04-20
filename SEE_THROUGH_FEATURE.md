# Per-Shape See Through 功能說明

## 功能概述

新增了針對每個形狀類型的 `see_through` 設定，玩家現在可以根據自己的喜好，為不同形狀獨立調整是否顯示透視（即是否能透過方塊看到形狀）。

## 配置

### 全局設定（在 `config.yml` 中）

現在每個形狀都有獨立的 `see_through` 設定：

```yaml
renderer:
  global:
    see_through: true              # 全局設定（保留供備用）
  
  cuboid:
    see_through: true              # Cuboid 形狀的 see_through 設定
    # ... 其他設定
  
  cylinder:
    see_through: true              # Cylinder 形狀的 see_through 設定
    # ... 其他設定
  
  ellipsoid:
    see_through: true              # Ellipsoid 形狀的 see_through 設定
    # ... 其他設定
  
  polygon:
    see_through: true              # Polygon 形狀的 see_through 設定
    # ... 其他設定
  
  polyhedron:
    see_through: true              # Polyhedron 形狀的 see_through 設定
    # ... 其他設定
```

### 玩家設定

玩家可以使用命令個性化調整每個形狀的 `see_through` 設定。

#### 查看設定

使用 `/wedisplay show <形狀類型>` 命令查看當前設定：

```
/wedisplay show cuboid
```

#### 修改設定

使用 `/wedisplay set <形狀類型> see_through <true|false>` 命令：

```
# 啟用 Cuboid 的透視
/wedisplay set cuboid see_through true

# 禁用 Cylinder 的透視
/wedisplay set cylinder see_through false

# 啟用 Ellipsoid 的透視
/wedisplay set ellipsoid see_through true

# 禁用 Polygon 的透視
/wedisplay set polygon see_through false

# 啟用 Polyhedron 的透視
/wedisplay set polyhedron see_through true
```

#### 重置設定

將特定形狀的 `see_through` 設定重置為伺服器默認值：

```
# 重置 Cuboid 的 see_through 設定
/wedisplay reset cuboid see_through

# 重置整個 Cuboid 形狀的所有設定
/wedisplay reset cuboid
```

## 實現細節

### 代碼變更

1. **config.yml**：為每個形狀新增了 `see_through: true` 設定

2. **RenderSettings.java**：
   - 為每個形狀新增了 `see_through` 字段
   - 新增了相應的 getter 方法（`isCuboidSeeThrough()`, `isCylinderSeeThrough()` 等）

3. **PlayerRenderSettings.java**：
   - 為每個形狀新增了 `Boolean see_through` 字段
   - 新增了相應的 getter 方法，支持玩家覆蓋伺服器設定
   - 在 `load()` 和 `clearFields()` 方法中添加了 see_through 的讀取和清空

4. **Renderer 類（CuboidRenderer, CylinderRenderer 等）**：
   - 在每個 Renderer 類中覆蓋了 `isSeeThrough()` 方法
   - 現在使用各自形狀的 see_through 設定，而不是全局設定

5. **PlayerSettingsCommand.java**：
   - 更新了 `parseValue()` 方法以支持解析 `see_through` 設定為布爾值

## 使用示例

假設你想：
- 讓 Cuboid 選擇區域透視顯示
- 讓 Cylinder 選擇區域不透視顯示

可以執行：

```
/wedisplay set cuboid see_through true
/wedisplay set cylinder see_through false
```

之後，Cuboid 形狀會透過方塊顯示，而 Cylinder 形狀不會。

## 向後兼容性

此更新完全向後兼容：
- 如果玩家沒有設定任何個性化的 `see_through` 值，會使用伺服器配置中的對應形狀值
- 全局 `see_through` 設定保留，但不再被使用（可在未來移除）
