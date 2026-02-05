# Design: Add NeoForge Platform Support

## Metadata

- **Change ID**: `add-neoforge-platform-support`
- **Status**: Proposed
- **Last Updated**: 2025-02-05

## Overview

This design document outlines the technical architecture for adding NeoForge support to MC-MMD-rust. The implementation follows the existing Architectury-based multi-loader pattern used for Fabric and Forge, introducing a new `neoforge/` module while maximizing code reuse through the `common/` module.

## Architecture

### Current Multi-Loader Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Gradle Build System                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────┐   │
│  │ fabric  │  │  forge  │  │neoforge │  │   common    │   │
│  └────┬────┘  └────┬────┘  └────┬────┘  └──────┬──────┘   │
│       │            │            │               │           │
│       └────────────┴────────────┴───────────────┘           │
│                     Architectury Platform                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     Minecraft Runtime                       │
│  (Fabric Loader | Forge | NeoForge)                         │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

```
worktree/
├── common/                 # Shared code (Architectury)
│   └── src/main/java/com/shiroha/mmdskin/
│       ├── config/        # Common config interfaces
│       ├── model/         # Model loading and management
│       ├── physics/       # Physics engine bridge
│       └── render/        # Rendering abstractions
│
├── fabric/                 # Fabric-specific
│   └── src/main/java/com/shiroha/mmdskin/fabric/
│       └── MmdSkinFabric.java
│
├── forge/                  # Forge-specific (existing)
│   └── src/main/java/com/shiroha/mmdskin/forge/
│       ├── MmdSkinForge.java
│       ├── MmdSkinForgeClient.java
│       └── ...
│
└── neoforge/               # NeoForge-specific (NEW)
    └── src/main/java/com/shiroha/mmdskin/neoforge/
        ├── MmdSkinNeoForge.java          # Main mod class
        ├── MmdSkinNeoForgeClient.java    # Client initialization
        ├── register/
        │   ├── MmdSkinRegisterCommon.java
        │   └── MmdSkinRegisterClient.java
        ├── config/
        │   ├── MmdSkinConfig.java
        │   └── ModConfigScreen.java
        ├── network/
        │   └── MmdSkinNetworkPack.java
        └── maid/
            └── MaidRenderEventHandler.java
```

## Component Design

### 1. Build System (Gradle)

#### 1.1 Settings Configuration

**File**: `settings.gradle`

**Change**:
```gradle
include 'common'
include 'fabric'
include 'forge'
include 'neoforge'  // NEW
```

#### 1.2 NeoForge Build Configuration

**File**: `neoforge/build.gradle`

**Key Differences from Forge**:

```gradle
plugins {
    id 'com.gradleup.shadow' version '8.3.6'
}

architectury {
    platformSetupLoomIde()
    neoforge()  // Changed from forge()
}

loom {
    neoforge {  // Changed from forge
        mixinConfig "mmdskin-neoforge.mixins.json"
    }
    mixin {
        defaultRefmapName = "mmdskin-neoforge-refmap.json"
    }
    runs {
        client {
            client()
            name "NeoForge Client"
            runDir "run/client"
            // No neoforge VM arg needed (NeoForge-specific)
            programArgs "--username", "Player1"
        }
        server {
            server()
            name "NeoForge Server"
            runDir "run/server"
        }
    }
}

dependencies {
    // Common module integration
    common(project(path: ":common", configuration: "namedElements")) { transitive false }
    shadowCommon(project(path: ":common", configuration: "transformProductionNeoForge")) { transitive false }

    // NeoForge platform
    neoforge "net.neoforged:neoforge:${neoforge_version}"  // Changed from forge

    // Architectury API - NeoForge version
    modApi("dev.architectury:architectury-neoforge:${architectury_version}")
    include("dev.architectury:architectury-neoforge:${architectury_version}")

    // Cloth Config API - NeoForge version
    modApi("me.shedaniel.cloth:cloth-config-neoforge:${cloth_config_version}")

    // Optional dependencies
    modCompileOnly("maven.modrinth:touhou-little-maid:${tlm_version}-neoforge+mc1.20.1")
}
```

**Rationale**:
- Uses Architectury's NeoForge platform support
- NeoForge dependency replaces Forge
- Configuration `transformProductionNeoForge` handles bytecode transformations
- Include Architectury and Cloth Config via Jar-in-Jar

#### 1.3 Version Properties

**File**: `gradle.properties`

**Additions**:
```properties
neoforge_version=47.1.81
architectury_version=9.2.14
cloth_config_version=11.1.136
```

### 2. Mod Metadata

#### 2.1 NeoForge Mods.toml

**File**: `neoforge/src/main/resources/META-INF/neoforge.mods.toml`

**Key Changes from Forge**:

```toml
# NeoForge uses different loader identifier
modLoader = "neoforge"
loaderVersion = "[47.1,)"  # NeoForge 47.1.x+ for MC 1.20.1

[[mods]]
modId = "mmdskin"
version = "${mod_version}"
displayName = "3D Skin Layers"
authors = "shiroha233, kjkjkAIStudio(原作者), asuka-mio, Gengorou-C"
description = '''A mod that replaces Minecraft entity rendering with MMD models'''
logoFile = "icon.png"

[[dependencies.mmdskin]]
modId = "neoforge"
mandatory = true
versionRange = "[47.1,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.mmdskin]]
modId = "minecraft"
mandatory = true
versionRange = "[1.20.1,1.21)"
ordering = "NONE"
side = "BOTH"

[[dependencies.mmdskin]]
modId = "cloth_config"
mandatory = true
versionRange = "*"
ordering = "NONE"
side = "CLIENT"
```

**Rationale**:
- NeoForge uses `modLoader = "neoforge"` instead of `"javafml"`
- Loader version range reflects NeoForge's versioning scheme
- Rest of structure is compatible with Forge format

#### 2.2 Resource Processing

**File**: `neoforge/build.gradle`

```gradle
tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
        minecraft_version: '1.20.1',
        minecraft_version_range: '[1.20.1,1.21)',
        neoforge_version: project.neoforge_version,  # Changed
        neoforge_version_range: '[47.1,)',           # Changed
        mod_id: 'mmdskin',
        mod_name: '3D Skin Layers',
        mod_version: project.version,
        mod_authors: 'shiroha233 et al.',
        mod_description: 'MMD model rendering mod for Minecraft'
    ]

    inputs.properties replaceProperties

    filesMatching(['META-INF/neoforge.mods.toml']) {  # Changed
        expand replaceProperties
    }
}
```

### 3. Platform Implementation

#### 3.1 Main Mod Class

**File**: `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/MmdSkinNeoForge.java`

**Design**:

```java
package com.shiroha.mmdskin.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Import from common module
import com.shiroha.mmdskin.MMD skin;

@Mod(MmdSkin.MOD_ID)
public class MmdSkinNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(MmdSkinNeoForge.class);

    public MmdSkinNeoForge(IEventBus modEventBus) {
        // Register common setup event
        modEventBus.addListener(this::commonSetup);

        // Register deferred registration from common module
        MmdSkinRegisterCommon.register(modEventBus);

        // Register to NeoForge event bus for game events
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("MMD Skin Mod (NeoForge) loaded");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Initialize common module
        MmdSkin.init();

        // Register networking
        MmdSkinNetworkPack.register();

        LOGGER.info("Common setup completed");
    }
}
```

**Key API Mappings**:
- `net.minecraftforge.*` → `net.neoforged.*`
- `@Mod` annotation from `net.neoforged.fml.common.Mod`
- `IEventBus` from `net.neoforged.bus.api.IEventBus`
- `FMLCommonSetupEvent` from `net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent`

**Rationale**:
- Follows NeoForge mod initialization pattern
- Delegates complex logic to common module
- Minimal platform-specific code

#### 3.2 Client Initialization

**File**: `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/MmdSkinNeoForgeClient.java`

**Design**:

```java
package com.shiroha.mmdskin.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.ClientTickEvent;

@EventBusSubscriber(modid = MmdSkin.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MmdSkinNeoForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Initialize client-specific components
        MmdSkinRegisterClient.register();

        // Register keybindings
        MmdSkinKeyBindings.register();

        // Register renderer
        event.enqueueWork(() -> {
            EntityRenderers.register(
                MmdSkinEntityType.PROJECTILE.get(),
                MmdSkinProjectileRenderer::new
            );
        });
    }

    @EventBusSubscriber(modid = MmdSkin.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameEvents {
        @SubscribeEvent
        public static void onRenderTick(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                MmdSkinRenderer.onRenderTick(event.getPartialTick());
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            MmdSkinClient.onClientTick();
        }
    }
}
```

**Key Differences from Forge**:
- `RenderLevelStageEvent` instead of Forge's `RenderWorldLastEvent`
- Event stage enum values may differ
- `net.neoforged.neoforge.client.event` package path

#### 3.3 Registration Classes

**File**: `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/register/MmdSkinRegisterCommon.java`

**Design Pattern**:

```java
package com.shiroha.mmdskin.neoforge.register;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.shiroha.mmdskin.common.registry.MmdSkinRegistry;

public class MmdSkinRegisterCommon {
    public static final DeferredRegister<?> OBJECTS = DeferredRegister.create(
        NeoForgeRegistries.Keys.EVENT_KEYS,
        MmdSkin.MOD_ID
    );

    public static void register(IEventBus modEventBus) {
        OBJECTS.register(modEventBus);

        // Delegate to common registry
        MmdSkinRegistry.registerCommon();
    }
}
```

**Rationale**:
- Uses NeoForge's `DeferredRegister` for type-safe registration
- Bridges to common module's registry system
- Maintains consistency with common module interfaces

### 4. Configuration System

#### 4.1 Config Implementation

**File**: `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/config/MmdSkinConfig.java`

**Design**:

```java
package com.shiroha.mmdskin.neoforge.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "mmdskin")
public class MmdSkinConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static MmdSkinConfig instance;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int renderDistance = 50;

    @ConfigEntry.Category("physics")
    @ConfigEntry.BoundedDiscrete(min = 10, max = 120)
    public int physicsFPS = 60;

    // ... other config fields

    public static void init() {
        // Cloth Config auto-registration
    }
}
```

**Rationale**:
- Uses Cloth Config API's `@Config` annotation
- Implements `ConfigData` for serialization
- Categories match common module config structure

#### 4.2 Config Screen

**File**: `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/config/ModConfigScreen.java`

**Design**:

```java
package com.shiroha.mmdskin.neoforge.config;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.ModListScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;

public class ModConfigScreen {
    public static Screen getConfigScreen(Screen parentScreen) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parentScreen)
            .setTitle(Component.translatable("title.mmdskin.config"))
            .setSavingRunnable(() -> {
                // Save config
                MmdSkinConfig.save();
            });

        // Add categories
        addRenderingCategory(builder);
        addPhysicsCategory(builder);

        return builder.build();
    }

    private static void addRenderingCategory(ConfigBuilder builder) {
        ConfigCategory rendering = builder.getOrCreateCategory(
            Component.translatable("category.mmdskin.rendering")
        );

        rendering.addEntry(builder.startIntList(
            Component.translatable("option.mmdskin.renderDistance"),
            MmdSkinConfig.instance.renderDistance
        ).setDefaultValue(50).setSaveConsumer(val -> {
            MmdSkinConfig.instance.renderDistance = val;
        }).build());
    }

    // ... other categories
}
```

### 5. Networking

#### 5.1 Network Implementation

**File**: `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/network/MmdSkinNetworkPack.java`

**Design**:

```java
package com.shiroha.mmdskin.neoforge.network;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;

public class MmdSkinNetworkPack {
    public static final ResourceLocation CHANNEL_ID =
        ResourceLocation.fromNamespaceAndPath(MmdSkin.MOD_ID, "main");

    public static void register(RegisterPayloadHandlerEvent event) {
        // Register custom payload handlers
        event.register(CHANNEL_ID, clientHandler(), serverHandler());
    }

    private static IPlayPayloadHandler<ExamplePayload> clientHandler() {
        return (payload, context) -> {
            // Handle client-bound packet
            context.workHandler().execute(() -> {
                // Process packet on main thread
                handleClientPacket(payload);
            });
            context.packetHandler().sendPacket(new AckPayload());
        };
    }

    private static IPlayPayloadHandler<ExamplePayload> serverHandler() {
        return (payload, context) -> {
            // Handle server-bound packet
            context.workHandler().execute(() -> {
                handleServerPacket(payload);
            });
        };
    }

    public static void sendToClient(ServerPlayer player, ExamplePayload payload) {
        PacketDistributor.PLAYER.with(player).send(payload);
    }

    public static void sendToServer(ExamplePayload payload) {
        PacketDistributor.SERVER.noArg().send(payload);
    }
}
```

**Key Differences from Forge**:
- NeoForge 1.20.2+ uses modern `ChannelBuilder` and custom payload system
- Replaces Forge's `SimpleChannel`
- `RegisterPayloadHandlerEvent` for channel registration
- `PacketDistributor` API

**Rationale**:
- NeoForge's new networking API is more type-safe and efficient
- Custom payloads replace legacy packet system
- Better integration with Minecraft's network protocol

### 6. Native Library Integration

#### 6.1 Library Loading

The native Rust library loading is handled in the `common` module through JNI. NeoForge should use the same mechanism as Forge since:

1. Both use standard Java `System.loadLibrary()`
2. Library extraction from JAR to temp directory is identical
3. JNI function signatures are platform-independent

**Verification Needed**:
- Test native library loading on NeoForge
- Verify library path resolution works
- Confirm no NeoForge-specific classloading issues

**Fallback Strategy**:
If issues arise, wrap native loading in platform-specific adapter:

```java
// In neoforge module
public class NeoForgeNativeLoader {
    public static void loadNativeLibrary() {
        try {
            // Try standard loading first
            System.loadLibrary("mmdskin_engine");
        } catch (UnsatisfiedLinkError e) {
            // NeoForge-specific fallback
            String libPath = extractLibrary();
            System.load(libPath);
        }
    }
}
```

### 7. Build Outputs

#### 7.1 Artifact Naming

**Build Products**:
- `fabric/build/libs/mmdskin-fabric-{version}.jar`
- `forge/build/libs/mmdskin-forge-{version}.jar`
- `neoforge/build/libs/mmdskin-neoforge-{version}.jar` (NEW)

**Shadow JARs (dev)**:
- `mmdskin-fabric-{version}-dev.jar`
- `mmdskin-forge-{version}-dev.jar`
- `mmdskin-neoforge-{version}-dev.jar` (NEW)

#### 7.2 Release JAR Structure

```
mmdskin-neoforge-1.0.0.jar
├── META-INF/
│   ├── MANIFEST.MF
│   └── neoforge.mods.toml
├── com/shiroha/mmdskin/
│   ├── neoforge/           # Platform-specific code
│   └── common/             # Shadowed common code
├── assets/mmdskin/         # Resources
├── natives/                # Native libraries (extracted at runtime)
└── META-INF/jarjar/        # Embedded dependencies
    └── architectury/
```

## Testing Strategy

### Unit Testing

No formal unit tests for platform code - relies on manual testing.

### Integration Testing

**Test Matrix**:

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| Build succeeds | JAR produced without errors | Critical |
| Client startup | Main menu reachable | Critical |
| Mod loading | Mod appears in mod list | Critical |
| PMX loading | Model loads and renders | Critical |
| Physics | Hair/cloth simulate | Critical |
| Config UI | Screen opens and saves | High |
| Iris compatibility | No shader conflicts | High |
| Native library | Loads without errors | Critical |
| Multiplayer | Server/client work | Medium |

### Performance Testing

**Benchmarks** (compare NeoForge vs Forge):
- FPS with single model loaded
- FPS with 10 models visible
- Physics simulation time (ms per frame)
- Memory usage (MB)
- JAR file size

**Acceptance Criteria**:
- FPS within 5% of Forge version
- Memory usage within 10% of Forge
- No memory leaks after 1 hour runtime

## Migration Risks & Mitigations

### Risk 1: API Incompatibility

**Risk**: NeoForge APIs differ significantly from Forge in some areas

**Impact**: High - Could block implementation

**Mitigation**:
- Reference NeoForge documentation extensively
- Check NeoForge mod examples on GitHub
- Start with simplest possible implementation
- Incremental testing of each component

### Risk 2: Dependency Incompatibility

**Risk**: Architectury or Cloth Config versions don't support NeoForge properly

**Impact**: High - Could require framework changes

**Mitigation**:
- Verify dependency versions before starting
- Test with minimal reproduction case first
- Have fallback to older stable versions
- Monitor NeoForge community for known issues

### Risk 3: Native Library Issues

**Risk**: JNI library fails to load or crashes on NeoForge

**Impact**: Critical - Blocks all functionality

**Mitigation**:
- Test native loading early in implementation
- Add extensive error logging
- Implement graceful degradation
- Have workaround ready (platform-specific loader)

### Risk 4: Build Configuration Errors

**Risk**: Gradle configuration issues prevent building

**Impact**: High - Blocks development

**Mitigation**:
- Start with working Forge config as template
- Make incremental changes
- Test each Gradle task individually
- Keep backup of working configuration

## Rollback Strategy

If critical issues arise that cannot be resolved:

1. **Revert commit** removing `neoforge/` module
2. **Remove** `include 'neoforge'` from `settings.gradle`
3. **Document** issues for future investigation
4. **Consider** alternative approach (e.g., waiting for NeoForge updates)

**No impact** on existing Fabric and Forge functionality since NeoForge module is isolated.

## Future Considerations

### Potential Enhancements

1. **Unified JAR**: Investigate single JAR for all loaders (requires advanced build tooling)
2. **Automatic Testing**: CI/CD pipeline for automated cross-platform testing
3. **Performance Profiling**: Detailed profiling to optimize NeoForge-specific paths
4. **Mod Compat**: Add compatibility patches for popular NeoForge mods

### Maintenance Burden

**Estimated Maintenance Effort**:
- Each Minecraft version update: +4-6 hours (third platform)
- Each major Architectury update: +1-2 hours (testing)
- Each major NeoForge update: +2-4 hours (API changes)

**Cost-Benefit Analysis**:
- **Benefit**: ~30-40% increase in potential user base
- **Cost**: ~25-30% increase in maintenance workload
- **Verdict**: **Worthwhile** given NeoForge's growing adoption

## References

### Documentation

- [NeoForge Official Docs](https://docs.neoforged.net/)
- [NeoForge Gradle Plugin](https://github.com/neoforged/NeoGradle)
- [Architectury NeoForge Support](https://github.com/ArchitecturyDev/Architectury)
- [Cloth Config NeoForge](https://github.com/shedaniel/ClothConfig)

### Example Mods

- NeoForge example mods (official repository)
- Existing multi-loader mods using Architectury
- Community NeoForge mods for reference implementations

## Appendix: API Mapping Table

| Forge (old) | NeoForge (new) | Notes |
|-------------|----------------|-------|
| `net.minecraftforge` | `net.neoforged` | Package rename |
| `net.minecraftforge.fml` | `net.neoforged.fml` | FML package |
| `@Mod("modid")` | `@Mod("modid")` | Same annotation, different package |
| `SimpleChannel` | `ChannelBuilder` | New networking API |
| `RenderWorldLastEvent` | `RenderLevelStageEvent` | Rendering events |
| `modLoader = "javafml"` | `modLoader = "neoforge"` | mods.toml |
| `forgeVersion` | `neoforgeVersion` | Gradle properties |
| `forge()` | `neoforge()` | Architectury platform |

---

**Document Version**: 1.0
**Last Updated**: 2025-02-05
**Status**: Ready for Review
