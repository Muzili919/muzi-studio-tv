# 去开机广告 / 去桌面推广（小米电视示例）

> 全部通过 ADB，不需要 root，不卸载任何系统应用，都可以恢复。先备份：
> `adb shell pm list packages -d > disabled-before.txt`、`adb shell settings list global > global.txt`、`adb shell settings list secure > secure.txt`

1. 电视：设置 → 账号与安全 → 允许 ADB 调试（或开发者选项里的 USB/网络调试）。
2. 电脑：`adb connect <电视IP>:5555`，在电视上点「始终允许」。
3. 关闭开机广告：系统设置里一般有「开机广告/开机推荐」开关，先关掉；再查看广告相关包：
   `adb shell pm list packages | grep -iE 'ad|splash|advert'`
   确认不是你要用的再 `adb shell pm disable-user --user 0 <包名>`。
4. 默认桌面：Android 8+ 可在系统设置里把 studio 设为默认 HOME；部分系统（如 Android 6 的小米）不允许禁用原桌面，
   可用 studio 的「HOME 键转跳」（无障碍服务）或「开机自启」作为替代。
5. 恢复：`adb shell pm enable <包名>`。

English: everything is done over ADB without root; packages are disabled with `pm disable-user`, never uninstalled, and can be re-enabled with `pm enable`. On Android 8+ you can set studio as the default home; on older sets use the HOME-key redirect or start-on-boot fallback.

延伸参考 / See also: [cleanyourtv](https://github.com/Gathin23/cleanyourtv) 的分品牌禁用清单（MIT）。
