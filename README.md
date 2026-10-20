# HMCL Download Agent

## 背景

2026/10/20，上游 HMCL 通过 [PR #7017](https://github.com/HMCL-dev/HMCL/pull/7017) 移除了下载并安装游戏本体的功能（详见 Glavo 的 [B站动态](https://www.bilibili.com/opus/1261188623656550402)）。

本项目通过 Java Agent 在 HMCL 类加载时改写 `RegionUtils` 的网络地区检测，使其返回 `false`，恢复游戏下载。

## 使用

```bash
java -javaagent:/path/to/hmcl-download-agent.jar -jar HMCL.jar
```

使用 HMCL 启动器时，通过 `JAVA_TOOL_OPTIONS` 传入 agent：

```bash
JAVA_TOOL_OPTIONS="-javaagent:/absolute/path/to/hmcl-download-agent.jar" ./HMCL.sh
```

Windows 下使用 `set JAVA_TOOL_OPTIONS=-javaagent:C:\path\to\hmcl-download-agent.jar` 后运行 `HMCL.exe`。

## 构建与测试

```bash
./gradlew build
./scripts/test-against-hmcl.sh
```

生成 `build/libs/hmcl-download-agent.jar`，已内嵌 ASM，无运行时依赖。

## 开源协议

本项目使用 MIT 协议。内嵌 ASM 使用 BSD-3-Clause，详见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。
