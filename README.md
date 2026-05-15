# ElderLauncher（看护桌面）

一个为老年人设计的 Android 桌面应用（Java），用于减少误触和简化操作。

## 主要功能
- 大按钮打开抖音
- 大按钮手电筒
- 大按钮清理抖音后台
- 超大时间日期显示
- 家人一键电话（头像 + 姓名 + 一键拨号）
- 首次强制设置 4 位 PIN
- 隐藏家属工具入口（右上角小圆点长按）
- 支持无障碍防下拉（可在家属工具里打开设置）
- 支持调试解锁（临时取消锁定）

## 项目结构
- `app/` Android 应用源码
- `release/elder-launcher-v1.2-debug.apk` 可直接安装的调试包（已签名，开发测试用）
- `release/elder-launcher-v1.2-release-unsigned.apk` Release 构建产物（未签名，发布前需你自己签名）

## 运行截图
![家属设置](docs/screenshots/01-settings.jpg)
![家属工具菜单](docs/screenshots/02-caregiver-tools.jpg)
![主桌面](docs/screenshots/03-main-home.jpg)

## 环境要求
- Android Studio（建议 Koala 或更新）
- Android SDK（minSdk 26，targetSdk 34）
- JDK 17（AGP 8.x 推荐）

## 本地构建
```powershell
cd "C:\Users\30203\Documents\New project"
.\gradlew.bat assembleDebug --no-daemon
```

生成 APK：
- `app/build/outputs/apk/debug/app-debug.apk`

## 安装 APK
```powershell
adb install -r release/elder-launcher-v1.2-debug.apk
```

如果要安装 release 包，请先签名 `release/elder-launcher-v1.2-release-unsigned.apk`。

## 首次使用
1. 启动应用后设置 4 位 PIN。
2. 进入家属设置添加家人电话和头像。
3. 将本应用设为默认桌面（Home app）。

## 家属工具入口
- 右上角小圆点，长按约 1.8 秒。
- 可进行：进入设置（PIN）、打开无障碍设置、默认桌面设置、调试解锁开关。

## 已知限制
- 普通应用权限下无法 100% 永久封死通知栏，这是 Android 系统限制。
- “无障碍防下拉”是增强方案，效果受不同厂商系统行为影响。
- “清理抖音后台”受系统进程管理策略影响，属于 best-effort。



## 仓库
- GitHub: https://github.com/3020ka/Car-Desktop.git
