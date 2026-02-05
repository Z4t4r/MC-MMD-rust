# Maid Module Code Analysis

This document describes the structure and code of the Maid integration in the `common/src/main/java/com/shiroha/mmdskin/maid` package. The module integrates with the **TouhouLittleMaid** mod: it binds MMD models to maid entities, allows selecting animations, and syncs state over the network across clients.

---

## 1. Module Overview

### 1.1 Responsibilities

- **Model binding**: One-to-one mapping between maid entity and MMD model name, with lazy loading and caching.
- **Rendering**: Renders the MMD model on the maid entity instead of vanilla; animation state follows entity state (idle, walking, swimming, etc.).
- **UI**: Maid config wheel (from `MaidConfigWheelScreen` in the `ui` package), maid model selector screen, and maid action wheel.
- **Network**: C2S send interfaces for model change and action execution are defined in common; platform-specific implementations are injected by Fabric/Forge.

### 1.2 Files in the Package

| File | Responsibility |
|------|----------------|
| `MaidMMDModelManager.java` | Maid UUID → model name / loaded model mapping and lazy loading |
| `MaidMMDRenderer.java` | Maid MMD model rendering, animation state updates, entity offset |
| `MaidActionNetworkHandler.java` | Abstract maid action network sender (entityId + animId) |
| `MaidModelNetworkHandler.java` | Abstract maid model-change network sender (entityId + modelName) |
| `MaidActionWheelScreen.java` | Maid action selection wheel UI |
| `MaidModelSelectorScreen.java` | Maid MMD model selection list UI |

The entry UI `MaidConfigWheelScreen` lives under `common/.../ui` and opens the two sub-screens above from the wheel.

---

## 2. Core Classes

### 2.1 MaidMMDModelManager

**Path**: `maid/MaidMMDModelManager.java`

**Purpose**: Manages maid-entity UUID ↔ MMD model binding and loaded-model cache.

- **Storage**:
  - `maidModelBindings`: `UUID → String` (model name, i.e. folder name)
  - `loadedModels`: `UUID → MMDModelManager.Model` (lazy-loaded model instance)
- **Main API**:
  - `bindModel(maidUUID, modelName)`: Bind or update; if `modelName` is empty or default, unbind.
  - `unbindModel(maidUUID)`: Unbind and remove from cache.
  - `getBindingModelName(maidUUID)` / `hasMMDModel(maidUUID)`: Query binding and existence.
  - `getModel(maidUUID)`: Get model instance; lazy-loads via `MMDModelManager.GetModel(modelName, "maid_" + uuid)` when not loaded, and sets default `idle` animation.
- **Threading**: Uses `ConcurrentHashMap`; safe for use from render thread and network callbacks.

**Dependencies**: `UIConstants.DEFAULT_MODEL_NAME`, `MMDModelManager`, `MMDAnimManager`, `IMMDModel`.

---

### 2.2 MaidMMDRenderer

**Path**: `maid/MaidMMDRenderer.java`

**Purpose**: Renders the maid entity as an MMD model according to current binding and animation state.

- **Entry**: `render(entity, maidUUID, entityYaw, partialTicks, poseStack, packedLight)`
  - Returns `false` if the maid has no binding or model cannot be obtained (caller falls back to vanilla rendering).
  - Otherwise: load model properties, update animation state, compute offset, call `modelData.model.render(...)`, and return `true` on success.
- **Animation state**: `updateAnimationState` sets `EntityAnimState.State` from entity state (death, sleeping, riding, swimming, climbing, sprinting, walking, idle); calls `ChangeAnim` only when state changes to avoid per-frame switching.
- **Entity offset**: `getEntityTranslation` reads render offset from model property `entityTrans` (`"x,y,z"`).
- **Note**: `setEntityVelocity` is a reserved hook; not implemented (JNI physics velocity).

**Dependencies**: `MaidMMDModelManager`, `MMDModelManager`, `MMDAnimManager`, `EntityAnimState`, `RenderContext`.

---

### 2.3 MaidActionNetworkHandler

**Path**: `maid/MaidActionNetworkHandler.java`

**Purpose**: Provides an abstraction for “send maid action”; actual network implementation is injected by the platform.

- **Interface**: `MaidActionSender = (entityId, animId) -> void`
- **Usage**: `sendMaidAction(entityId, animId)`; if `networkSender` is not set, only logs.
- **Injection**: Fabric/Forge `MmdSkinRegisterClient` calls `setNetworkSender(...)` during client init and injects the real send logic (e.g. Forge `SimpleChannel.sendToServer(...)`).

---

### 2.4 MaidModelNetworkHandler

**Path**: `maid/MaidModelNetworkHandler.java`

**Purpose**: Provides an abstraction for “send maid model change”; implementation is platform-specific.

- **Type**: `BiConsumer<Integer, String>`, i.e. `(entityId, modelName) -> void`
- **Usage**: `sendMaidModelChange(entityId, modelName)`
- **Injection**: Same as above; set by Fabric/Forge RegisterClient at startup.

---

### 2.5 MaidActionWheelScreen

**Path**: `maid/MaidActionWheelScreen.java`

**Purpose**: Maid action selection wheel: reads action list from config, shows a sector wheel, applies animation locally and sends to server for sync on click.

- **Constructor**: `(maidUUID, maidEntityId, maidName)`; action list from `ActionWheelConfig.getDisplayedActions()`.
- **Rendering**: Wheel sectors, dividers, outer ring, center circle, action names; highlights sector under cursor.
- **Interaction**: On mouse release over selected sector, `executeAction(slot)`:
  - Gets local maid model with `MMDModelManager.GetModel("Maid_" + maidUUID)` and calls `ChangeAnim(..., slot.animId)`;
  - Then calls `MaidActionNetworkHandler.sendMaidAction(maidEntityId, slot.animId)` to sync to server/other clients.
- **Dependencies**: `ActionWheelConfig`, `MMDModelManager`, `MMDAnimManager`.

---

### 2.6 MaidModelSelectorScreen

**Path**: `maid/MaidModelSelectorScreen.java`

**Purpose**: Maid MMD model selector: list of all available models (including “use vanilla rendering”); click to bind and sync.

- **Constructor**: `(maidUUID, maidEntityId, maidName)`; current binding from `MaidMMDModelManager.getBindingModelName(maidUUID)`.
- **Data**: `ModelInfo.scanModels()` for all models, plus `UIConstants.DEFAULT_MODEL_NAME` for vanilla.
- **Interaction**: Clicking a card calls `selectModel(card)`:
  - `MaidMMDModelManager.bindModel(maidUUID, card.displayName)`;
  - `MaidModelNetworkHandler.sendMaidModelChange(maidEntityId, card.displayName)`;
  - `MMDModelManager.onModelSwitch()` triggers switch and cache cleanup.
- **Dependencies**: `MaidMMDModelManager`, `MaidModelNetworkHandler`, `ModelInfo`, `UIConstants`.

---

## 3. Data Flow and Call Graph

### 3.1 Opening Maid Config (platform layer)

1. Player looks at maid and presses the configured key (e.g. B) → Fabric/Forge detects raycast hit in client tick.
2. Check if entity is a TouhouLittleMaid maid by class name (e.g. `EntityMaid` / `touhoulittlemaid`).
3. Open `MaidConfigWheelScreen(maidUUID, entity.getId(), maidName, keyCode)`.
4. From wheel, “Model switch” → `openMaidModelSelector()` → `MaidModelSelectorScreen`.
5. From wheel, “Action select” → `openMaidActionWheel()` → `MaidActionWheelScreen`.

### 3.2 Model Binding and Sync

- **Select model** (`MaidModelSelectorScreen`):  
  `bindModel(uuid, name)` → local `loadedModels` may be replaced/cleared → `sendMaidModelChange(entityId, name)` → server forwards → other clients in packet handler call `MaidMMDModelManager.bindModel(maidEntity.getUUID(), modelName)` (see Forge `MmdSkinNetworkPack`).
- **Rendering** (platform, e.g. Forge `MaidRenderEventHandler`):  
  If `MaidMMDModelManager.hasMMDModel(uuid)` then call `MaidMMDRenderer.render(...)`; if it returns `true`, `event.setCanceled(true)` to skip vanilla render.

### 3.3 Action Execution and Sync

- **Select action** (`MaidActionWheelScreen`):  
  Local `ChangeAnim` + `MaidActionNetworkHandler.sendMaidAction(entityId, animId)`.  
  After server forwards, other clients in packet handling should resolve maid entity and anim ID and run the same logic (e.g. apply same animation to maid).

---

## 4. Platform Integration

### 4.1 Common layer

- No Fabric/Forge API; only Minecraft and project common (render/config/UI).
- Network layer only defines “send” interfaces; platforms inject implementations at startup.

### 4.2 Fabric

- `MmdSkinRegisterClient`: `setNetworkSender` injects `MaidModelNetworkHandler` and `MaidActionNetworkHandler` (using Fabric `ClientPlayNetworking.send`).
- Maid detection and opening `MaidConfigWheelScreen` happen in client tick or key handler.

### 4.3 Forge

- `MmdSkinRegisterClient`: Same injection of both handlers (using Forge `SimpleChannel.sendToServer`).
- `MaidRenderEventHandler`: Subscribes to `RenderLivingEvent.Pre`; if entity is maid and `MaidMMDModelManager.hasMMDModel(uuid)`, calls `MaidMMDRenderer.render` and `setCanceled(true)` on success.
- Packet decode (e.g. `MmdSkinNetworkPack`): Parse maid entity and model name / anim ID from opCode, then call `MaidMMDModelManager.bindModel` or the corresponding action handler.

---

## 5. Dependency Overview

```
MaidConfigWheelScreen (ui)
    ├── MaidModelSelectorScreen
    │       ├── MaidMMDModelManager
    │       ├── MaidModelNetworkHandler
    │       └── ModelInfo, UIConstants
    └── MaidActionWheelScreen
            ├── MaidMMDModelManager (GetModel "Maid_"+uuid)
            ├── MaidActionNetworkHandler
            └── ActionWheelConfig, MMDAnimManager

MaidMMDRenderer
    └── MaidMMDModelManager, MMDModelManager, EntityAnimState, MMDAnimManager

Fabric/Forge RegisterClient  →  setNetworkSender(MaidModelNetworkHandler, MaidActionNetworkHandler)
Fabric/Forge NetworkPack receive  →  MaidMMDModelManager.bindModel / action handling
Forge MaidRenderEventHandler  →  MaidMMDModelManager.hasMMDModel, MaidMMDRenderer.render
```

---

## 6. Summary

- The **maid** package implements MMD replacement rendering, model selection, and action selection for TouhouLittleMaid maids, with platform-agnostic network interfaces.
- **MaidMMDModelManager** is the single source for bindings and cache; **MaidMMDRenderer** handles a single render and state; the two **NetworkHandler**s only expose send APIs, with Fabric/Forge injecting implementations and performing `bindModel` and action sync on receive, so maid MMD appearance stays consistent in multiplayer.
