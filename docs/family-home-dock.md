# 家人桌面底坞（Gavin 确认，2026-10-22）

来源：CleanUR / CleanYourTV 调研定稿 §0。实现在 `studio.muzi.tv` 的 `MainActivity.Home`，不另开应用。

## 底坞四格（固定顺序，不可改首格）

| 槽位 | 显示名 | 行为 | 备注 |
|------|--------|------|------|
| **1（永远第一）** | **蜂蜜** | 打开 FongMi（`com.fongmi.android.tv`） | 首格锁定；腾讯视频 / B站等资源**只通过蜂蜜**访问 |
| 2 | **YouTube** | 打开系统已装 YouTube（或 SmartTube 若已安装） | Gavin **确认要单独占格** |
| 3 | **全部应用** | 打开本应用「全部应用」列表；列表内含「安装应用」入口 | 其它应用不占首页格，一律收进这里 |
| 4 | **设置** | 打开系统设置 | 避免家人进小米广告墙 |

## 明确不展示（首页坞 / 首屏）

- 腾讯视频、哔哩哔哩、LibreTV、直播、外接设备、直播频道等 **不单独占格**
- 不展示其它预装 junk 图标（可在「全部应用」里看到）
- 不把 Netflix/Prime/Hotstar 等 CleanYourTV 默认坞搬过来

## 安装应用

- **不单独占坞格**；放在「全部应用」页内固定入口（首行「安装应用」）

## 自动更新

- 启动时读清单 `studio` 段（`applicationId` 仍是 `studio.muzi.tv`）
- 默认地址：https://zhishixingqiu.top/tv/version.json
- APK：https://zhishixingqiu.top/tv/apk/muzi-studio-*.apk
- 运行时可用 `files/config.json` 的 `updateUrl` 覆盖；构建时可用 `-PmuziUpdateUrl` 或 `-PmuziBaseUrl`
