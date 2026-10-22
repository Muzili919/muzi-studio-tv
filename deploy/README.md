# 部署 / Deploy

静态文件放到任意 HTTPS 站点的 `/tv/` 路径下 / Serve these from `/tv/` on any HTTPS host:

| 文件 File | 用途 Purpose |
|---|---|
| `www/version.json` | studio 自动更新清单（versionCode / url / sha256）/ auto-update manifest |
| `../fongmi-config/fongmi.example.json` | FongMi 配置示例（片源均为占位）/ FongMi config example, placeholder sources |
| `../fongmi-config/live-example.m3u` | 直播列表示例 / live playlist example |
| `server/sports_cms.py` | 可选：把多个 CMS 聚合成体育分类的小服务，监听 127.0.0.1:18082 / optional sports aggregator |
| `server/muzi-sports.service` | systemd 单元 / systemd unit |
| `server/muzi-tv.locations` | nginx `location` 片段，在你的 server 块里 include / nginx snippet |

步骤 / Steps:
1. 把 `www/` 拷到 `/var/www/muzi-tv/`，`sports_cms.py` 拷到 `/opt/muzi-tv/`。
2. `systemctl enable --now muzi-sports`；在 nginx HTTPS server 块 `include muzi-tv.locations;`，`nginx -t && nginx -s reload`。
3. 编辑 `sports_cms.py` 里的 `SRC` 与 `fongmi.example.json` 的 `api`，换成你有权使用的接口。
4. 家人桌面默认检查 `https://zhishixingqiu.top/tv/version.json`（`studio` 段，包名 `studio.muzi.tv`）。覆盖：`./gradlew assembleRelease -PmuziUpdateUrl=https://host/tv/version.json`，或 `-PmuziBaseUrl=https://host/tv`。
5. 发新版：`versionCode+1` → 签名构建 → `deploy/sync-tv.sh <release.apk>`。脚本只改 `version.json` 的 `studio` 字段（versionCode / versionName / url / sha256），并拷 APK 到 `deploy/www/apk/muzi-studio-<versionName>.apk`。公网 URL 为 `https://zhishixingqiu.top/tv/apk/muzi-studio-<versionName>.apk`。有 SSH 时 `SSH_TARGET=user@host deploy/sync-tv.sh <apk>` 会传到 `/var/www/muzi-tv/`。没有密钥时，把这两个文件交到已经在发 `public-tv/www` 的那台机器（Mac：`/Users/xiaolongmu/Projects/muzi-tv/public-tv/www/`）再同步。`/tv/` 目录列表会 403，具体文件 URL 应 200。

回滚 / Rollback：删 include 行并 reload；`systemctl disable --now muzi-sports`；删除上述目录。
