# 家人桌面安装与检查更新

包名 `studio.muzi.tv`，minSdk 23（卧室 Android 6 可用）。开机 `BootReceiver` 与 HOME 意图保持原样。首页只有四格，从左到右：

1. **蜂蜜** — 打开 FongMi `com.fongmi.android.tv`（`openFongMi`）。未安装时提示「尚未安装蜂蜜（FongMi）」。
2. **YouTube** — 依次尝试 `com.google.android.youtube.tv`、`com.google.android.youtube`，再试 SmartTube，再按包名里带 `youtube` 的已装应用。都没有则提示「尚未安装 YouTube」。
3. **全部应用** — 已安装应用列表。**第一项固定为「安装应用」**（文件管理器；没有则打开系统选择安装包）。安装应用不是坞上的第五格。
4. **设置** — 系统设置（`android.settings.SETTINGS`，失败再试 `com.xiaomi.mitv.settings`）。

不在坞上：腾讯视频、哔哩哔哩、LibreTV、电视直播、外接设备、直播频道。腾讯 / B 站内容走蜂蜜。

## 装哪个 APK

签名发布包：

- 文件名：`muzi-studio-1.1.4.apk`
- versionCode **105** / versionName **1.1.4**
- SHA-256：`176c1421f90069dc09082a3578230d28f4cc44a0148aa745f3a72c9caf3eb278`（与 `deploy/www/version.json` 的 `studio.sha256` 相同）
- 签名：v1 + v2（Android 6 认 v1）
- 构建产物：`android/app/build/outputs/apk/release/app-release.apk`
- 同步暂存：`deploy/www/apk/muzi-studio-1.1.4.apk`（仓库忽略 `*.apk`，以构建机或 PR 附件为准）

签名密钥是本次新生成的，**不入库**（`android/signing/muzi-release.jks` 与仓库根 `signing.env` 已在 `.gitignore`）。以后自动更新必须用同一把钥匙。钥匙丢失就只能卸载重装。

## 签名不一致时先卸载

若电视上已有旧的 `studio.muzi.tv`（公网 1.1.2 / versionCode 103，或其它钥匙签的包），系统会拒绝覆盖安装。先卸载旧包，再装本 APK。卸载会清掉应用内的 LibreTV 连接配置。

```text
设置 → 应用 → muzi 主卧 / studio.muzi.tv → 卸载
再安装 muzi-studio-1.1.4.apk
```

装好后若要当桌面：按主页键，选「muzi 主卧」并设为始终。Android 6 小米不能禁用系统桌面，沿用现有开机拉起，不要停用 `com.mitv.tvhome`。

## 开机后核对四格

冷启动应直接是星空主页，底坞只有四格，焦点默认在第一格。

| 操作 | 期望 |
|------|------|
| 看底坞从左到右 | 蜂蜜、YouTube、全部应用、设置。没有腾讯、哔哩、直播、HDMI |
| 确认「蜂蜜」 | 打开 FongMi。未安装只出现提示，不闪退 |
| 确认「YouTube」 | 打开已装的 YouTube（或 SmartTube）。未安装提示「尚未安装 YouTube」 |
| 确认「全部应用」 | 列表第一项是「安装应用」，下面才是已装应用 |
| 确认「安装应用」 | 打开文件管理器；没有文件管理器时进入选择安装包 |
| 确认「设置」 | 打开系统设置，不是应用内的 LibreTV / 哔哩菜单 |
| 左右键 | 只在这四格之间移动 |
| 返回 | 从全部应用回到这四格主页 |

## 自动更新（沿用 zhishixingqiu.top，不用 muzi.studio）

| 项 | 值 |
|----|----|
| 清单 | https://zhishixingqiu.top/tv/version.json |
| 使用字段 | `studio.package` / `studio.versionCode` / `studio.versionName` / `studio.url` / `studio.sha256` |
| 包名 | `studio.muzi.tv`（与清单 `studio` 键相同，没有另开 applicationId） |
| APK | https://zhishixingqiu.top/tv/apk/muzi-studio-1.1.4.apk |
| 服务器目录 | `/var/www/muzi-tv/`（nginx `location /tv/`，见 `deploy/server/muzi-tv.locations`） |
| Mac 对照 | `/Users/xiaolongmu/Projects/muzi-tv/public-tv/www/version.json` 与 `public-tv/www/apk/` |

启动时 `Updater.check` 拉清单（每个进程一次）。`studio.versionCode` 大于已安装 versionCode 时弹出「木子电视 有新版本」，按「更新」下载并用 SHA-256 校验后调系统安装器。Android 6 用 `PackageInfo.versionCode` 和 `file://`，不调用 API 28 的 `getLongVersionCode`。已是最新则不弹窗。检查失败只写 logcat 标签 `MuziUpdater`，不打扰家人。

默认地址写在 `Updater.VERSION_URL`，构建再写入 `BuildConfig.UPDATE_URL`（默认同一个公网地址）。要换源而不改代码：在应用私有目录 `files/config.json` 增加 `"updateUrl":"https://…/version.json"`（须 http 或 https）。构建期覆盖：`-PmuziUpdateUrl=…` 或 `-PmuziBaseUrl=https://主机/tv`。

`/tv/` 列目录会 403。用具体 URL 检查：

```bash
curl -fsS https://zhishixingqiu.top/tv/version.json
curl -fsSI https://zhishixingqiu.top/tv/apk/muzi-studio-1.1.4.apk
```

### 怎么确认会弹出更新框

弹窗条件：清单里的 `studio.versionCode` **大于** 电视上已装的 versionCode。刚装上 105，且清单也是 105 时，启动**不会**弹窗，这是正常的。

1. 电视上还留着旧包（公网 1.1.2，versionCode 103），且清单已是 105：冷启动数秒内应出现「木子电视 有新版本」，正文版本 `1.1.4`。进程还在时返回主页不会再请求；要再看就划掉应用再开。
2. 旧包若是另一把签名，点「更新」能下载，系统安装器会因签名不一致拒绝覆盖。这时先卸载，再装 1.1.4。弹窗本身已经说明检查成功。
3. 装上 105 且清单也是 105：再冷启动，不应再弹「木子电视 有新版本」。
4. 要在**同一把新钥匙**上走完「下载并安装」：先装一个 versionCode 更小、用 `muzi-release.jks` 签的包，清单保持 105，冷启动后点「更新」。不要把清单 versionCode 长期写成大于 APK 真实 versionCode，否则每次启动都会再提示。

本云端环境没有 SSH / 部署密钥，**不能**把 APK 写到 zhishixingqiu.top。`deploy/sync-tv.sh` 会在本地改好 `deploy/www/version.json` 的 `studio` 段（不动 `fongmi`），并把 APK 拷到 `deploy/www/apk/`。请把这两个文件交到正在发布的机器：

- 本仓库：`deploy/www/version.json`、`deploy/www/apk/muzi-studio-1.1.4.apk`
- Mac：`/Users/xiaolongmu/Projects/muzi-tv/public-tv/www/` 下同名路径
- 服务器：`/var/www/muzi-tv/version.json` 与 `/var/www/muzi-tv/apk/muzi-studio-1.1.4.apk`

已有 SSH 时：

```bash
SSH_TARGET=user@zhishixingqiu-host deploy/sync-tv.sh android/app/build/outputs/apk/release/app-release.apk
```

上传后用上面的 curl 确认 `studio.versionCode` 为 105、`url` 指向 `muzi-studio-1.1.4.apk`、`sha256` 与 APK 一致，再按上一节在电视上看弹窗。
