# EchoPlay for Android

一款支持 B 站音轨播放和本地曲库管理的 Android 播放器。最低支持 Android 8.0（API 26）。

[![查看 Android 发行版](https://img.shields.io/badge/Android-%E6%9F%A5%E7%9C%8B%E5%8F%91%E8%A1%8C%E7%89%88-3DDC84?style=for-the-badge)](https://github.com/Y0vngG/EchoPlay-for-Android/releases)

发行版发布后，可在 Releases 页面下载 APK 并在手机上打开安装。也可以按下方步骤从源码运行。

## 主要功能

- 无需登录即可搜索 B 站视频，支持粘贴视频链接和按分 P 播放。
- 收藏曲目并管理本地曲库，支持筛选和移除收藏。
- 支持在线播放、拖动进度、列表循环和随机播放。
- 支持后台播放，可通过系统媒体控件暂停、继续或切换曲目。
- 可在应用内登录 B 站，也可退出登录。主题支持浅色、深色和跟随系统。

播放品质提供 360P、1080P 和 Hi-Res 无损三档。360P 优先选择低码率音轨，1080P 优先选择高品质音轨。Hi-Res 需要有效的 B 站大会员，实际播放品质取决于视频提供的音轨。

## 从源码运行

安装 JDK 17 或更高版本，以及 Android SDK Platform 37 和 Build-Tools 36.0.0。Gradle Wrapper 会使用项目配置的 JetBrains JDK 25 运行构建，首次构建时可能需要联网下载。

在 Android Studio 中打开项目根目录，完成 Gradle 同步后，选择 Android 设备或模拟器并运行 `app`。也可以连接已启用 USB 调试的设备，在项目根目录运行以下命令安装调试版。

```powershell
.\gradlew.bat installDebug
```

在 macOS 或 Linux 上使用 `./gradlew installDebug`。曲库、播放状态和应用设置保存在设备上的应用私有目录中。

## 打包 Android APK

在项目根目录构建调试 APK。

```powershell
.\gradlew.bat assembleDebug
```

在 macOS 或 Linux 上使用 `./gradlew assembleDebug`。调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

发布时，可在 Android Studio 中选择 **Build → Generate Signed App Bundle or APK**，为 `app` 模块生成签名的 `release` APK，再将 APK 上传到 GitHub Releases。

## 测试

在项目根目录运行 Android Lint。

```powershell
.\gradlew.bat lintDebug
```

在 macOS 或 Linux 上使用 `./gradlew lintDebug`。安装调试版后，可在设备上检查搜索、分 P 播放、收藏、后台媒体控制和登录。

## 致谢

项目延续了桌面版播放器的 B 站音轨播放和本地曲库功能。 [EchoPlay Windows 版](https://github.com/Y0vngG/EchoPlay)
