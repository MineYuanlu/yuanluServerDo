# CI 工作流说明

本文档详细说明 `.github/workflows/ci_test.yml` 的 CI/CD 配置。

## 触发条件

- **Push**: `main`, `master`, `dev` 分支
- **Pull Request**: 目标为 `main`, `master`, `dev` 分支

## Job 矩阵

### 1. Build Matrix (`build`)

编译测试，验证项目在不同 Java 版本下均能编译通过。

| Java 版本 | 说明 |
|-----------|------|
| 8  | 项目源码级兼容目标 |
| 11 | 中间稳定版本 |
| 17 | Paper 1.18+ / Velocity 3.3.0+ 推荐版本 |
| 21 | 最新 LTS，Paper 1.20.5+ 要求 |

- 使用 `./mvnw -B -T 1C clean package --file pom.xml`
- 产物通过 `actions/upload-artifact@v4` 上传

### 2. Smoke Test — Bukkit (`smoke-test-bukkit`)

使用 [TestMC](https://github.com/gmitch215/TestMC) 启动真实 Minecraft 服务器验证插件加载。

| Runtime | Version | JDK | 说明 |
|---------|---------|-----|------|
| Paper   | 1.15.2  | 11  | 目标版本时代 |
| Paper   | 1.16.5  | 17  | BungeeCord 1.16 对应时代 |
| Paper   | 1.20.4  | 17  | 现代兼容性验证 |
| Paper   | 1.21.4  | 21  | 最新版本兼容性 |

> Spigot / CraftBukkit 变体在 `ci_test.yml` 中已注释，可按需启用（BuildTools 编译较慢）。

### 3. Smoke Test — BungeeCord (`smoke-test-bungeecord`)

| Runtime    | Version | JDK | 说明 |
|------------|---------|-----|------|
| BungeeCord | 1.16.5  | 17  | 目标版本（对应 `bungeecord-api 1.16-R0.5`） |
| BungeeCord | 1.21.4  | 21  | 最新版本兼容性 |

### 4. Smoke Test — Velocity (`smoke-test-velocity`)

| Runtime  | Version           | JDK | 说明 |
|----------|-------------------|-----|------|
| Velocity | 3.3.0-SNAPSHOT    | 17  | 目标版本（对应 `velocity-api 3.3.0-SNAPSHOT`） |
| Velocity | 3.4.0-SNAPSHOT    | 17  | 当前最新可用版本 |

> ⚠️ Velocity 3.5.0-SNAPSHOT 需要 Java 21，但 TestMC 尚未支持该版本。

## 设计决策

- **`fail-fast: false`**: 任一矩阵失败不影响其他组合继续测试
- **`needs: build`**: 冒烟测试依赖编译阶段，避免重复编译
- **`concurrency`**: 同一分支多次推送时自动取消旧任务
- **超时设置**: Build 15min / Bukkit 20min / Proxy 15-20min

## 功能测试 (TODO)

计划引入 MockBukkit 进行单元测试：

```xml
<dependency>
    <groupId>org.mockbukkit.mockbukkit</groupId>
    <artifactId>mockbukkit-v1.21</artifactId>
    <version>4.0.0</version>
    <scope>test</scope>
</dependency>
```

MockBukkit 仅支持 Bukkit/Paper 平台，BungeeCord/Velocity 模块需另寻测试方案。

## 参考

- TestMC: https://github.com/gmitch215/TestMC
- MockBukkit: https://github.com/MockBukkit/MockBukkit
