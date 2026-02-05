# Proposal: Add NeoForge Platform Support

## Metadata

- **Change ID**: `add-neoforge-platform-support`
- **Status**: ExecutionCompleted
- **Created**: 2025-02-05
- **Completed**: 2025-02-05
- **Author**: AI Assistant (shiroha233)
- **Branch**: `feat/neo-forge`

## Overview

Add support for the NeoForge mod loader to MC-MMD-rust, expanding platform compatibility from Fabric and Forge to include NeoForge. NeoForge is a modern fork of Forge (for Minecraft 1.20.2+) with improved APIs, better performance, and active community development.

### Current State

The project currently supports two mod loaders:
- **Fabric**: Implemented in `fabric/` module
- **Forge**: Implemented in `forge/` module
- **Common code**: Shared logic in `common/` module using Architectury framework

### Problem Statement

1. **Incomplete Platform Coverage**: NeoForge users cannot use MC-MMD-rust, limiting potential user base
2. **Build System Gap**: No Gradle configuration for NeoForge module
3. **Loader Adaptation Missing**: NeoForge has API differences from Forge requiring specific adaptations
4. **Dependency Configuration**: NeoForge uses different dependency declarations and version ranges
5. **Build Output**: Need to generate standalone NeoForge JAR file
6. **Platform-Specific Logic**: Some code paths may need NeoForge-specific handling

### Proposed Solution

Add a new `neoforge/` submodule that mirrors the `forge/` structure while adapting to NeoForge-specific APIs and conventions. The implementation will:

1. Create `neoforge/` module with proper Gradle configuration using NeoForge plugins
2. Implement platform-specific initialization, event listeners, and rendering integration
3. Reuse existing `common/` module code through Architectury framework
4. Generate `mmdskin-neoforge-{version}.jar` as build output
5. Maintain compatibility with existing dependencies (Cloth Config, Architectury API, Touhou Little Maid)

## Scope

### In Scope

1. **Build Configuration**
   - Add `neoforge` module to `settings.gradle`
   - Create `neoforge/build.gradle` with NeoForge Gradle plugin
   - Configure NeoForge-specific `neoforge.mods.toml`
   - Update root `build.gradle` if needed for multi-platform support

2. **Platform Implementation**
   - Create `neoforge/src/main/java/` source tree
   - Implement NeoForge mod initializer class
   - Adapt Forge-specific code to NeoForge APIs
   - Implement platform-specific event handlers and registration

3. **Dependencies**
   - Configure NeoForge dependencies (`net.neoforged:neoforge`)
   - Update Architectury API to NeoForge-compatible version
   - Configure Cloth Config API for NeoForge
   - Handle Touhou Little Maid optional dependency

4. **Native Library Loading**
   - Verify JNI library loading mechanism works on NeoForge
   - Adapt platform-specific native loading code if needed

5. **Testing & Validation**
   - Verify client startup with `./gradlew :neoforge:runClient`
   - Test core functionality (model loading, rendering, physics)
   - Validate compatibility with Iris shaders and dependencies

### Out of Scope

1. **Backports to older Minecraft versions** (1.19.x, 1.18.x, etc.)
2. **New features or functionality changes** (platform addition only)
3. **Performance optimizations** beyond what NeoForge provides
4. **Refactoring existing Forge/Fabric code** unless necessary for compatibility
5. **Documentation updates** (will be handled separately after implementation)

### Assumptions

1. NeoForge API is sufficiently similar to Forge that most code can be adapted with minimal changes
2. Architectury framework provides adequate abstraction for cross-platform compatibility
3. JNI library loading mechanism is consistent between Forge and NeoForge
4. NeoForge 47.1.x+ for Minecraft 1.20.1 is stable and production-ready

## Impact Assessment

### Benefits

1. **User Base Expansion**: Reach NeoForge users who have migrated from Forge
2. **Ecosystem Compatibility**: Better integration with NeoForge mod ecosystem
3. **Future-Proofing**: NeoForge represents the future direction of Minecraft modding
4. **Architecture Consistency**: Leverages existing Architectury framework for high code reuse

### Risks

1. **Maintenance Complexity**: Supporting three platforms increases testing and compatibility workload
2. **API Differences**: NeoForge has breaking changes from Forge requiring careful adaptation
3. **Build Time**: Additional module increases overall build duration
4. **Dependency Compatibility**: Some dependencies may need updates for NeoForge support

### Mitigation Strategies

1. **Code Reuse**: Maximize shared code in `common/` module to minimize duplication
2. **Automated Testing**: Implement cross-platform tests to catch regressions early
3. **Incremental Implementation**: Start with basic platform support, then add features
4. **Community Testing**: Release beta versions to gather NeoForge user feedback

## Success Criteria

### Functional Requirements

- [ ] NeoForge JAR builds successfully with `./gradlew :neoforge:build`
- [ ] NeoForge client starts without errors with `./gradlew :neoforge:runClient`
- [ ] PMX/PMD models load and render correctly on NeoForge
- [ ] Physics simulation works as expected on NeoForge
- [ ] Configuration UI (Cloth Config) functions properly
- [ ] Native library (Rust engine) loads successfully

### Compatibility Requirements

- [ ] Works with NeoForge 47.1.x+ on Minecraft 1.20.1
- [ ] Compatible with Iris shaders on NeoForge
- [ ] Compatible with Touhou Little Maid (NeoForge version) if available
- [ ] No conflicts with other NeoForge mods

### Build Requirements

- [ ] `neoforge` module included in Gradle build
- [ ] Outputs `mmdskin-neoforge-{version}.jar` with correct filename
- [ ] Source JAR includes both neoforge and common sources
- [ ] Build passes all validation checks

### Performance Requirements

- [ ] FPS impact within 5% of Forge version
- [ ] Physics simulation maintains 60+ FPS
- [ ] Memory usage comparable to Forge version

## Alternatives Considered

### Alternative 1: Drop Forge Support, Replace with NeoForge

**Description**: Remove Forge module and only support Fabric + NeoForge

**Pros**:
- Reduced maintenance complexity (2 platforms instead of 3)
- NeoForge is the future, Forge is legacy

**Cons**:
- Existing Forge users would be forced to migrate
- Forge still has significant user base
- Breaking change for current users

**Decision**: **Rejected** - Maintaining Forge support is important for existing users

### Alternative 2: Use Multi-Loader Plugin (FML-Library-Like)

**Description**: Use a plugin to generate single JAR that works on both Forge and NeoForge

**Pros**:
- Single build artifact
- Simpler release process

**Cons**:
- Complex build configuration
- Limited community support
- May not work well with Architectury

**Decision**: **Rejected** - Technical complexity outweighs benefits, better to have separate JARs

### Alternative 3: Delay NeoForge Support Until Later

**Description**: Wait until NeoForge adoption is more widespread

**Pros**:
- Focus on core features
- Let NeoForge ecosystem mature

**Cons**:
- Miss early adopter market
- Play catch-up later
- User demand exists now

**Decision**: **Rejected** - Current user demand and strategic importance justify immediate implementation

## Implementation Estimate

### Effort Breakdown

- **Build Configuration**: 2-4 hours
- **Platform Code Adaptation**: 8-12 hours
- **Dependency Configuration**: 2-3 hours
- **Testing & Debugging**: 6-10 hours
- **Documentation Updates**: 1-2 hours

**Total Estimate**: 19-31 hours

### Dependencies

1. **Architectury NeoForge support** - Must verify Architectury version has NeoForge integration
2. **NeoForge Gradle plugin** - Must be stable and documented
3. **Cloth Config NeoForge** - Must have compatible version available

### Blocking Issues

None identified - all required dependencies are available.

## Related Changes

### Linked Proposals

None - this is a standalone platform addition

### Referenced Specs

- `openspec/specs/build-system/` - Build and release specifications (if exists)
- `openspec/specs/platform-support/` - Platform compatibility requirements (if exists)

## Approval

- [ ] **Technical Lead**: Architecture review and approval
- [ ] **Project Maintainer**: Final approval and merge decision

## Appendix

### NeoForge vs Forge Key Differences

1. **Package Names**: `net.neoforged.*` instead of `net.minecraftforge.*`
2. **Mod Loading**: Uses NeoForge-specific mod loading system
3. **Event System**: Some events have different signatures or names
4. **Dependency Format**: Uses `neoforge.mods.toml` instead of `mods.toml`
5. **Version Ranges**: Different version scheme (47.1.x+ for 1.20.1)

### References

- [NeoForge Documentation](https://docs.neoforged.net/)
- [Architectury NeoForge Support](https://github.com/ArchitecturyDev/Architectury)
- [NeoForge Gradle Plugin](https://github.com/neoforged/NeoGradle)

### Migration Notes

Key migration steps from Forge to NeoForge:
1. Update imports from `net.minecraftforge` to `net.neoforged`
2. Replace `@Mod` annotation with NeoForge equivalent
3. Update event bus registration
4. Adapt `mods.toml` to `neoforge.mods.toml` format
5. Update Gradle plugin and dependencies
6. Test all platform-specific code paths
