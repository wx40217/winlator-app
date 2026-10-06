# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

现有 Java、XML Views、AppCompat、Fragments 和 RecyclerView；继续使用现有 Android 原生技术栈。

## Users

主要服务项目所有者和有基本折腾能力的玩家。用户已授权本轮未决设计项采用推荐方案；以下第一版范围可在方案评审后调整。

## Product Purpose

在 Android 上通过 Wine 和 Box86/Box64 运行 Windows 程序。第一版改造聚焦已有游戏快捷方式的查找、启动和再次启动。

## Operating Context

推荐默认：手机竖屏浏览游戏，游戏运行时以横屏为主；浏览页面也适配横屏和大屏。游戏关联现有容器，快捷方式可覆盖部分容器设置。

## Capabilities and Constraints

- 复用现有快捷方式、目录、容器配置和启动链路，保留其他 Windows 程序入口。
- 第一版改善已有快捷方式；首次安装程序、创建快捷方式及容器深度配置沿用现有入口。
- 用户已确认默认图标网格，设置中可切换紧凑列表并持久记住选择。
- 已有数据支持名称搜索、容器筛选和名称排序。最近启动、收藏、浏览位置需要新增本地持久化数据。
- 运行页退出当前会重启应用。浏览上下文的恢复需要跨重启持久化，不能仅依赖 Fragment 内存状态。
- “最近启动”记录启动动作，不代表程序运行成功；“再次启动”不表示恢复游戏进度。
- 现有图标和真实程序名称是内容依据；不默认联网匹配封面或兼容性预设。

## Brand Commitments

保留 Winlator 项目身份。PPSSPP、Dolphin 和 RetroArch 提供交互参考，第一版界面遵循 Android 原生操作习惯。

## Evidence on Hand

- 主仓库 fork：https://github.com/wx40217/winlator
- App 子模块 fork：https://github.com/wx40217/winlator-app
- 初始调研基于父仓库固定的 App 提交 a030f552f452158a2db64fdb32b490fa19c0b48d；实现已对齐 App fork 的 3981d86，保留其外接鼠标修复。
- 已完成原生调试 APK 构建、元数据单元测试，以及 Android API 30 ARM64 模拟器上的布局切换、搜索、收藏、目录返回和浏览状态恢复检查。
- 模拟器界面使用独立测试夹具；真实 Windows 游戏启动、游戏退出引发的重启和物理设备体验尚未验证。

## Product Principles

- 常用游玩操作优先可见，专业设置保持可达。
- 复用运行能力，明确区分界面反馈与真实运行结果。
- 搜索、收藏和浏览状态帮助玩家快速再次启动。
- 保留现有数据和 Windows 程序管理能力。

## Accessibility & Inclusion

推荐以 48dp 触控目标、系统字体缩放、可读对比度、无障碍标签、系统返回行为和减少动画偏好作为第一版验收约束。
