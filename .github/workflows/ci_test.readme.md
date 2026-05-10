# CI 工作流说明

本文档详细说明 `.github/workflows/ci_test.yml` 的 CI/CD 配置。

## 触发条件

- **Push**: `main`, `master`, `develop` 分支
- **Pull Request**: 目标为 `main`, `master`, `develop` 分支

## Job 矩阵

### 1. Build Matrix (`build`)

编译测试，验证项目能够正确编译并生成产物。

| Java 版本 | 说明 |
|-----------|------|
| 17 | velocity-api 3.3.0+ 的注解处理器最低要求；产物字节码仍兼容 Java 8 |
| 21 | 最新 LTS，确保前瞻性兼容 |

- 使用 `./mvnw -B -T 1C clean package --file pom.xml`
- 产物通过 `actions/upload-artifact@v4` 上传

#### 为什么编译矩阵只保留 Java 17 / 21？

| Java 版本 | 排除原因 |
|-----------|----------|
| 8  | 源码级兼容目标，但 `velocity-api 3.3.0-SNAPSHOT` 的注解处理器需要 Java 17+，无法在 Java 8 下完成编译。产物通过 `maven-compiler-plugin` 的 `<source>1.8</source>` 和 `<target>1.8</target>` 保证字节码兼容。 |
| 11 | 中间版本，-ci 收益有限。Bukkit 1.15.2 时代的实际运行环境已由冒烟测试覆盖。 |

> ⚠️ 产物 JAR 仍可在 **Java 8+** 的 Minecraft 服务器上运行，只要实际运行的服务端支持对应平台版本。

---

### 2. Smoke Test — Bukkit (`smoke-test-bukkit`)

使用 [TestMC](https://github.com/gmitch215/TestMC) 启动真实 Minecraft 服务器验证插件加载。分三个子矩阵：

#### 2a. Paper

| Version | JDK | 节点意义 |
|---------|-----|----------|
| 1.15.2 | 11 | 项目目标版本时代（Bukkit API 1.15.1） |
| 1.16.5 | 17 | BungeeCord 1.16 对应时代 |
| 1.17.1 | 17 | **首个强制要求 Java 16 的 MC 版本**，验证跨越 JVM 大版本边界 |
| 1.20.4 | 17 | 现代兼容性验证 |
| 1.21.4 | 21 | **当前最新稳定版** |

#### 2b. Spigot

| Version | JDK | 节点意义 |
|---------|-----|----------|
| 1.15.2 | 11 | Spigot 构建路径的经典验证（BuildTools 编译较慢，取代表性版本） |
| 1.16.5 | 17 | 中间兼容版本 |
| 1.21.4 | 21 | 最新版本兼容性 |

> Paper 已覆盖大部分现代场景；Spigot 矩阵取**代表性版本**以控制 CI 耗时。

#### 2c. Purpur

| Version | JDK | 节点意义 |
|---------|-----|----------|
| 1.20.4 | 17 | Paper 硬分叉前的现代版本 |
| 1.21.4 | 21 | 当前最新稳定版（Purpur 基于 Paper 的最新分支） |

> Purpur 从 1.19 开始逐步差异化，取 **1.20.4+** 覆盖其独立生态。

---

### 3. Smoke Test — BungeeCord (`smoke-test-bungeecord`)

| Version | JDK | 节点意义 |
|---------|-----|----------|
| 1.16.5 | 17 | 目标版本（对应 `bungeecord-api 1.16-R0.5`） |
| 1.20.5 | 21 | **首个强制要求 Java 21 的 MC 版本**，验证 BungeeCord 对 Java 21 的兼容 |
| 1.21.4 | 21 | 当前最新版本兼容性 |

---

### 4. Smoke Test — Waterfall (`smoke-test-waterfall`)

| Version | JDK | 节点意义 |
|---------|-----|----------|
| 1.16.5 | 17 | BungeeCord 1.16 时代的 Waterfall 分支 |
| 1.20.5 | 21 | 首个强制 Java 21 版本 |
| 1.21.4 | 21 | 当前最新版本兼容性 |

> Waterfall 作为 BungeeCord 的高性能分支，API 基本一致，但内部实现有差异，需独立验证。

---

### 5. Smoke Test — Velocity (`smoke-test-velocity`)

| Version | JDK | 节点意义 |
|---------|-----|----------|
| 3.3.0-SNAPSHOT | 17 | 目标版本（对应 `velocity-api 3.3.0-SNAPSHOT`） |
| 3.4.0-SNAPSHOT | 17 | 当前最新可用版本 |

> ⚠️ Velocity 3.5.0-SNAPSHOT 需要 **Java 21**，但 [TestMC](https://github.com/gmitch215/TestMC) 尚未支持该版本。待 TestMC 更新后应补充此矩阵项。

---

### MC 版本选择总览

各版本在矩阵中的定位如下：

| 版本 | 定位 | 强制 JVM | 冒烟测试覆盖 |
|------|------|----------|--------------|
| 1.15.2 | 项目目标版本 | Java 8+ | Paper, Spigot |
| 1.16.5 | BungeeCord 1.16 对应时代 | Java 8+ | Paper, Spigot, BungeeCord, Waterfall |
| 1.17.1 | **首个强制 Java 16** | Java 16+ | Paper |
| 1.20.4 | 现代兼容性中间节点 | Java 17+ | Paper, Spigot, Purpur |
| 1.20.5 | **首个强制 Java 21** | Java 21+ | BungeeCord, Waterfall |
| 1.21.4 | **当前最新稳定版** | Java 21+ | 全部 |

---

## 设计决策

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `fail-fast` | `false` | 任一矩阵组合失败不影响其他组合继续测试，最大化问题暴露 |
| `needs: build` | 所有冒烟测试依赖 `build` | 编译一次，产物复用；避免每个冒烟测试独立编译 |
| `concurrency` | `group: ${{ github.workflow }}-${{ github.ref }}` + `cancel-in-progress: true` | 同一分支多次推送时自动取消旧任务，节省 CI 资源 |
| 超时 | Build 15min / Bukkit 20min / Proxy 15-20min | 防止 TestMC 下载/启动服务器时意外挂起导致计费溢出 |

## 功能测试 (TODO)

计划引入 MockBukkit 进行单元测试：

```xml
<dependency>
    <groupId>org.mockbukkit.mockbukkit</groupId>
    <artifactId>mockbukkit-v1.21</artifactId>
    <version>4.0.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

MockBukkit 仅支持 Bukkit/Paper 平台，BungeeCord/Velocity 模块需另寻测试方案（如专用 Mock 框架或集成测试）。

当功能测试就绪后，取消 `ci_test.yml` 中 Phase 3 的注释，启用 `./mvnw -B test -pl yuanluServerDo-bukkit -am`。

## 参考

- TestMC: https://github.com/gmitch215/TestMC
- MockBukkit: https://github.com/MockBukkit/MockBukkit
