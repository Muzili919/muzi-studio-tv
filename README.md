# muzi studio TV

一个给 Android TV（Android 6.0+ / API 23）用的轻量电视桌面：推荐页 → 电影 / 电视剧 / 动漫 / 综艺 / 体育 / 直播 → 分类，播放可交给 FongMi；带远程自动更新、开机自启、HOME 键转跳。

A lightweight Android TV home (Android 6.0+ / API 23): a recommendation page with Movies / TV / Anime / Variety / Sports / Live entries and sub-categories, FongMi as an optional player/source, plus self-hosted auto-update, start-on-boot and HOME-key redirect.

## 目录 / Layout
- `android/` — studio 源码 / app source (Java, Media3 ExoPlayer)
- `fongmi-config/` — FongMi 配置与直播列表示例（接口均为占位）/ FongMi config & playlist examples (placeholders)
- `deploy/` — 服务端部署（nginx 片段、systemd、version.json）/ server deploy files
- `docs/debloat.md` — 去开机广告、默认桌面说明 / debloat & default-home notes

## 构建 / Build
```bash
cd android
./gradlew assembleRelease -PmuziBaseUrl=https://your.domain/tv -PmuziLibreUrl=https://libretv.your.domain
# 可选签名 / optional signing:
# 优先读 android/signing.env（gitignore，不入库）；也可继续用环境变量：
# MUZI_KEYSTORE=/path/release.jks MUZI_KEYSTORE_PASSWORD=... MUZI_KEY_ALIAS=... ./gradlew assembleRelease
```
`muziBaseUrl` 下应有 `version.json`、`live-example.m3u`、`sports/`（见 `deploy/`）。远程升级必须一直用同一把签名。

The base URL must serve `version.json`, the playlist and optional `sports/`. Always sign updates with the same key.

## 片源说明 / Content sources
本仓库**不提供任何片源**，示例里的接口都是 `example.com` 占位。请只接入你有权使用的内容。
This repo ships **no content sources**; all endpoints are `example.com` placeholders. Use only content you are entitled to.

## 致谢 / Credits
- [cleanyourtv](https://github.com/Gathin23/cleanyourtv)（MIT）— 去广告与替换桌面的思路 / debloat & launcher ideas（本仓库暂未包含其代码）
- [LibreTV](https://github.com/LibreSpark/LibreTV)（AGPL-3.0）— 推荐页界面参考；studio 只通过 URL 打开你自己部署的 LibreTV，本仓库**不包含** LibreTV 代码 / UI inspiration; linked by URL only, no LibreTV code included
- [FongMi TV](https://github.com/FongMi/TV) — 播放器 / player（未修改、未重新打包 / unmodified, not redistributed）

## License
MIT
