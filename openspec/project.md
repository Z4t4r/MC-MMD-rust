# MC-MMD-rust Project

## Project Overview

**MC-MMD-rust** is a Minecraft mod that implements MikuMikuDance (MMD) model rendering and physics simulation in-game.

**Current Version**: v1.0.2-1.21.1
**Target Minecraft Version**: 1.21.1 (Note: README may incorrectly state 1.20.1)
**License**: MIT

## Technology Stack

### Languages
- **Rust 1.70+**: Physics and animation engine (rust_engine module)
- **Java 21+**: Minecraft mod (Fabric/NeoForge)
- **GLSL**: Shaders for GPU skinning

### Core Dependencies

#### Rust (rust_engine)
- **Rapier3D**: 3D physics engine for rigid body simulation
- **glam**: 3D mathematics library (vectors, matrices, quaternions)
- **mmd-rs**: MMD format parsing (PMX models, VMD animations)
- **JNI**: Java Native Interface bindings for cross-language calls

#### Java (Minecraft Mod)
- **Fabric Loader 0.17.2**: Mod loading platform
- **Fabric API 0.116.4+1.21.1**: Fabric modding API
- **NeoForge 21.1.80**: Alternative modding platform
- **Architectury 13.0.6**: Multi-platform abstraction layer
- **Cloth Config 15.0.130**: Configuration UI

### Build System
- **Gradle 8.x**: Build automation
- **Cargo**: Rust package manager

## Architecture

### High-Level Structure

```
MC-MMD-rust/
├── rust_engine/          # Rust physics/animation engine
│   ├── src/
│   │   ├── animation/    # VMD animation playback
│   │   ├── model/        # PMX model loading
│   │   ├── physics/      # Rapier3D physics simulation
│   │   ├── skeleton/     # Skeletal hierarchy
│   │   ├── skinning/     # Vertex skinning calculations
│   │   ├── morph/        # Morph/blend shape support
│   │   └── jni_bridge/   # Java-Rust interface
│   └── deps/             # Rust dependencies
├── common/               # Shared Java code
│   └── src/main/java/com/shiroha/mmdskin/
│       ├── renderer/     # OpenGL rendering pipeline
│       ├── resource/     # Model/animation loading
│       └── config/       # Configuration system
├── fabric/               # Fabric platform adapter
└── neoforge/             # NeoForge platform adapter
```

### Component Responsibilities

#### rust_engine (Rust)
- **Performance-critical computations**: Physics simulation, skinning
- **Format parsing**: PMX model loading, VMD animation parsing
- **Memory management**: Efficient data structures for animation data
- **JNI interface**: Expose Rust functions to Java layer

#### common (Java)
- **OpenGL integration**: Rendering pipeline, shader management
- **Game integration**: Entity hooks, player model replacement
- **Resource management**: Loading models/textures from disk
- **UI**: Model selection, animation controls, configuration screens
- **Platform abstraction**: Shared code for Fabric/NeoForge

#### fabric / neoforge (Java)
- **Platform-specific initialization**: Mod entry points
- **Mixin injections**: Game code modifications
- **Platform APIs**: Event handling, registry integration

## Key Features

1. **PMX Model Loading**: Load MMD models with textures, bones, and physics
2. **VMD Animation Playback**: Bone and morph animations with timeline control
3. **Physics Simulation**: Real-time hair, clothing, and accessory physics
4. **GPU Skinning**: Compute shader-based vertex skinning for performance
5. **Multi-layer Animation**: Blend multiple animations simultaneously (idle + action)
6. **Model Configuration**: UI for model selection, material visibility, animation slots
7. **Maid Integration**: Touhou Little Maid entity support
8. **Iris Compatibility**: Works with shader mods

## Build Requirements

### Prerequisites
- Rust toolchain 1.70+
- JDK 21+
- Gradle 8.x (included via wrapper)

### Build Commands

```bash
# Build Rust engine
cd rust_engine
cargo build --release

# Build Minecraft mod
./gradlew build

# Build both (from project root)
./gradlew build  # Will build Rust as part of process
```

### Native Libraries

The mod includes precompiled native libraries for:
- **windows-x64**: Windows 64-bit
- **linux-loongarch64**: LoongArch Linux
- **android-arm64**: Android (for potential mobile usage)

## Resource Structure

The mod uses `.minecraft/3d-skin/` as the resource root:

```
.minecraft/3d-skin/
├── EntityPlayer/      # Player models (one per subdirectory)
├── DefaultAnim/       # Built-in animations (idle, walk, etc.)
├── CustomAnim/        # User animations
├── DefaultMorph/      # Built-in morphs
├── CustomMorph/       # User morphs
└── Shader/            # Custom shaders
```

## Recent Changes (v1.0.2)

Based on recent commit history:
- JNI memory safety improvements
- Shader caching optimizations
- UV morph rendering fixes
- Outline alpha rendering fixes
- Configuration hot-reload support
- Stage mode mixin additions
- Network opCode fixes

## Development Workflow

This project uses OpenSpec for managing major changes:
- Proposals are stored in `openspec/changes/<change-id>/`
- Spec deltas track capability changes in `openspec/specs/<capability>/`
- Use proposals for: Features, Refactors, Performance, Architecture changes
- Minor fixes can use standard PR workflow

## Project Maintainers

- **Primary**: Shiroha (project owner)
- **Contributors**: See Git history and ACKNOWLEDGMENTS.md

## Related Projects

- [KAIMyEntity](https://github.com/kjkjkAIStudio/KAIMyEntity): Original Minecraft MMD mod
- [KAIMyEntity-C](https://github.com/Gengorou-C/KAIMyEntity-C): Direct predecessor
- [mmd-rs](https://github.com/aankor/mmd-rs): MMD format parser
- [Rapier](https://rapier.rs): Physics engine

## References

- README.md for user documentation
- LICENSE for licensing terms
- ACKNOWLEDGMENTS.md for full credits
