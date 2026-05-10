# AGENTS.md for yuanluServerDo

## Project Overview

Minecraft cross-server plugin suite (Java 8, Maven multi-module). Supports Bukkit/Spigot sub-servers with BungeeCord or Velocity proxy.

**Project is discontinued** — author graduated and is no longer maintaining. Accept maintenance PRs only.

## Module Layout

| Module | Type | Runtime | Key Files |
|--------|------|---------|-----------|
| `yuanluServerDo-common` | Shared library | None (compiled into all) | `Channel.java` (protocol), `ShareData.java` |
| `yuanluServerDo-bukkit` | Bukkit plugin | Bukkit/Spigot/Paper | `Main.java`, `Core.java`, `cmds/` |
| `yuanluServerDo-bungeecord` | BungeeCord plugin | BungeeCord proxy | `Main.java`, `Core.java`, `ConfigManager.java` |
| `yuanluServerDo-velocity` | Velocity plugin | Velocity proxy | `Main.java`, `Core.java`, `ConfigManager.java` |
| `yuanluServerDo-bukkit-bungeecord` | Shade aggregator | — | Only `pom.xml`, merges bukkit+bungeecord into one fat JAR |
| `yuanluServerDo-bukkit-velocity` | Shade aggregator | — | Only `pom.xml`, merges bukkit+velocity into one fat JAR |

**Never add Java source to the two shade aggregator modules.** They exist solely for deployment convenience.

## Build

- Requires Java 8+ (CI uses 17)
- Uses `maven-shade-plugin` with relocations for `org.bstats`
- All modules declare `<minimizeJar>true</minimizeJar>`
- **Maven Wrapper included** — no need to install Maven globally

### Local Build Commands

```bash
# Full build (all modules)
./mvnw -B clean package

# Build specific module and its dependencies
./mvnw -B clean package -pl yuanluServerDo-bukkit -am

# Skip tests (faster build)
./mvnw -B clean package -DskipTests

# Install to local repository
./mvnw -B clean install
```

**Note**: On Windows, use `mvnw.cmd` instead of `./mvnw`.

### Build Artifacts

Artifacts are generated in each module's `target/` directory:
- `yuanluServerDo-bukkit/target/*.jar`
- `yuanluServerDo-bungeecord/target/*.jar`
- `yuanluServerDo-velocity/target/*.jar`
- `yuanluServerDo-bukkit-bungeecord/target/*.jar` (fat JAR)
- `yuanluServerDo-bukkit-velocity/target/*.jar` (fat JAR)

### CI / Release (GitHub Actions)

CI 详细配置见 `.github/workflows/ci_test.readme.md`。**修改或接触测试相关代码前必须先阅读该文件。**

- **Continuous Integration** (`ci_test.yml`): PR/push 时触发，包含 Java 8/11/17/21 编译矩阵 + Bukkit/BungeeCord/Velocity 多版本冒烟测试
- **Release** (`Upload Release Asset.yml`): Release `published` 时触发，构建并上传 5 个 JAR 产物

### Cross-Server Protocol

- **Channel**: `bc:yuanlu-sdo` (ShareData.BC_CHANNEL, forced lowercase)
- **Packet format**: 4-byte big-endian int (Channel.ordinal) + 1-byte subId + payload
- **Protocol version**: Computed from `Channel` enum names MD5 hash. Mismatch blocks communication.
- **Naming convention**: `{s|p}{hexId}{C|S}_{name}`
  - `s`=send, `p`=parse; `C`=client(Bukkit), `S`=server(proxy)
  - Example: `s0C_tpReq` = Bukkit sends, subId 0, client side, teleport request

### Key Design Patterns

- `Channel` enum = packet type registry + codec + dispatcher
- `DataIn` / `DataOut` use fixed-size object pools (16 / 128) — never replace with unbuffered streams
- `WaitMaintain` (DelayQueue) handles all timeouts: `T_Net` (5s), `T_User` (120s)
- `LRUCache` (array-based, not LinkedHashMap) with `synchronized` — subclass `create(K)` for lazy loading
- Callbacks use custom functional interfaces in `Channel.Package` (e.g., `BoolConsumer`, `BiPlayerConsumer`)

## Bukkit-Specific Notes

### Command Registration
- `preload: true` in `config.yml` → registers commands during `onLoad()` to override Essentials/CMI
- `CommandManager` scans `cmds` package by reflection; class names must match `CmdXxx`
- Command name inferred from class name via camelCase-to-hyphen conversion

### Message System
- `config.yml` `message.*` supports three formats:
  - Plain string (`&` color codes)
  - String list (multi-line)
  - Config section with `.json` (tellraw) and `.msg` (fallback) sub-keys
- `Main.mes(node, type)` uses bitmask: `1`=no prefix, `2`=check empty, `4`=disable JSON

### Core Singleton
- `Core.INSTANCE` is both `Listener` and `PluginMessageListener`
- `CLEAR_LISTENER` callbacks fire on `PlayerQuitEvent` — register cleanup logic there

## Proxy-Specific Notes

### BungeeCord / Velocity Duality
- Both proxy modules are **parallel implementations** with nearly identical logic
- Velocity has one extra class: `CmdProxy.java` (registers backend commands as proxy commands)
- Both use `ConfigManager.java` with `ConfFile` / `PlayerConfFile` enums for YAML persistence

### Cross-Server Teleport (`Core.tpLocation`)
1. Send `TpLoc.s1S_tpLoc` to target server to prepare spawn location
2. If different server: call proxy API to switch player connection (`connect()`)
3. Callback returns success/failure to originating server

### Data Storage
- **WARPs**: `warp.yml` in proxy data folder
- **HOMEs**: Per-player `home.yml` under `{uuid}/` subdirectories, lazily loaded via `HomesLRU`
- **Server groups**: `proxy-config.yml` `server-group` controls which servers can teleport to each other
- **Always delay-save**: `WaitMaintain` batches writes; do not call synchronous save in hot paths

## External Dependencies

| Module | External Dep | Scope |
|--------|-------------|-------|
| common | `lombok` | provided |
| bukkit | `bukkit 1.15.1`, `bstats-bukkit` | provided / compile |
| bungeecord | `bungeecord-api 1.16-R0.5`, `bstats-bungeecord` | provided / compile |
| velocity | `velocity-api 3.3.0-SNAPSHOT`, `bungeecord-config`, `bstats-velocity` | provided / compile |

- Parent POM defines `yl-yuanlu-maven.pkg.coding.net` repository for internal artifacts
- ~~`settings.xml` references CODING Maven registry credentials (env vars)~~ (removed — no longer needed)

## Testing Notes

- **No unit tests exist** in this repo
- CI smoke tests use [TestMC](https://github.com/gmitch215/TestMC) to verify plugin loading on real servers
- When modifying protocol (`Channel.java`), verify both proxy implementations parse correctly
- Bukkit version compatibility spans many MC versions; avoid Bukkit APIs newer than 1.15.1 in common code

### Adding Unit Tests

If you want to add unit tests (especially for maintenance PRs):

1. Add `mockbukkit-v1.21` (or appropriate version) as a `test` scope dependency in the target module's `pom.xml`:
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
2. Write tests in `src/test/java/`
3. MockBukkit only supports Bukkit/Paper — BungeeCord/Velocity modules need different approaches
4. Update `.github/workflows/ci_test.yml` to uncomment the feature test job once tests exist

## Agent Checklist Before Editing

- [ ] Any change to `Channel` enum order or names **breaks protocol compatibility** — increment version or keep stable
- [ ] Common module must remain **zero Minecraft platform dependency** — no Bukkit/Velocity/Bungee imports
- [ ] Both proxy modules need parallel changes for feature parity
- [ ] Shade aggregator modules (`bukkit-bungeecord`, `bukkit-velocity`) should never get source code
