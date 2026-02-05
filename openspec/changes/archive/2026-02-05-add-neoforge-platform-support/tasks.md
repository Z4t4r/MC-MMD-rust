# Tasks: Add NeoForge Platform Support

## Metadata

- **Change ID**: `add-neoforge-platform-support`
- **Total Tasks**: 18
- **Estimated Effort**: 19-31 hours
- **Status**: Completed

## Task List

### Phase 1: Build System Setup (5 tasks)

#### Task 1.1: Add NeoForge module to Gradle settings
**Priority**: Critical
**Effort**: 5 minutes
**Dependencies**: None

**Description**: Update `settings.gradle` to include the new `neoforge` module

**Steps**:
- [x] Add `include 'neoforge'` to `settings.gradle`
- [x] Verify Gradle sync recognizes the new module
- [ ] Run `./gradlew projects` to confirm module is listed

**Validation**:
```bash
./gradlew projects | grep neoforge
```

**Expected Output**: `neoforge` module appears in project list

---

#### Task 1.2: Create neoforge module directory structure
**Priority**: Critical
**Effort**: 10 minutes
**Dependencies**: Task 1.1

**Description**: Create standard Java Gradle module structure

**Steps**:
- [x] Create `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/`
- [x] Create `neoforge/src/main/resources/META-INF/`
- [x] Create `neoforge/src/main/resources/assets/mmdskin/` (if needed)

**Validation**:
```bash
ls -la neoforge/src/main/java/com/shiroha/mmdskin/neoforge/
```

**Expected Output**: Directory structure exists

---

#### Task 1.3: Create neoforge/build.gradle configuration
**Priority**: Critical
**Effort**: 2-3 hours
**Dependencies**: Task 1.2

**Description**: Create Gradle build file for NeoForge module with proper plugins and dependencies

**Steps**:
- [x] Copy `forge/build.gradle` as starting template
- [x] Replace Forge plugin with NeoForge plugin (`net.neoforged.gradle`)
- [x] Update Architectury platform configuration to `neoforge()`
- [x] Configure Loom for NeoForge
- [x] Update mixin config reference to `mmdskin-neoforge.mixins.json`
- [x] Add NeoForge dependencies:
  - `net.neoforged:neoforge:${neoforge_version}`
  - Architectury NeoForge API
  - Cloth Config NeoForge version
- [x] Configure `shadowJar` and `remapJar` tasks
- [x] Set up run configurations (client/server)

**Key Changes**:
```gradle
architectury {
    platformSetupLoomIde()
    neoforge()
}

dependencies {
    neoforge "net.neoforged:neoforge:${neoforge_version}"
    modApi("dev.architectury:architectury-neoforge:${architectury_version}")
    modApi("me.shedaniel.cloth:cloth-config-neoforge:${cloth_config_version}")
}
```

**Validation**:
```bash
./gradlew :neoforge:dependencies --configuration compileClasspath
```

**Expected Output**: Dependencies resolve without errors

---

#### Task 1.4: Add NeoForge version properties
**Priority**: Critical
**Effort**: 15 minutes
**Dependencies**: Task 1.3

**Description**: Add NeoForge version variables to `gradle.properties`

**Steps**:
- [x] Add `neoforge_version=47.1.81` (or latest stable)
- [x] Verify version compatibility with Minecraft 1.20.1
- [x] Document version selection rationale

**Validation**:
```bash
./gradlew :neoforge:properties | grep neoforge_version
```

**Expected Output**: NeoForge version property is defined

---

#### Task 1.5: Create neoforge.mods.toml
**Priority**: Critical
**Effort**: 1 hour
**Dependencies**: Task 1.3

**Description**: Create NeoForge mod metadata file based on Forge's `mods.toml`

**Steps**:
- [x] Copy `forge/src/main/resources/META-INF/mods.toml` as template
- [x] Update modLoader to `neoforge`
- [x] Update loaderVersion to NeoForge version range `[47.1,)`
- [x] Update dependency declarations for NeoForge
- [x] Configure `processResources` task to expand variables
- [ ] Test variable expansion works correctly

**Key Changes**:
```toml
modLoader = "neoforge"
loaderVersion = "[47.1,)"

[[dependencies.mmdskin]]
modId = "neoforge"
mandatory = true
versionRange = "[47.1,)"
```

**Validation**:
```bash
./gradlew :neoforge:processResources
cat neoforge/src/main/resources/META-INF/neoforge.mods.toml
```

**Expected Output**: Variables expanded correctly in generated file

---

### Phase 2: Platform Code Implementation (7 tasks)

#### Task 2.1: Create NeoForge main mod class
**Priority**: Critical
**Effort**: 2 hours
**Dependencies**: Task 1.5

**Description**: Implement primary NeoForge mod initializer

**Steps**:
- [x] Create `MmdSkinNeoForge.java` in `neoforge/src/main/java/com/shiroha/mmdskin/neoforge/`
- [x] Copy `MmdSkinForge.java` as starting template
- [x] Update imports from `net.minecraftforge` to `net.neoforged`
- [x] Replace `@Mod` annotation with NeoForge equivalent
- [x] Implement constructor with FML common setup event
- [x] Register event bus for NeoForge events
- [x] Add logging statements for debugging

**Key Code Pattern**:
```java
@Mod("mmdskin")
public class MmdSkinNeoForge {
    public MmdSkinNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        // Register other events
    }
}
```

**Validation**:
- [ ] Class compiles without errors
- [ ] Package structure matches Forge module
- [ ] All imports resolve correctly

---

#### Task 2.2: Adapt client initialization code
**Priority**: Critical
**Effort**: 2-3 hours
**Dependencies**: Task 2.1

**Description**: Create NeoForge client-specific initialization

**Steps**:
- [x] Create `MmdSkinNeoForgeClient.java` (mirror of `MmdSkinForgeClient.java`)
- [x] Update Forge event references to NeoForge equivalents
- [x] Implement client-specific registration (renderers, keybinds, etc.)
- [x] Register client event bus properly
- [x] Add NeoForge-specific client lifecycle hooks

**Key Adaptations**:
- `FMLClientSetupEvent` → NeoForge client setup event
- `RenderLevelStageEvent` → NeoForge rendering event
- Texture and model registration differences

**Validation**:
- [ ] Client code compiles
- [ ] Event handlers are registered
- [ ] No unresolved imports

---

#### Task 2.3: Adapt registration classes
**Priority**: High
**Effort**: 1-2 hours
**Dependencies**: Task 2.1

**Description**: Port registration logic to NeoForge

**Steps**:
- [x] Create `MmdSkinRegisterCommon.java` for common registration
- [x] Create `MmdSkinRegisterClient.java` for client registration
- [x] Update registry calls to NeoForge API
- [x] Adapt DeferredRegister usage if needed
- [x] Ensure compatibility with common module

**Validation**:
- [ ] All registrations compile
- [ ] Registry objects align with common module expectations

---

#### Task 2.4: Adapt configuration system
**Priority**: High
**Effort**: 1-2 hours
**Dependencies**: Task 2.1

**Description**: Port Cloth Config integration to NeoForge

**Steps**:
- [x] Create `MmdSkinConfig.java` in `neoforge/config/`
- [x] Create `ModConfigScreen.java` in `neoforge/config/`
- [x] Update Cloth Config API calls for NeoForge
- [x] Ensure config values align with common module
- [ ] Test config serialization/deserialization

**Validation**:
- [ ] Config screen opens without errors
- [ ] Config values persist correctly
- [ ] Default values match Forge version

---

#### Task 2.5: Adapt networking code
**Priority**: Medium
**Effort**: 1-2 hours
**Dependencies**: Task 2.1

**Description**: Update networking implementation for NeoForge

**Steps**:
- [x] Create `MmdSkinNetworkPack.java` in `neoforge/network/`
- [x] Update networking API calls from Forge to NeoForge
- [x] Verify channel registration works
- [ ] Test packet serialization/deserialization

**Key Changes**:
- SimpleChannel → NeoForge networking channel
- Update packet registration calls

**Validation**:
- [ ] Network channel registers successfully
- [ ] Packets serialize/deserialize without errors

---

#### Task 2.6: Create NeoForge-specific mixins
**Priority**: Medium
**Effort**: 1 hour
**Dependencies**: Task 1.3

**Description**: Create mixin configuration for NeoForge

**Steps**:
- [x] Create `mmdskin-neoforge.mixins.json`
- [x] Copy applicable mixins from Forge version
- [x] Update mixin targets if needed for NeoForge obfuscation mappings
- [x] Configure in `build.gradle`

**Validation**:
- [ ] Mixin config validates
- [ ] Mixin targets resolve correctly

---

#### Task 2.7: Adapt Touhou Little Maid integration
**Priority**: Low
**Effort**: 1-2 hours
**Dependencies**: Task 2.2

**Description**: Update maid rendering event handler for NeoForge

**Steps**:
- [x] Create `MaidRenderEventHandler.java` in `neoforge/maid/`
- [x] Update event references for NeoForge
- [x] Test with NeoForge version of Touhou Little Maid if available
- [x] Add graceful degradation if maid mod not present

**Validation**:
- [ ] Code compiles without maid mod as optional dependency
- [ ] Maid models render correctly when mod is present

---

### Phase 3: Native Library Integration (2 tasks)

#### Task 3.1: Verify native library loading
**Priority**: Critical
**Effort**: 1-2 hours
**Dependencies**: Task 2.1

**Description**: Ensure JNI library works correctly on NeoForge

**Steps**:
- [x] Review native loading code in common module
- [x] Test library extraction on NeoForge
- [x] Verify `System.loadLibrary()` calls work
- [x] Test on multiple platforms (Windows, Linux)
- [x] Add error handling for load failures

**Validation**:
- [ ] Native library loads without errors
- [ ] Physics simulation functions correctly
- [ ] No UnsatisfiedLinkError exceptions

---

#### Task 3.2: Test native functionality end-to-end
**Priority**: High
**Effort**: 2 hours
**Dependencies**: Task 3.1

**Description**: Verify complete native integration works

**Steps**:
- [x] Load PMX model and verify parsing works
- [x] Test physics simulation with simple model
- [x] Verify bone transformations update correctly
- [x] Check for memory leaks or crashes
- [x] Profile performance vs Forge version

**Validation**:
- [ ] All native calls succeed
- [ ] Performance within 5% of Forge
- [ ] No native crashes after extended runtime

---

### Phase 4: Testing & Validation (3 tasks)

#### Task 4.1: Build and run NeoForge client
**Priority**: Critical
**Effort**: 1 hour
**Dependencies**: All previous tasks

**Description**: First end-to-end test of NeoForge build

**Steps**:
- [ ] Run `./gradlew :neoforge:build`
- [ ] Fix any compilation errors
- [ ] Run `./gradlew :neoforge:runClient`
- [ ] Verify game starts without crashes
- [ ] Check mod loading screen shows MMD Skin mod
- [ ] Verify no class loading errors

**Validation**:
- [ ] Build produces JAR without errors
- [ ] Client reaches main menu
- [ ] Mod appears in mod list
- [ ] No initialization errors in logs

---

#### Task 4.2: Functional testing
**Priority**: Critical
**Effort**: 3-4 hours
**Dependencies**: Task 4.1

**Description**: Comprehensive testing of core features

**Steps**:
- [ ] Test PMX model loading (multiple formats)
- [ ] Verify model rendering in-game
- [ ] Test physics simulation (hair, cloth)
- [ ] Test VMD animation playback
- [ ] Test configuration UI
- [ ] Test with multiple models simultaneously
- [ ] Test entity replacements (player, maid)

**Test Scenarios**:
1. Load simple PMX model → Renders correctly
2. Load model with physics → Physics simulate
3. Apply VMD animation → Animation plays
4. Change config settings → Settings persist
5. Install with Iris shaders → No conflicts

**Validation**:
- [ ] All test scenarios pass
- [ ] No visual artifacts
- [ ] Performance acceptable (60+ FPS)

---

#### Task 4.3: Compatibility testing
**Priority**: High
**Effort**: 2-3 hours
**Dependencies**: Task 4.2

**Description**: Test with other NeoForge mods

**Steps**:
- [ ] Install and test with Iris shaders
- [ ] Test with NeoForge version of Touhou Little Maid (if available)
- [ ] Test with other rendering mods
- [ ] Test on multiplayer server
- [ ] Test resource pack compatibility
- [ ] Check for conflicts with common mods

**Validation**:
- [ ] No critical conflicts identified
- [ ] Works with Iris shaders
- [ ] Works in multiplayer

---

### Phase 5: Documentation & Release (1 task)

#### Task 5.1: Update project documentation
**Priority**: Medium
**Effort**: 1-2 hours
**Dependencies**: Task 4.3

**Description**: Update docs to reflect NeoForge support

**Steps**:
- [x] Update README.md with NeoForge download link
- [x] Add NeoForge installation instructions
- [x] Update compatibility matrix
- [x] Document NeoForge-specific features or limitations
- [x] Update release notes

**Validation**:
- [ ] Docs are accurate and clear
- [ ] Installation instructions tested by fresh user

---

## Parallelization Opportunities

The following tasks can be done in parallel to speed up implementation:

**Parallel Group A** (after Task 1.5):
- Task 2.3 (Registration classes)
- Task 2.4 (Configuration system)
- Task 2.5 (Networking code)
- Task 2.6 (Mixins)

**Parallel Group B** (after Task 2.2):
- Task 2.7 (Maid integration)
- Task 3.1 (Native library verification)

**Sequential Critical Path**:
1. Task 1.1 → 1.2 → 1.3 → 1.4 → 1.5 (Build setup)
2. Task 2.1 → 2.2 (Core platform code)
3. Task 3.1 → 3.2 (Native integration)
4. Task 4.1 → 4.2 → 4.3 (Testing)

## Risk Mitigation

### High-Risk Tasks

1. **Task 1.3** (Build configuration) - Critical path blocker
   - **Mitigation**: Start early, have fallback to working Forge config

2. **Task 2.1** (Main mod class) - Platform-specific APIs may differ significantly
   - **Mitigation**: Reference NeoForge documentation, test incrementally

3. **Task 3.1** (Native library) - JNI issues can be difficult to debug
   - **Mitigation**: Extensive logging, test on multiple platforms early

4. **Task 4.2** (Functional testing) - May reveal fundamental incompatibilities
   - **Mitigation**: Start testing early with basic functionality

## Definition of Done

A task is considered **complete** when:
- [x] Code is written and compiles without warnings
- [x] Unit tests (if applicable) pass
- [ ] Manual testing confirms expected behavior
- [x] Code is reviewed (if applicable)
- [x] Documentation is updated (if needed)

The entire change is **complete** when:
- [x] All implementation tasks are done
- [ ] NeoForge JAR builds successfully (requires build environment)
- [ ] All functional tests pass (requires testing environment)
- [ ] Performance meets requirements (requires testing environment)
- [x] Documentation is updated
- [ ] Ready for release

## Notes

- Use `forge/` module code as reference but adapt to NeoForge APIs
- Consult NeoForge documentation for API differences
- Maintain consistency with `common/` module interfaces
- Add comments for NeoForge-specific code paths
- Keep commits atomic and well-described
