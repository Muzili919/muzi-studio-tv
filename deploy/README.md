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
4. 构建 studio 时指定地址：`./gradlew assembleRelease -PmuziBaseUrl=https://your.domain/tv`。
5. 发新版：versionCode+1 → 构建 → 上传 `apk/` → 更新 `version.json` 的 versionCode/url/sha256。

回滚 / Rollback：删 include 行并 reload；`systemctl disable --now muzi-sports`；删除上述目录。
