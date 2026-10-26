# muzi studio TV

Android TV（Android 6.0+ / API 23）电视桌面：木子影界、直播电视、自选应用与设置入口；添加和移除应用在设置中，支持上下翻页。支持清晰星空壁纸、连续水波、流星、开机检查和下载更新、HOME键返回。普通设备安装更新仍可能需要系统确认。

A native Android TV home (API23+) with separate VOD/live entries, customizable app shortcuts, gentle lake motion and startup update checks. System installation approval may still be required.

## 目录 / Layout
- `android/` — studio 源码 / app source (Java, Media3 ExoPlayer)
- `fongmi-config/` — FongMi 配置与直播列表示例（接口均为占位）/ FongMi config & playlist examples (placeholders)
- `deploy/` — 服务端部署（nginx 片段、systemd、version.json）/ server deploy files
- `docs/debloat.md` — 去开机广告、默认桌面说明 / debloat & default-home notes

## 构建 / Build
```bash
cd android
./gradlew assembleRelease
# 更新清单默认 https://zhishixingqiu.top/tv/version.json
# 覆盖 / override: -PmuziUpdateUrl=https://host/tv/version.json
# 或 -PmuziBaseUrl=https://host/tv（自动加上 /version.json）
# 签名 / signing: 仓库根 signing.env（gitignore），或
# MUZI_KEYSTORE=/path/release.jks MUZI_KEYSTORE_PASSWORD=... MUZI_KEY_ALIAS=... ./gradlew assembleRelease
```
默认更新源是现有站点 `https://zhishixingqiu.top/tv/`（`version.json` + `apk/muzi-studio-*.apk`）。远程升级必须一直用同一把签名。发布步骤见 `deploy/sync-tv.sh` 与 `INSTALL-FAMILY.md`。

The base URL must serve `version.json`, the playlist and optional `sports/`. Always sign updates with the same key.

## 片源说明 / Content sources
本仓库**不提供任何片源**，示例里的接口都是 `example.com` 占位。请只接入你有权使用的内容。
This repo ships **no content sources**; all endpoints are `example.com` placeholders. Use only content you are entitled to.

## 致谢 / Credits
- [cleanyourtv](https://github.com/Gathin23/cleanyourtv)（MIT）— 去广告与替换桌面的思路 / debloat & launcher ideas（本仓库暂未包含其代码）
- [LibreTV](https://github.com/LibreSpark/LibreTV)（AGPL-3.0）— 推荐页界面参考；studio 只通过 URL 打开你自己部署的 LibreTV，本仓库**不包含** LibreTV 代码 / UI inspiration; linked by URL only, no LibreTV code included
- [FongMi TV](https://github.com/FongMi/TV) — 播放器 / player（木子播放器基于4.0.7增补首页与路由；附加组件源码在player/，原客户端与依赖沿用上游许可）

## 1.2.5 升级说明

[新版安装和旧版本兼容说明](docs/releases/1.2.5.md)。桌面及播放器APK在GitHub Release，自有更新通道沿用原version.json。客户配置和观看记录不会放入仓库。

## License
MIT
