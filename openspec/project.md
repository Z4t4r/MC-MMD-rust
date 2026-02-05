# Project Context

## Purpose

MC-MMD-rust is a Minecraft mod (version 1.20.1) that implements MikuMikuDance (MMD) model rendering and physics simulation. The project brings anime-style 3D models with real-time physics into Minecraft through a hybrid Rust/Java architecture.

Key goals:
- Load and render PMX/PMD models in Minecraft with full animation support
- Provide real-time physics simulation for hair, clothing, and accessories
- Support VMD animations for bone and morph transformations
- Deliver high-performance GPU skinning via compute shaders
- Enable multi-layer animation blending and mixing

## Tech Stack

### Core Technologies
- **Rust 1.70+**: Physics and animation engine (rust_engine module)
- **Java 17**: Minecraft mod development (Fabric/Forge mod loaders)
- **Gradle 8.x**: Build system for Java modules
- **OpenGL**: Graphics rendering (via Minecraft's rendering pipeline)
- **JNI**: Rust-Java interop bridge

### Key Dependencies
- **Rapier3D**: 3D physics engine for hair and cloth simulation
- **glam**: High-performance 3D math library
- **nalgebra/vek**: Linear algebra utilities
- **mmd-rs**: PMX/VMD file format parser
- **Architectury**: Multi-loader mod framework (Fabric + Forge)

### Graphics & Shaders
- **Compute Shaders**: GPU-accelerated vertex skinning
- **Iris Shader Compatibility**: Works with Minecraft shader packs
- **Custom Shader Pipeline**: Optimized for MMD model rendering

## Project Conventions

### Code Style

#### Rust Code (rust_engine/)
- Follow Rust 2021 edition standards
- Use `rustfmt` for code formatting
- Prefer idiomatic Rust patterns (Result, Option, iterators)
- Error handling with `thiserror` and `anyhow`
- Documentation comments (`///`) for all public APIs
- Use `rayon` for parallel processing where applicable

#### Java Code (fabric/, forge/, common/)
- Follow Java 17 language features
- Use Architectury conventions for cross-loader compatibility
- Cloth Config API for mod settings UI
- Maven/Gradle standard dependency management
- UTF-8 encoding for all source files
- Minecraft obfuscation mappings via official Mojang mappings

### Architecture Patterns

#### Two-Tier Architecture
1. **Rust Engine Layer** (`rust_engine/`)
   - PMX/VMD format parsing
   - Bone hierarchy management
   - Physics simulation (Rapier3D)
   - JNI bindings for Java interop
   - Native library compilation (.dll/.so)

2. **Java Mod Layer** (`fabric/`, `forge/`, `common/`)
   - OpenGL model rendering
   - Compute shader management
   - Game event integration
   - UI/HUD components
   - Resource loading and caching

#### Data Flow
```
Minecraft Game Events
        ↓
Java Mod Layer (event hooks)
        ↓
JNI Bridge
        ↓
Rust Engine (physics/animation)
        ↓
JNI Bridge (updated bone transforms)
        ↓
Java Layer (OpenGL rendering)
```

#### Cross-Loader Pattern
- Common code in `common/` module
- Loader-specific code in `fabric/` and `forge/`
- Architectury plugin for API abstraction

### Testing Strategy

#### Rust Testing
- Unit tests for core physics calculations
- Integration tests for PMX/VMD parsing
- Property-based testing for math operations
- Native library loading validation

#### Java Testing
- Mod loading tests in development environment
- Manual testing via `runClient` task
- Shader compatibility testing (vanilla + Iris)
- Cross-loader testing (Fabric and Forge)

#### Validation Commands
```bash
# Rust tests
cd rust_engine && cargo test

# Java mod build and run
./gradlew build
./gradlew :fabric:runClient  # Fabric testing
./gradlew :forge:runClient   # Forge testing
```

### Git Workflow

#### Branch Strategy
- `main`: Stable development branch
- Feature branches: `add-`, `update-`, `fix-` prefix
- OpenSpec changes: `changes/<change-id>/` for proposals

#### Commit Conventions
- Use Conventional Commits format (optional but recommended)
- Include Co-Authored-By for AI-assisted commits
- Link related issues/PRs when applicable
- Sign commits for security (optional)

#### OpenSpec Integration
- All breaking changes require OpenSpec proposals
- Specs stored in `openspec/specs/<capability>/`
- Active proposals in `openspec/changes/`
- Archive completed changes after deployment

## Domain Context

### MMD Format Specifics
- **PMX Models**: Polygon mesh with bones, morphs, and physics bodies
- **VMD Animations**: Keyframe-based bone and morph animations
- **VPD Pose Files**: Single-frame pose/morph snapshots
- **Physics Rigid Bodies**: Linked to bones for dynamic simulation

### Minecraft Integration
- **Entity Types**: Player, Tamable entities (e.g., Touhou Little Maid)
- **Game States**: Idle, walk, sprint, sneak, swim, sleep, elytra fly, climb
- **Animation Priority**: Model-specific → Custom → Default animations
- **Resource Directory**: `.minecraft/3d-skin/` (auto-created)

### Native Library Loading
- Platform detection at runtime (Windows/Linux/macOS)
- Library extraction from JAR to temp directory
- JNI `System.loadLibrary()` with fallback paths
- Graceful degradation on load failure

## Important Constraints

### Performance Constraints
- Physics simulation must run at 60+ FPS
- GPU skinning via compute shaders for 1000+ bone models
- Memory budget: Model data + textures < 500MB per instance
- JNI call overhead minimization (batch updates where possible)

### Compatibility Constraints
- Minecraft 1.20.1 only (no backports planned)
- Must work with Iris shaders and other rendering mods
- Fabric and Forge loader support required
- Windows 7+ and Linux (macOS best-effort)

### Legal Constraints
- Models and animations provided by users (not bundled)
- Third-party licenses documented in `THIRD_PARTY_LICENSES.md`
- MIT License for project code
- Attributions for referenced projects (KAIMyEntity, Saba, etc.)

## External Dependencies

### Runtime Dependencies
- **Minecraft 1.20.1**: Game platform
- **Fabric Loader** or **Forge**: Mod loader
- **Cloth Config API**: Mod configuration UI (required dependency)
- **Touhou Little Maid** (optional): Additional entity support

### Build-Time Dependencies
- **Rust toolchain**: cargo, rustc, rustfmt
- **JDK 17**: Java development kit
- **Gradle**: Build automation (via Gradle wrapper)
- **Architectury plugins**: Multi-loader tooling

### External Services
- None (offline-first design)
- Maven repositories for dependency resolution
- GitHub for code hosting and releases
