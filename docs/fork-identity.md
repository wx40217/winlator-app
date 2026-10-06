# Fork 安装身份与后续同步

## 当前身份

- `applicationId`：`com.wxwinlat`；显示名称：`Winlator Fork`。
- 上游安装包身份为 `com.winlator`。两者使用各自的 Android UID、私有文件、RootFS、容器和 SharedPreferences；文件分享 authority 与前台服务 action 也按安装包身份生成。
- Java namespace 和 JNI 类名保留 `com.winlator`。它们是代码符号，不是安装包身份，保留可以避免破坏上游原生方法绑定。
- 当前交付仍为本机 Debug 签名。独立包名解决与上游的安装冲突；fork 内部后续更新仍需保持签名连续。正式签名及其备份另行配置。

## 原生运行文件中的绝对路径

上游 RootFS、Box64、Gladio、Vortek、Turnip 和 VirGL 归档中存在 `/data/data/com.winlator/` 的编译路径、配置或链接。仅修改 Gradle 的 applicationId 会导致运行文件仍指向上游私有目录。

`TarCompressorUtils` 解压归档时，使用 `RuntimePathRebaser` 将 `/data/data/com.winlator/` 与 `/data/user/0/com.winlator/` 转为当前 fork 路径，也转换归档中的绝对符号链接。转换覆盖随包资产及后来安装的上游组件，不修改源归档。

`com.wxwinlat` 与 `com.winlator` 的 ASCII 长度同为 12；转换保持文件长度和 ELF 内部偏移、字符串终止位置不变。Gradle 和转换器拒绝不等长的包名，避免二进制字符串被截断或扩长。将来更换为其他长度的包名，需要先重新编译相应 guest 组件。应用自身原生代码通过 Gradle → CMake 传入的安装包身份生成缓存与 socket 路径。

这只处理安装身份和路径，不代表已验证全部驱动、真实游戏、物理设备或多 Android 用户运行。

## 后续需求：与原版之间同步

用户希望后续考虑 fork 与原版的数据同步；本轮没有实现同步或自动导入。安装 fork 后不会自动出现原版的私有容器、快捷方式或收藏。

后续先调查原版现有的导入导出能力，再确定显式选择的导出包/共享目录方案。不同包名、不同签名的应用通常无法直接读取对方私有目录，不能依赖直接共享 RootFS 或修改原版文件作为同步机制。

设计同步前需要分别定义游戏文件与存档、容器配置、快捷方式及 fork 独有收藏/最近记录的范围，处理绝对路径与容器身份映射、同名冲突、格式版本、备份恢复和同步期间游戏正在运行的情况。首个可行版本优先考虑用户主动触发、可预览和可回滚的单向导入；双向自动同步在协议、权限和冲突策略明确之后再评估。
