# 木子影界客户端组件

本目录保存木子增加的首页、排序/地区/类型筛选、独立直播入口、缓冲设置和Android 6网络兼容组件。它们加载在原FongMi 4.0.7中，播放器、直播、观看记录及配置数据库沿用原实现。最低API23，ARMv7候选422 / 4.0.7-muzi.10。

本次是对既有木子412客户端解包后增补组件，不是完整上游源码构建。附加Java组件在src/，组件检查在testsrc/。原客户端及其依赖保持各自上游许可，不适用本仓库桌面代码的MIT声明。

构建附加组件：使用JDK17和Gradle8.7，在本目录运行gradle jar；D8以min-api 23把jar编译为classes4.dex，替换已解包木子412包中原classes4.dex。用apktool重新打包时，将HomeActivity.onNewIntent在原处理之后调用HomeSurface.entry(Activity,Intent)，并在application中加入meta-data studio.muzi.entry.version=1。包名不变，versionCode=422，versionName=4.0.7-muzi.10，app_name为木子影界。保留既有HomeSurface.install/configured/back/menu插入点。最终APK应zipalign并以原木子签名重签，不能用另一把钥匙覆盖客户设备。

桌面通过muzi_entry=vod或live启动HomeActivity。直播入口等待原配置初始化后进入LiveActivity；点播首页不展示直播栏目。体育新增乒乓球；片库没有对应条目时显示暂无内容。

没有收录任何用户配置、订阅或观看记录。发布APK及SHA256见GitHub Release；安装前导出旧配置和记录备份。

原生配置与直播列表读取的补丁在patches/，仅自有服务器走兼容读取路径。第三方来源继续使用原网络规则。

播放器进程在局域网代理不可达时回退直连；可达的LAN代理与远程代理继续沿用。不修改电视的全局代理或其他应用。

搜索支持语音键（VOICE_ASSIST/ASSIST/SEARCH）、标准中文语音结果回填；没有标准识别服务的旧电视使用系统语音输入法。实际识别受遥控器、输入法及系统语音服务支持限制，需逐机确认。
