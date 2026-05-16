# WorldEditDisplay 分享/監控/標籤功能設計方案

## 一、權限節點設計

```
worldeditdisplay.use                          # 已有，預設 true
worldeditdisplay.use.settings                 # 已有，預設 true
worldeditdisplay.use.share                    # 已有，預設 true  ← 一般分享功能總開關
worldeditdisplay.render.auto-enable           # 已有，預設 true
worldeditdisplay.reload                       # 已有，預設 op

# 新增（share 子指令）
worldeditdisplay.use.share.invite             # 新增，/wedisplay share <player>，預設 true
worldeditdisplay.use.share.accept             # 新增，/wedisplay share accept <player>，預設 true
worldeditdisplay.use.share.revoke             # 新增，/wedisplay share revoke <player>，預設 true
worldeditdisplay.use.share.unwatch            # 新增，/wedisplay share unwatch <player>，預設 true
worldeditdisplay.use.share.list               # 新增，/wedisplay share list，預設 true

# 新增（view 子指令）
worldeditdisplay.use.view                     # 新增，/wedisplay view（toggle），預設 op
worldeditdisplay.use.view.defaultenable       # 新增，預設 op
worldeditdisplay.use.view.hide                # 新增，/wedisplay view hide/hideall/unhide，預設 op
worldeditdisplay.use.view.list                # 新增，/wedisplay view list，預設 true
worldeditdisplay.use.view.label               # 新增，/wedisplay view label，預設 true
worldeditdisplay.use.view.label.defaultenable # 新增，預設 true
```

- `use.share`：一般分享功能總開關，關閉時所有 share 子指令均不可用
- `use.share.invite`：可傳送邀請給其他玩家查看自己的選區
- `use.share.accept`：可接受其他玩家的邀請
- `use.share.revoke`：可撤銷「你分享給某玩家」的觀看資格（你是 sharer）
- `use.share.unwatch`：可主動停止觀看某玩家的選區（你是 viewer）
- `use.share.list`：可使用 `/wedisplay share list` 查看分享關係列表
- `use.view`：可使用 `/wedisplay view` 切換 viewall 模式，且不會被對方發現
- `use.view.defaultenable`：進入伺服器時預設開啟 viewall 模式
- `use.view.hide`：可使用 `/wedisplay view hide/hideall/unhide` 排除特定玩家
- `use.view.list`：可使用 `/wedisplay view list` 查看 viewall 監控狀態
- `use.view.label`：可使用 `/wedisplay view label` 切換名稱標籤顯示，無論來源是 share 或 viewall
- `use.view.label.defaultenable`：進入伺服器時預設開啟名稱標籤顯示

## 二、指令設計

### 分享指令

```
/wedisplay share <player>           # 邀請其他玩家查看自己的選區（需 use.share.invite）
/wedisplay share accept <player>    # 接受某位玩家的邀請（需 use.share.accept）
/wedisplay share revoke <player>    # 撤銷你分享給 player 的觀看資格，即踢出觀看（需 use.share.revoke）
/wedisplay share unwatch <player>   # 主動停止觀看 player 的選區，對方不受影響（需 use.share.unwatch）
/wedisplay share list [page]        # 顯示目前分享關係列表（需 use.share.list）
```

> **`revoke` vs `unwatch` 語意區別**
> - `revoke`：你是 sharer，撤銷某人的觀看資格；對方會收到「分享已被撤銷」通知。
> - `unwatch`：你是 viewer，主動離開觀看；sharer 不會收到通知。

### 觀看指令（view 子指令）

```
/wedisplay view                   # Toggle viewall 模式開/關（需 use.view）
/wedisplay view hide <player>     # 將某玩家從 viewall 清單中排除（需 use.view.hide）
/wedisplay view hideall           # 將所有目前在線玩家加入 hide 清單（需 use.view.hide）
/wedisplay view unhide <player>   # 取消排除（需 use.view.hide）
/wedisplay view label             # 切換名稱標籤顯示（需 use.view.label）
/wedisplay view list [page]       # 顯示 viewall 監控狀態（需 use.view.list）
```

### view 指令行為
- 需要 `worldeditdisplay.use.view` 權限才能使用
- 登入時預設為 **關閉**，除非玩家擁有 `worldeditdisplay.use.view.defaultenable`
- 若擁有 `defaultenable`，登入後 viewall 預設為 **開啟**，可再用 `/wedisplay view` 切換
- 關閉時：即使擁有 view 權限，也只走正常 share 流程（只看被分享的人）
- 開啟時：靜默監控所有在線玩家選區（排除 hide 清單中的玩家）
- toggle 狀態存在玩家的 session 狀態中，不持久化，重新登入恢復預設值

### 邀請請求逾時機制
- 邀請發出後，pendingRequest 預設存活 **30 秒**（可於 config 調整）
- 逾時後自動失效，inviter 與 invitee 雙方皆收到「邀請已過期」訊息
- 若 invitee 在邀請過期前離線，視同逾時處理
- 在同一邀請仍 pending 期間，inviter 不可對同一玩家重複發送邀請

### 邊界情況處理
- **邀請自己**：拒絕，回傳錯誤訊息
- **重複邀請（已 active）**：提示「你已與該玩家建立分享關係」
- **重複邀請（已 pending）**：提示「邀請已送出，等待對方回應（剩餘秒數）」
- **對離線玩家使用 hide**：允許加入 hide 清單，下次上線時即生效（session 範圍內）
- **對自己使用 hide**：拒絕

### view label 指令行為
- 需要 `worldeditdisplay.use.view.label` 權限才能使用
- 登入時預設為 **關閉**，除非玩家擁有 `worldeditdisplay.use.view.label.defaultenable`
- 若擁有 `defaultenable`，登入後標籤顯示預設為 **開啟**，可再用 `/wedisplay view label` 切換
- toggle 狀態存在玩家的 session 狀態中，不持久化
- 名稱標籤顯示目前觀看中的 sharer 名稱，並沿用該 sharer 對應的共享顏色
- 標籤開關同時影響 share 與 viewall 兩種觀看模式

### list 顯示格式

**`/wedisplay share list`（分享關係）**
- §a[Online] §fPlayerA    ← 在線
- §7[Offline] §fPlayerB   ← 離線

**`/wedisplay view list`（viewall 監控）**
- §a[Online] §fPlayerC    ← viewall 中的在線玩家
- hidden: PlayerD, PlayerE  ← 目前 session 排除的玩家

## 三、資料結構與狀態

- `ShareManager` 只管理需要持久化的分享關係：
  - `pendingRequests`（含建立時間戳，用於逾時判斷）
  - `activeShares`
  - 對應的 load/save 邏輯
- 持久化採**定期儲存**策略：每 **5 分鐘**自動寫入一次，伺服器正常關閉時也強制寫入
  - 格式：JSON（或 YAML），儲存於插件資料夾
  - 僅儲存 `activeShares`；`pendingRequests` 為記憶體狀態，重啟後清空
- 觀看狀態由玩家的 session 狀態管理，不放進 `ShareManager`：
  - `viewAllEnabled`
  - `viewAllHidden`（僅對目前 session 有效）
  - `showLabels`
- 選區顏色分配已有實作（依 sharer UUID 或顏色池決定），`ShareManager` 無需另行管理
- 如果未來 session 狀態變多，可以再抽出一個獨立的 `ViewState` / `MonitorState` 管理器
- viewall hide 清單不持久化，玩家重新登入或伺服器重啟後重置

## 四、渲染與標籤

- `RenderManager` 新增：
  - `resolveVisibleSharers(Player viewer, UUID viewerId)`
    - 只有在玩家擁有 `use.view` 權限且 session 中 viewAllEnabled 為 true 時，回傳所有在線且未被 hide 的玩家
    - 否則回傳原本的 getActiveSharers
  - `sharedLabelShapes: Map<UUID, Map<UUID, Shape>>` 管理標籤 entity
- 標籤顯示：
  - 位置：選區 bounding box 中心
  - 文字：sharer 的名稱，顏色與選區顏色一致
  - 僅當 sharer 在線時顯示
  - 標籤是否顯示由權限節點與 PlayerData toggle 控制，不需要額外 config 開關

### 距離與世界篩選（viewall 模式）
- 每次判斷是否對 viewer 渲染 sharer 的選區前，先做兩道篩選：
  1. **世界篩選**：viewer 與 sharer 必須在同一世界，否則跳過
  2. **距離篩選**：根據選區 bounding box 的對角線長度（diameter）動態計算載入門檻：
     ```
     effectiveDistance = max(selectionDiameter * sizeMultiplier, minLoadDistance)
     ```
     若 viewer 到選區中心的距離 > `effectiveDistance`，跳過渲染
- Config 可控參數：
  - `viewall.distance-based-loading.enabled`（預設 true）
  - `viewall.distance-based-loading.min-distance`（預設 64，單位 block）
  - `viewall.distance-based-loading.size-multiplier`（預設 2.0）
- 上述篩選僅影響 viewall 模式；share 模式（主動接受的分享）不受距離限制

## 五、notifyViewersOfSharer 廣播邏輯

- 除了原本 activeViewers，還需通知所有 viewall 已啟用且未 hide 該 sharer 的在線玩家
- 建議在 memory 中維護一個目前已啟用 viewall 的 viewer 集合，避免每次更新都掃描全服
- 當被觀看的玩家登出時，應自動清除該玩家的所有選取區顯示，包含 share 與 viewall 觀看者的畫面
- 登出只會移除渲染結果，不會自動解除既有分享關係；玩家重新登入後若分享關係仍存在，選取區可再次顯示

## 六、list 指令顯示

### `/wedisplay share list [page]`（分享關係）
- 顯示你目前**主動分享給他人**及**被他人分享**的關係列表
- 標註對方是否在線；若某玩家離線，名稱仍保留但不顯示選取區
- 需要 `worldeditdisplay.use.share.list` 權限

### `/wedisplay view list [page]`（viewall 監控）
- 顯示 viewall 模式下目前正在監控的所有在線玩家
- 標註目前 session hide 清單中的玩家
- 需要 `worldeditdisplay.use.view.list` 權限（隱含需要 `use.view`）

### 分頁設計（兩個 list 指令共用）
- 每頁顯示固定筆數（預設 8 筆），可於 config 調整
- 頁尾顯示 MiniMessage 可點擊翻頁列（以 share list 為例）：
  ```
  <gray>--- <white>第 1 頁 / 共 3 頁</white> ---
  [<click:run_command:'/wedisplay share list 0'><gray>◀ 上一頁</gray></click>]  
  [<click:run_command:'/wedisplay share list 2'><gray>下一頁 ▶</gray></click>]
  ```
- 第一頁時「上一頁」按鈕為灰色不可點擊；最後一頁時「下一頁」同理
- page 參數為 0-based index，預設第 0 頁

## 七、其他

- `use.view.hide` 清單為 session-only，不持久化
- `use.view.defaultenable` 只影響登入預設狀態，不影響玩家手動 toggle
- `use.view.label.defaultenable` 只影響登入預設狀態，不影響玩家手動 toggle
- 標籤以 `use.view.label` 權限控制是否可用，並由 session 狀態決定當前是否顯示
- 所有權限皆可用 LuckPerms 等插件細緻分配

---

本設計方案已與 2026/5/14 討論確認。