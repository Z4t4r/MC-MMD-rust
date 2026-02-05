# Spec: Build System - NeoForge Module Addition

## Capability: Build System

**Context**: The project uses Gradle 8.x with Architectury plugins to build multi-loader Minecraft mod. Current build outputs Fabric and Forge JARs. This spec defines requirements for adding NeoForge build capability.

**Scope**: Gradle configuration, build tasks, dependency management, and artifact generation for NeoForge platform.

---

## ADDED Requirements

### Requirement: NeoForge Gradle Module Configuration

The build system MUST include a complete `neoforge/` module with proper Gradle configuration.

#### Scenario: Module is declared in settings

**Given** the project uses Gradle multi-module structure
**When** `settings.gradle` is processed
**Then** `include 'neoforge'` is present
**And** the module appears in project listing

#### Scenario: Build file uses correct plugins

**Given** `neoforge/build.gradle` exists
**When** Gradle evaluates the build script
**Then** Shadow plugin is applied: `id 'com.gradleup.shadow' version '8.3.6'`
**And** Architectury plugin is configured with `neoforge()` platform
**And** Loom plugin is configured for NeoForge

#### Scenario: NeoForge dependencies are declared

**Given** the module requires NeoForge platform libraries
**When** dependency resolution runs
**Then** NeoForge dependency is present: `neoforge "net.neoforged:neoforge:${neoforge_version}"`
**And** Architectury NeoForge API is included: `modApi("dev.architectury:architectury-neoforge:${architectury_version}")`
**And** dependencies are configured for shadowing

---

### Requirement: NeoForge Version Management

The build system MUST manage NeoForge version through Gradle properties.

#### Scenario: Version property is defined

**Given** `gradle.properties` stores build constants
**When** properties are loaded
**Then** `neoforge_version` property is defined (e.g., `47.1.81`)
**And** version is compatible with Minecraft 1.20.1

#### Scenario: Version ranges are configured

**Given** NeoForge has version-specific API requirements
**When** mod metadata is generated
**Then** loader version range is set to `[47.1,)`
**And** NeoForge dependency uses compatible version range

#### Scenario: Version can be updated

**Given** a new NeoForge version is released
**When** developer updates `neoforge_version` property
**Then** build uses new version without code changes
**And** compatibility is validated through testing

---

### Requirement: NeoForge Build Tasks

The build system MUST provide standard Gradle tasks for NeoForge module.

#### Scenario: Build task produces JAR

**Given** the module is properly configured
**When** `./gradlew :neoforge:build` is executed
**Then** compilation completes without errors
**And** a JAR file is produced in `neoforge/build/libs/`
**And** JAR is named `mmdskin-neoforge-{version}.jar`

#### Scenario: Shadow JAR bundles dependencies

**Given** the module includes shaded dependencies
**When** `shadowJar` task runs
**Then** common module classes are included
**And** Architectury API is bundled
**And** service files are merged
**And** unnecessary files are excluded (e.g., `module-info.class`)

#### Scenario: Remap Jar applies obfuscation

**Given** development uses deobfuscated names
**When** `remapJar` task runs
**Then** classes are remapped to obfuscated names
**And** remapped JAR is the final build artifact
**And** remapping depends on `shadowJar` output

#### Scenario: Source JAR includes all sources

**Given** the module should provide sources for debugging
**When** `sourcesJar` task runs
**Then** NeoForge-specific sources are included
**And** common module sources are merged
**And** duplicate entries are excluded
**And** source JAR is built successfully

---

### Requirement: NeoForge Run Configurations

The build system MUST provide run configurations for development and testing.

#### Scenario: Client run configuration exists

**Given** developers need to test client-side features
**When** `./gradlew :neoforge:runClient` is executed
**When** Minecraft client launches with NeoForge
**And** the mod is loaded in development environment
**And** run directory is set to `run/client`

#### Scenario: Server run configuration exists

**Given** developers need to test server-side features
**When** `./gradlew :neoforge:runServer` is executed
**When** Minecraft server launches with NeoForge
**And** the mod is loaded on server
**And** run directory is set to `run/server`

#### Scenario: Run configurations use correct arguments

**Given** the game launches from Gradle
**When** run configurations are applied
**Then** client uses `--username Player1` for testing
**And** necessary VM args are configured (e.g., for game tests)
**And** working directory is set correctly

---

### Requirement: NeoForge Resource Processing

The build system MUST process and expand variables in NeoForge resources.

#### Scenario: Mods.toml variable expansion

**Given** `neoforge.mods.toml` contains placeholder variables
**When** `processResources` task runs
**Then** `${mod_version}` is replaced with actual version
**And** `${neoforge_version}` is replaced with configured version
**And** `${minecraft_version}` is replaced with `1.20.1`
**And** all other properties are expanded correctly

#### Scenario: Resource inputs are tracked

**Given** incremental builds should work correctly
**When** `processResources` is configured
**Then** input properties are declared
**Then** files matching pattern are tracked
**And** changes to properties trigger resource reprocessing

---

### Requirement: NeoForge Mixin Configuration

The build system MUST configure mixin processing for NeoForge.

#### Scenario: Mixin config is referenced

**Given** the mod uses mixins for bytecode manipulation
**When** Loom is configured for NeoForge
**Then** `mixinConfig "mmdskin-neoforge.mixins.json"` is set
**And** `defaultRefmapName` is set to `"mmdskin-neoforge-refmap.json"`
**And** mixin refmap is generated during build

#### Scenario: Mixin file exists

**Given** the mod defines NeoForge-specific mixins
**When** resources are processed
**Then** `mmdskin-neofixin.mixins.json` is in resources
**And** mixin targets are valid for NeoForge mappings
**And** refmap references are correct

---

### Requirement: NeoForge Dependency Shadowing

The build system MUST shadow certain dependencies into the NeoForge JAR.

#### Scenario: Common module is shadowed

**Given** the common module contains shared code
**When** `shadowJar` task runs
**Then** common module classes are included via `transformProductionNeoForge` configuration
**And** common resources (native libraries, assets) are included
**And** no package conflicts occur

#### Scenario: Architectury API is bundled

**Given** Architectury provides cross-platform APIs
**When** dependencies are resolved
**Then** `architectury-neoforge` is included with `include()` configuration
**And** API is available at runtime without external dependency
**And** Jar-in-Jar metadata is generated

#### Scenario: Cloth Config is external dependency

**Given** Cloth Config is required for configuration UI
**When** mod dependencies are declared
**Then** Cloth Config is declared as `modApi` (external dependency)
**And** Cloth Config is NOT bundled into JAR
**And** users must install Cloth Config separately

---

### Requirement: NeoForge Build Outputs

The build system MUST produce correct artifacts with proper naming.

#### Scenario: Final JAR naming

**Given** the build completes successfully
**When** artifacts are produced
**Then** main JAR is named `mmdskin-neoforge-{version}.jar`
**And** dev JAR is named `mmdskin-neoforge-{version}-dev.jar`
**And** sources JAR is named `mmdskin-neoforge-{version}-sources.jar`

#### Scenario: JAR contents are correct

**Given** the final JAR is built
**When** JAR contents are inspected
**Then** `META-INF/neoforge.mods.toml` is present
**And** `com/shiroha/mmdskin/neoforge/` classes are present
**And** `com/shiroha/mmdskin/common/` classes are shadowed
**And** `assets/mmdskin/` resources are included
**And** native libraries are included for extraction

#### Scenario: JAR manifest is correct

**Given** the JAR includes manifest metadata
**When** `MANIFEST.MF` is inspected
**Then** specification and implementation attributes are set
**And** mod information is included
**And** license file is referenced

---

### Requirement: NeoForge Build Performance

The build system MUST maintain reasonable build times with the additional module.

#### Scenario: Incremental builds work

**Given** only NeoForge module changed
**When** build is executed
**Then** only NeoForge module is recompiled
**And** Fabric and Forge modules are not rebuilt
**And** build time is proportional to changes

#### Scenario: Parallel builds work

**Given** multiple modules need building
**When** `./gradlew build` is executed with `--parallel` flag
**Then** modules build in parallel where possible
**And** build completes faster than sequential build
**And** no race conditions occur

#### Scenario: Build cache is effective

**Given** Gradle build cache is configured
**When** build is executed after cache hit
**Then** cached outputs are reused
**And** build time is significantly reduced
**And** cache keys are correct

---

### Requirement: NeoForge Gradle Plugin Compatibility

The build system MUST use compatible versions of Gradle plugins for NeoForge.

#### Scenario: Architectury plugin supports NeoForge

**Given** the project uses Architectury framework
**When** Architectury plugin is applied
**Then** plugin version supports `neoforge()` platform method
**And** plugin is compatible with configured Gradle version
**And** Loom integration works correctly

#### Scenario: Shadow plugin works with Loom

**Given** both Shadow and Loom plugins are applied
**When** build tasks are executed
**Then** no conflicts between plugins occur
**And** `shadowJar` configuration is compatible with Loom
**And** `remapJar` correctly processes shadowed JAR

---

## MODIFIED Requirements

### Requirement: Multi-Platform Build Matrix

**Previous**: Build system produced Fabric and Forge JARs only.

**Modified**: Build system now produces Fabric, Forge, and NeoForge JARs.

#### Scenario: All platforms build together

**Given** the project supports three platforms
**When** `./gradlew build` is executed
**Then** all three platform modules build successfully
**And** three platform-specific JARs are produced in `build/libs/` directories
**And** build summary shows all modules completed

#### Scenario: Individual platform builds work

**Given** developer wants to build only NeoForge
**When** `./gradlew :neoforge:build` is executed
**Then** only NeoForge module is built
**And** other platforms are not built
**And** build time is minimized

---

### Requirement: Root Build Configuration

**Previous**: Root `build.gradle` configured for Fabric and Forge only.

**Modified**: Root build may need updates to support NeoForge in aggregate tasks.

#### Scenario: Root build tasks include NeoForge

**Given** root project defines aggregate tasks
**When** `./gradlew build` is executed from root
**Then** NeoForge module is included in build
**And** no explicit NeoForge configuration needed in root build file
**And** module dependencies are resolved correctly

---

## REMOVED Requirements

None - this is a pure addition, no existing build requirements are removed.

---

## Implementation Notes

### Dependency Configuration Strategy

When configuring NeoForge dependencies:

1. **Use Architectury transform configurations**:
   - `namedElements` for compilation (deobfuscated names)
   - `transformProductionNeoForge` for final JAR (obfuscated names)

2. **Shadow carefully**:
   - Shadow Architectury API (Jar-in-Jar)
   - Shadow common module (internal code)
   - Don't shadow Cloth Config (external dependency)

3. **Version alignment**:
   - Ensure NeoForge version matches Minecraft 1.20.1
   - Verify Architectury version supports NeoForge
   - Test Cloth Config NeoForge version compatibility

### Build Configuration Checklist

- [ ] Add `include 'neoforge'` to `settings.gradle`
- [ ] Create `neoforge/build.gradle` with proper plugins
- [ ] Configure `architectury { neoforge() }`
- [ ] Set up Loom with NeoForge extension
- [ ] Add NeoForge dependencies
- [ ] Configure `shadowJar` and `remapJar`
- [ ] Create run configurations
- [ ] Set up `processResources` for `neoforge.mods.toml`
- [ ] Configure mixin processing
- [ ] Test build and run tasks

### Testing the Build System

Validate build configuration in this order:

1. **Configuration validation**:
   ```bash
   ./gradlew :neoforge:dependencies --configuration compileClasspath
   ./gradlew :neoforge:properties
   ```

2. **Build test**:
   ```bash
   ./gradlew :neoforge:build
   ```

3. **Run test**:
   ```bash
   ./gradlew :neoforge:runClient
   ```

4. **Artifact verification**:
   ```bash
   jar tf neoforge/build/libs/mmdskin-neoforge-*.jar
   ```

### Common Build Issues and Solutions

**Issue**: Dependency resolution fails for NeoForge
**Solution**: Verify NeoForge Maven repository is in `settings.gradle`

**Issue**: Mixin refmap not generated
**Solution**: Check Loom mixin configuration and refmap name

**Issue**: Shadow JAR too large
**Solution**: Review exclusions in `shadowJar` configuration

**Issue**: Run configuration crashes
**Solution**: Verify NeoForge version matches Minecraft version

---

**Spec Version**: 1.0
**Change ID**: add-neoforge-platform-support
**Status**: Proposed
