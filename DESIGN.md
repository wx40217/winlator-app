---
name: Winlator 游戏库
description: 以 Android 原生操作和真实程序图标组织已有快捷方式。
colors:
  library-surface-light: "#F7F9FC"
  library-card-light: "#FFFFFF"
  library-tint-light: "#E3EDF9"
  library-accent-light: "#244D7A"
  library-header-light: "#244D7A"
  library-on-header-light: "#FFFFFF"
  library-text-light: "#192B42"
  library-secondary-text-light: "#53677E"
  library-surface-dark: "#121A25"
  library-card-dark: "#1C2735"
  library-tint-dark: "#273E59"
  library-accent-dark: "#B4D3F5"
  library-header-dark: "#273E59"
  library-on-header-dark: "#E5EDF8"
  library-text-dark: "#E5EDF8"
  library-secondary-text-dark: "#B4C5DC"
typography:
  title:
    fontFamily: sans-serif
    fontSize: 16sp
    fontWeight: 700
  body:
    fontFamily: sans-serif
    fontSize: 16sp
  label:
    fontFamily: sans-serif
    fontSize: 12sp
  supporting:
    fontFamily: sans-serif
    fontSize: 13sp
  empty-hint:
    fontFamily: sans-serif
    fontSize: 14sp
rounded:
  library: 12dp
spacing:
  item-inset: 6dp
  compact: 8dp
  related: 12dp
  card-inset: 14dp
  section: 16dp
  empty-inset: 24dp
components:
  search-light:
    backgroundColor: "{colors.library-card-light}"
    textColor: "{colors.library-text-light}"
    rounded: "{rounded.library}"
  search-dark:
    backgroundColor: "{colors.library-card-dark}"
    textColor: "{colors.library-text-dark}"
    rounded: "{rounded.library}"
  grid-card-light:
    backgroundColor: "{colors.library-card-light}"
    textColor: "{colors.library-text-light}"
    typography: "{typography.title}"
    rounded: "{rounded.library}"
    padding: "{spacing.card-inset}"
  grid-card-dark:
    backgroundColor: "{colors.library-card-dark}"
    textColor: "{colors.library-text-dark}"
    typography: "{typography.title}"
    rounded: "{rounded.library}"
    padding: "{spacing.card-inset}"
  list-item:
    typography: "{typography.title}"
    padding: "{spacing.compact}"
  overflow:
    width: 48dp
    height: 48dp
    padding: "{spacing.card-inset}"
  container-picker-light:
    backgroundColor: "{colors.library-card-light}"
    rounded: "{rounded.library}"
  container-picker-dark:
    backgroundColor: "{colors.library-card-dark}"
    rounded: "{rounded.library}"
  navigation-light:
    backgroundColor: "{colors.library-card-light}"
    textColor: "{colors.library-secondary-text-light}"
  navigation-dark:
    backgroundColor: "{colors.library-card-dark}"
    textColor: "{colors.library-secondary-text-dark}"
---

# Design System: Winlator 游戏库

## Overview

**Creative North Star: "冷色程序目录"**

蓝色导航提供稳定的屏幕上下文，冷浅色与深色阅读表面承载真实程序名称和图标。界面的性格直接、清楚，先让玩家辨认已有快捷方式，再提供搜索、筛选和再次启动入口。Android 原生控件承担交互，视觉表达集中在配色、内容层级和空间节奏。

本文件记录当前游戏库、主导航及设置页新增的布局偏好；它不是全部旧设置、容器编辑器或运行界面的统一改造规范。设计依据是 Java/XML 实现及模拟器截图；方向采用已授权的推荐方案。最近启动和收藏是本地元数据，不能作为运行成功或兼容性的视觉承诺。

**Key Characteristics:**

- 蓝色页眉与冷色明暗表面，跟随应用的系统／浅色／深色主题选择。
- 系统字体、两行程序标题与从属的容器／路径信息。
- 默认图标网格，可持久切换紧凑列表。
- 整项启动与独立的更多操作按钮各自承担明确任务。
- 横屏使用导航轨道及可纵向滚动的左控制区。

## Colors

主色是沉静的深蓝，明暗表面保留冷色倾向；完整值以 frontmatter 为准，Android 实现通过 `library*` theme attributes 解析。

### Primary

- **目录蓝**：`library-accent-light` 与 `library-header-light` 分别用于浅色主题的选中导航／收藏标记和页眉／状态栏。
- **浅冰蓝**：`library-accent-dark` 用于深色主题的选中导航及收藏标记；深色页眉使用 `library-header-dark`，避免把浅色强调色铺满导航背景。

### Neutral

- **冷纸／深夜底色**：`library-surface-light`、`library-surface-dark` 是游戏库根表面。
- **白卡／深蓝灰卡**：`library-card-light`、`library-card-dark` 承载网格、搜索框、容器筛选背景和导航表面。
- **冰雾／蓝灰衬色**：`library-tint-light`、`library-tint-dark` 用于图标底板和最近启动项。
- **深墨／浅灰白正文**：`library-text-light`、`library-text-dark` 用于库标题和主要内容；`library-on-header-light`、`library-on-header-dark` 用于页眉文字。
- **从属蓝灰**：`library-secondary-text-light`、`library-secondary-text-dark` 用于容器、路径、数量、说明及未选中的导航。

**The Theme Attribute Rule.** 新游戏库控件继续读取 `library*` theme attributes；浅色与深色值成对维护。当前实现是静态主题方案，没有墙纸动态配色。

系统选择由 `core/AppUtils.setActivityTheme` 按 `UI_MODE_NIGHT_MASK` 解析到浅色／深色主题；设置中也可显式选择浅色或深色。

## Typography

**Body Font:** Android 系统 sans-serif；当前布局没有装载自定义字体。`sans-serif` 表示系统继承，不是新增字体资产。

文字用 `sp` 随系统字体设置缩放。当前游戏库没有 display／hero 角色，也没有统一自定义行高、字距或等比字号尺度；不要从此文档推导不存在的 token。

### Hierarchy

- **Title**：`title` 用于程序名及库分区标题；源布局使用 `textStyle="bold"`。网格名称保留两行，超过两行在末尾省略；紧凑列表最多两行。
- **Body**：`body` 来自 `LibraryText`，用于游戏库的基础文字。
- **Label**：`label` 用于容器／目录上下文及项目数量。上下文最多两行，帮助区分同名程序。
- **Supporting**：`supporting` 用于目录路径和设置页布局说明。
- **Empty hint**：`empty-hint` 用于空状态的下一步说明。

搜索框、Chip、导航及旧设置控件继承 AppCompat／MaterialComponents 的字体规则；此处不把平台默认字号编造成项目 token。主工具栏保留应用现有标题样式，不要求游戏库标题承担工具栏角色。

**The Content Height Rule.** 名称和说明保持内容驱动的高度，文字缩放不通过固定文本高度抵消；两行省略必须保留完整的启动操作无障碍名称。

## Layout

竖屏：主工具栏位于顶部，搜索和筛选在控制区，内容区承担剩余空间，底部显示 Library／Containers／Settings 三个目的地。控制区左右留白来自 `section`；搜索之后用 `compact` 分隔筛选。主列表左右内边距为（10dp），底部为 `related`；每个项目外包一层 `item-inset`。

网格列数按实际 RecyclerView 宽度计算：`max(1, floor(widthDp / 180))`；（180dp）是列数计算目标，不是固定卡片宽度。网格内容最小高度为（156dp），内部使用 `card-inset`；图标为（52dp）见方。列表内容最小高度为（72dp），图标为（48dp）见方，文字距图标 `related`。

横屏判定来自 `Configuration.ORIENTATION_LANDSCAPE`，不是像素宽度或平板断点。主导航改为（80dp）宽 rail；左控制区为（260dp）宽、满可用高度的 `NestedScrollView`，搜索、筛选和最近区可纵向滚动，右侧内容区占剩余宽度。筛选由横排改为纵排。当前没有专门的平板宽度规则，手机横屏证据不等同于平板验证。

最近启动是横向 RecyclerView，只在存在真实记录且未进入目录／收藏／筛选时出现；列表与项目均为 `wrap_content`。项目宽度竖屏为（300dp）、横屏为（228dp），高度随标题与说明增长。空状态使用 `empty-inset`，行动与说明之间保留 `section`。

## Elevation & Depth

游戏库主要靠根表面、卡片表面和衬色形成层次。`library_card`、`library_tint`、`library_spinner` 的 XML 没有添加阴影或 elevation；不要为它们提取虚构的阴影 token。Material 导航、Chip、按钮、FAB 和弹出菜单仍可能使用平台／组件库继承的深度与状态反馈，不宣称整个应用完全无阴影。

没有新增自定义动画时长或 easing。点击反馈使用 `selectableItemBackground`／`selectableItemBackgroundBorderless` 及原生控件状态，不编造 Web hover 或 CSS focus 规则。

## Shapes

卡片、搜索背景、图标底板和容器选择框复用 `rounded.library` 的轻圆角。网格是独立卡片；普通紧凑列表项保持开放的行结构；最近列表项额外使用衬色背景。Chip 外形与触控扩展交由 `Widget.MaterialComponents.Chip.Choice`，不把库默认形状数值登记为本项目 token。

背景由 XML shape、layer-list 和 vector 定义；程序图片来自已有快捷方式，缺失时复用现有程序／文件夹图标。当前改造没有新增 shipping raster，也没有生成图片 prompt provenance。

## Components

### Search / Container Picker

搜索是 AppCompat `SearchView`，展开显示名称输入，最小高度（48dp）；背景引用卡片形状，正文、提示和图标读取对应的游戏库文字属性。容器筛选是原生 `Spinner`，最小高度（48dp），背景包含左（8dp）／右（36dp）／上下（4dp）的内容空间及（24dp）XML 下拉箭头；箭头距右边（8dp）。Spinner 标签仍由实际 ArrayAdapter／应用样式继承，不能宣称所有下拉文字已经完全改用新的文字属性。

### Collection Chips

All／Favorites 使用单选、必选的 Material ChipGroup，当前只占单行。Chip 启用 `ensureMinTouchTargetSize`；实际状态与颜色解析由 MaterialComponents 负责。不要在文档中添加未经实现的胶囊半径、悬停动画或选中描边。

### Grid Cards / List Items

真实图标与程序名是辨认入口，容器及相对目录是辅助信息。点击内容项启动程序或打开目录；更多操作按钮是独立的（48dp × 48dp）ImageButton，内边距使用 `card-inset`，并提供包含程序名的操作标签。收藏标记是（16dp）装饰图标，收藏状态同时加入启动项的无障碍名称。

损坏快捷方式在辅助文字中显示 Needs repair，点击时解释修复方法；加载失败提供重试；缺失快捷方式与启动链路异常使用 Snackbar。状态不靠颜色单独表达。

### Recent Launches

复用紧凑列表结构及衬色，不创建封面卡或占位游戏。高度跟随内容，最近记录表达的是启动动作；视觉上不能添加“运行成功”“恢复进度”或兼容性徽章。

### Navigation / Layout Preference

Material BottomNavigationView 与 NavigationRailView 使用同一菜单与选择逻辑。选中项读取 `libraryAccent`，未选中项读取 `librarySecondaryText`，表面读取 `libraryCard`。Android 系统返回和现有侧边菜单保留任务路径。

设置顶部新增的布局选择为全宽（48dp）Spinner，使用现有 `ComboBox` 样式，并保存 Grid／List 偏好；标题使用 `LibraryText`，说明使用 `supporting`。其下的旧设置内容保留现有样式，这次仅记录布局选择及主题接入，不视作全页视觉重构。

### Empty Actions / Paste

无数据、无收藏、无匹配和加载错误分别提供真实原因及下一步。空状态按钮最小高度（48dp），形状与颜色继承当前原生 Button；不登记未提取的 filled button token。粘贴 FAB 只在现有文件管理上下文需要时显示，仍沿用原有资源和行为。

## Do's and Don'ts

### Do:

- **Do** 以实际 Java/XML 及 Android 截图作为新增游戏库样式的依据，尺寸用 `dp`，字体用 `sp`。
- **Do** 配对维护游戏库明暗属性，并保留系统主题选择与系统字体缩放。
- **Do** 保留独立的（48dp × 48dp）更多操作目标和整项启动语义。
- **Do** 用容器／相对路径区分同名程序，保留内容驱动高度和完整操作标签。
- **Do** 让最近区只展示真实本地记录，继续复用已有程序或文件夹图标。
- **Do** 横屏使用当前 rail 与可滚动左控制区，按内容区实际宽度重新计算列数。

### Don't:

- **Don't** 把未统一改造的旧设置、容器页面和运行界面宣称为本次设计系统的完整覆盖。
- **Don't** 添加源码没有的阴影、字距、断点、动效或动态配色 token。
- **Don't** 把游戏库项目名／最近启动记录包装成兼容性、成功运行或进度恢复证明。
- **Don't** 用新增封面、虚构程序或生成图片替换真实快捷方式内容。
- **Don't** 将模拟器布局和边界检查当成真实 Windows 游戏、物理设备或 TalkBack 验证。

提取来源：`app/src/main/java/com/winlator/{ShortcutsFragment,MainActivity,SettingsFragment}.java`，`app/src/main/res/layout/{game_library_fragment,game_library_grid_item,game_library_list_item,main_activity,settings_fragment}.xml`，`app/src/main/res/values/{styles,library}.xml`，`app/src/main/res/drawable/library_*.xml` 与 `app/src/main/res/color/library_navigation.xml`。本次抽样查看父仓库 `.impeccable/review/phone-grid.png`、`phone-landscape.png`；它们是模拟器测试夹具。原生 detector 未运行，文档不包含 Web/CSS 审查结论。修复列表的 ship 结论不扩大为全产品或真实游戏验收。
