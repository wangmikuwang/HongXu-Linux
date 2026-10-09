<div align="center">

<img src="src/main/resources/icon.png" width="132" alt="虹叙应用图标">

# 虹叙 · HongXu（Linux）

**让你的故事继续。**

虹叙的 Linux 桌面版：多元人物与情感故事的文字冒险与 AI 叙事工坊——离线游玩分支剧本，或接入大模型，让 AI 导演与角色陪你即兴共创。

[![最新版本](https://img.shields.io/github/v/release/wangmikuwang/HongXu-Linux?label=%E6%9C%80%E6%96%B0%E7%89%88%E6%9C%AC)](https://github.com/wangmikuwang/HongXu-Linux/releases/latest)
![Debian](https://img.shields.io/badge/Debian-.deb-A81D33?logo=debian&logoColor=white)
![Arch Linux](https://img.shields.io/badge/Arch%20Linux-PKGBUILD-1793D1?logo=archlinux&logoColor=white)
[![GPL-3.0](https://img.shields.io/badge/%E8%AE%B8%E5%8F%AF%E8%AF%81-GPL--3.0-blue)](LICENSE)

</div>

## 安装

在[发布页面](https://github.com/wangmikuwang/HongXu-Linux/releases/latest)下载：

- **Debian / Ubuntu**：`sudo apt install ./hongxu_<版本>_amd64.deb`
- **Arch Linux**：`sudo pacman -U hongxu-<版本>-1-x86_64.pkg.tar.zst`，或从源码 `cd packaging/arch && makepkg -si`

安装后在应用菜单的「游戏」中打开，或在终端运行 `hongxu`。资料保存在 `~/.local/share/hongxu`，不上传剧情、存档或 AI 服务密钥；升级时直接覆盖安装即可保留。

## 从源码运行

需要 JDK 17 或更新版本：

```bash
./gradlew run          # 运行
./gradlew test         # 测试（含无界面渲染检查）
./gradlew packageDeb   # 构建 .deb（在 Linux 上）
```

基于 Kotlin 与 Compose for Desktop；液态玻璃效果由 Skia 着色器实现。

## 与手机版的差异

桌面版没有相机扫码、桌面图标切换、系统通知与壁纸取色；分享内容会复制到剪贴板或另存为 `.wenyou` 文件，二维码海报保存到「图片」文件夹。导入分享码、剧情、角色、AI 导演与存档功能与手机版一致。

## 许可

本应用以 [GPL-3.0](LICENSE) 发布。随附组件：Ionicons（MIT）、霞鹜文楷精简子集「Bundled Kai」（SIL OFL 1.1）、Material Color Utilities（Apache-2.0），许可文本见 `src/main/resources/licenses/` 与 `third_party/`。
