# 部署 / Deploy

静态文件放到任意 HTTPS 站点的 `/tv/` 路径下 / Serve these from `/tv/` on any HTTPS host:

| 文件 File | 用途 Purpose |
|---|---|
| `www/version.json` | studio 自动更新清单（versionCode / url / sha256）/ auto-update manifest |
| `../fongmi-config/fongmi.example.json` | FongMi 配置示例（片源均为占位）/ FongMi config example, placeholder sources |
| `../fongmi-config/live-example.m3u` | 直播列表示例 / live playlist example |
| `server/sports_cms.py` | 体育聚合：稳定一级（斯诺克/羽毛球/足球/篮球）+ 扁平「赛事·年」+ SQLite 年份索引。`python3 sports_cms.py --self-test`，全量前先 `--index --pages N`。无「羽毛球·年」「台球·红牛」「羽毛球·量子」。 |
| `server/muzi-sports.service` | systemd 单元 / systemd unit |
| `server/muzi-tv.locations` | nginx `location` 片段，在你的 server 块里 include / nginx snippet |

步骤 / Steps:
1. 把 `www/` 拷到 `/var/www/muzi-tv/`，`sports_cms.py` 拷到 `/opt/muzi-tv/`。
2. `systemctl enable --now muzi-sports`；在 nginx HTTPS server 块 `include muzi-tv.locations;`，`nginx -t && nginx -s reload`。
3. 编辑 `sports_cms.py` 里的 `SRC` 与 `fongmi.example.json` 的 `api`，换成你有权使用的接口。
4. 构建 studio 时指定地址：`./gradlew assembleRelease -PmuziBaseUrl=https://your.domain/tv`。
5. 发新版：versionCode+1 → 构建 → 上传 `apk/` → 更新 `version.json` 的 versionCode/url/sha256。

回滚 / Rollback：删 include 行并 reload；`systemctl disable --now muzi-sports`；删除上述目录。
