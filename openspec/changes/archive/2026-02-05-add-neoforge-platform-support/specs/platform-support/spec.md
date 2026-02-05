# Spec: Platform Support - NeoForge Addition

## Capability: Platform Support

**Context**: MC-MMD-rust mod currently supports Fabric and Forge mod loaders for Minecraft 1.20.1. This spec defines requirements for adding NeoForge as a third supported platform.

**Scope**: This spec covers platform-specific code, mod initialization, event handling, and platform integration for NeoForge loader.

---

## ADDED Requirements

### Requirement: NeoForge Platform Module

The project MUST include a `neoforge/` Gradle module that provides NeoForge-specific platform implementation.

#### Scenario: Module structure is created

**Given** the project follows Architectury multi-loader pattern
**When** a new `neoforge/` module is added
**Then** the module structure mirrors the existing `forge/` module:
- `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/` for platform code
- `neoforge/src/main/resources/META-INF/` for mod metadata
- `neoforge/build.gradle` for build configuration

#### Scenario: Module is included in build

**Given** the `neoforge/` module exists
**When** Gradle sync is performed
**Then** the module appears in `./gradlew projects` output
**And** the module builds successfully with `./gradlew :neoforge:build`

---

### Requirement: NeoForge Mod Initialization

The NeoForge module MUST provide a main mod class that follows NeoForge initialization patterns.

#### Scenario: Mod class uses NeoForge APIs

**Given** the `MmdSkinNeoForge` class exists
**When** the mod is loaded by NeoForge
**Then** the class is annotated with `@Mod("mmdskin")` from `net.neoforged.fml.common.Mod`
**And** the class constructor accepts `IEventBus` parameter
**And** all imports use `net.neoforged.*` packages

#### Scenario: Common setup is initialized

**Given** the mod is loading
**When** the `FMLCommonSetupEvent` fires
**Then** the common module initialization is triggered
**And** networking channels are registered
**And** logger confirms initialization completed

#### Scenario: Client-side initialization

**Given** the mod is on physical client
**When** the `FMLClientSetupEvent` fires
**Then** client-specific registrations are performed
**And** renderers are registered
**And** key bindings are initialized

---

### Requirement: NeoForge Event Handling

The NeoForge implementation MUST handle platform-specific events using NeoForge's event system.

#### Scenario: Rendering events are subscribed

**Given** the mod is loaded on client
**When** `RenderLevelStageEvent` fires during rendering
**Then** custom rendering logic is executed
**And** the event stage is checked (e.g., `AFTER_PARTICLES`)

#### Scenario: Tick events are processed

**Given** the game is running
**When** `ClientTickEvent.Post` fires
**Then** client tick logic from common module is executed
**And** physics simulation updates are performed

#### Scenario: Event bus registration

**Given** the mod is initializing
**When** event handlers are registered
**Then** mod-specific events use mod event bus from constructor
**And** game events use `NeoForge.EVENT_BUS`
**And** all event handlers are properly annotated with `@SubscribeEvent`

---

### Requirement: NeoForge Registration System

The NeoForge module MUST use NeoForge's deferred registration system for game objects.

#### Scenario: Deferred register is created

**Given** the mod needs to register game objects
**When** `MmdSkinRegisterCommon` is initialized
**Then** a `DeferredRegister` instance is created
**And** the register is bound to the mod event bus
**And** registry types match NeoForge registry keys

#### Scenario: Common module integration

**Given** the common module defines registry interfaces
**When** platform registration occurs
**Then** NeoForge registration delegates to common module
**And** registry objects are accessible from common code
**And** no duplicate registrations occur

---

### Requirement: NeoForge Configuration System

The NeoForge module MUST integrate with Cloth Config API for mod configuration.

#### Scenario: Config class uses Cloth Config

**Given** the mod needs user-configurable settings
**When** `MmdSkinConfig` class is defined
**Then** it implements `ConfigData` interface
**And** it is annotated with `@Config(name = "mmdskin")`
**And** config fields are annotated with `@ConfigEntry`

#### Scenario: Config screen is accessible

**Given** the mod is loaded
**When** user opens mod config from NeoForge mods screen
**Then** `ModConfigScreen` is displayed
**And** config categories match common module structure
**And** changes are persisted when saved

---

### Requirement: NeoForge Networking

The NeoForge module MUST use NeoForge's modern networking API for packet handling.

#### Scenario: Network channel registration

**Given** the mod needs to send packets between client and server
**When** `RegisterPayloadHandlerEvent` fires during initialization
**Then** a custom payload channel is registered
**And** channel ID is `mmdskin:main`
**And** both client and server handlers are registered

#### Scenario: Client-to-server communication

**Given** a client needs to send data to server
**When** `sendToServer()` is called with a payload
**Then** the payload is sent via `PacketDistributor.SERVER`
**And** the server handler receives and processes the payload

#### Scenario: Server-to-client communication

**Given** the server needs to send data to a specific client
**When** `sendToClient(player, payload)` is called
**Then** the payload is sent via `PacketDistributor.PLAYER`
**And** the client handler receives and processes the payload

---

### Requirement: NeoForge Mod Metadata

The NeoForge module MUST provide correct mod metadata in `neoforge.mods.toml` format.

#### Scenario: Mod loader identifier

**Given** NeoForge mod loading system
**When** `neoforge.mods.toml` is parsed
**Then** `modLoader` is set to `"neoforge"`
**And** `loaderVersion` specifies compatible NeoForge version range `[47.1,)`

#### Scenario: Dependency declarations

**Given** the mod depends on other mods
**When** dependencies are declared in `neoforge.mods.toml`
**Then** `neoforge` dependency is marked mandatory with version range
**And** `minecraft` dependency specifies `[1.20.1,1.21)`
**And** `cloth_config` dependency is marked mandatory for client side

#### Scenario: Variable expansion

**Given** Gradle build process
**When** `processResources` task runs
**Then** variables in `neoforge.mods.toml` are expanded
**And** `${mod_version}` is replaced with actual version
**And** `${neoforge_version}` is replaced with configured version

---

### Requirement: NeoForge Build Configuration

The NeoForge module MUST have proper Gradle configuration for building with NeoForge toolchain.

#### Scenario: Architectury platform configuration

**Given** Architectury framework is used
**When** `neoforge/build.gradle` is processed
**Then** `architectury` block calls `neoforge()`
**And** Loom is configured with `neoforge` extension
**And** mixin config references `mmdskin-neoforge.mixins.json`

#### Scenario: NeoForge dependency declaration

**Given** the module requires NeoForge platform
**When** dependencies are resolved
**Then** `neoforge "net.neoforged:neoforge:${neoforge_version}"` is declared
**And** Architectury API uses `architectury-neoforge` artifact
**And** Cloth Config uses `cloth-config-neoforge` artifact

#### Scenario: Build tasks produce correct outputs

**Given** the build is executed
**When** `./gradlew :neoforge:build` completes
**Then** `mmdskin-neoforge-{version}.jar` is produced
**And** JAR contains `neoforge.mods.toml` in `META-INF/`
**And** JAR includes shadowed common module classes

---

### Requirement: NeoForge Native Library Integration

The NeoForge module MUST correctly load the native Rust library for physics simulation.

#### Scenario: Native library loading

**Given** the mod initializes
**When** native loading code is executed
**Then** `System.loadLibrary("mmdskin_engine")` succeeds
**And** no `UnsatisfiedLinkError` is thrown
**And** physics functions are callable from Java

#### Scenario: Platform-specific library extraction

**Given** the native library is embedded in JAR
**When** the mod loads on different OS (Windows/Linux)
**Then** the correct library variant (`.dll` or `.so`) is extracted
**And** the library is loaded from temp directory
**And** extraction fallback works if direct loading fails

---

### Requirement: NeoForge Compatibility

The NeoForge implementation MUST maintain compatibility with existing project structure and dependencies.

#### Scenario: Common module integration

**Given** the common module contains shared logic
**When** NeoForge module uses common classes
**Then** all common APIs work identically on NeoForge
**And** no code duplication is required
**And** Architectury platform abstractions are used correctly

#### Scenario: Optional dependency support

**Given** Touhou Little Maid is an optional dependency
**When** the maid mod is not present
**Then** the main mod functions normally without maid features
**When** the maid mod is present (NeoForge version)
**Then** maid-specific rendering handlers are activated
**And** no crashes occur due to missing maid classes

#### Scenario: Shader mod compatibility

**Given** Iris shaders or other rendering mods are installed
**When** the mod renders MMD models
**Then** no visual artifacts or conflicts occur
**And** shaders apply correctly to MMD models
**And** performance remains acceptable

---

## MODIFIED Requirements

### Requirement: Platform Support Matrix

**Previous**: The mod supported Fabric and Forge loaders only.

**Modified**: The mod now supports Fabric, Forge, and NeoForge loaders.

#### Scenario: All platforms build successfully

**Given** the project supports three mod loaders
**When** `./gradlew build` is executed
**Then** Fabric, Forge, and NeoForge modules all build successfully
**And** three platform-specific JARs are produced

#### Scenario: Documentation reflects new platform

**Given** the mod supports NeoForge
**When** users read installation documentation
**Then** NeoForge download option is listed
**And** NeoForge installation instructions are provided
**And** compatibility matrix shows all three platforms

---

## REMOVED Requirements

None - this is a pure addition, no existing requirements are removed.

---

## Implementation Notes

### API Migration Key Points

When implementing NeoForge support, pay attention to these API differences from Forge:

1. **Package Renames**: All `net.minecraftforge.*` → `net.neoforged.*`
2. **Event System**: Some event types changed (e.g., `RenderWorldLastEvent` → `RenderLevelStageEvent`)
3. **Networking**: Modern custom payload API replaces legacy `SimpleChannel`
4. **Mod Metadata**: Use `modLoader = "neoforge"` in mods.toml

### Testing Priorities

Test in this order for efficiency:
1. Build configuration (can test without running game)
2. Mod loading (reach main menu)
3. Core functionality (model loading and rendering)
4. Physics simulation (native library integration)
5. Compatibility with other mods

### Rollback Plan

If critical issues arise:
- Remove `include 'neoforge'` from `settings.gradle`
- Delete `neoforge/` directory
- No impact on existing Fabric/Forge functionality

---

**Spec Version**: 1.0
**Change ID**: add-neoforge-platform-support
**Status**: Proposed
