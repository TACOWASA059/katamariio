# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-10-24

### Fixed
- **Attached Block Outer Protrusion Runaway (Self-Referential Inflation)**:
  - Fixed an issue in `RemoveBlockPacket.java` where attached blocks were placed relative to directional `surfaceRadius` instead of current AABB collision radius $R$. This caused consecutive block pickups in the same direction to stack outward endlessly, creating ballooning mushroom-shaped protrusions (reaching 40m+ outer radius while AABB lagged at 7m).
  - Attached blocks and living entities now snap strictly to the current collision sphere surface ($R = \text{size} / 2$), with blocks embedded slightly by 20% extent into the shell.
  - Ensured block angular coverage correctly accounts for full 1-meter block dimensions, preventing gaps between Fibonacci bins and allowing the collision AABB to grow smoothly and synchronously with the attached block shell.
- **AABB Growth Hard-Cap Removal**:
  - Removed artificial `baseRadius + 0.8f` clamping in bin radii initialization that previously prevented the collision AABB from expanding beyond ~3.6m.

### Fixed
- **Non-full block (snow layer, carpet, slab, etc.) attachment offset and extent correction**:
  - Dynamically calculates the geometric center offset and effective half-thickness (`extent`) via `KatamariBlockUtils.java` using the block's `VoxelShape` bounds.
  - Eliminated the floating gap where thin blocks (like snow layers, height 0.125) appeared floating ~0.44 blocks above the sphere surface caused by rendering from local corner `(0, 0, 0)` and assuming hardcoded `0.5` block extent.
  - Dynamically updates outer radius bounds (`binMaxRadii`) based on actual block thickness, preventing thin surface blocks from inflating outer collision radius and pushing subsequent blocks outward.
- **Water surface snagging and stutter elimination (smooth water gliding)**:
  - Resolved the root cause of violent 20Hz stuttering and speed drops on water surfaces. Vanilla `LiquidBlock.getCollisionShape` only yields a solid floor (`STABLE_SHAPE`) when the entity's feet are at or above the top of the water block (`pos.getY() + 1.0`). Gravity sinking feet by even 0.001 caused the collision shape to vanish (`Shapes.empty()`), creating a rapid 20Hz "sink -> fall -> collide & pushed up" vibration loop.
  - Added water surface hovering support in `LivingEntityMixin.java` (`travel` HEAD): smoothly lifts the player to the surface (`Math.max(motion.y, 0.04D)`) while on the surface without diving intent, keeping feet above the water threshold.
  - Stabilized vertical movement in `travel` (TAIL): clamps remaining downward motion (`motion.y = 0.0D`) so gravity never drags the ball under the surface while rolling across water.
  - Fixed accidental dive triggers at 20-degree pitch: looking slightly downward while rolling forward no longer disables `canStandOnFluid` or triggers downward dive thrust (`-0.20D`). Diving intent is now restricted to sneaking (Shift key) or steep dive angles (pitch > 50 degrees).
  - Smooth navigation across flowing water: even though flowing water blocks do not generate vanilla collision shapes, water surface hovering prevents sinking, enabling seamless travel across rivers and ocean currents.
  - Enhanced step-up height (`maxUpStep`) in `EntityMixin.java`: dynamically sets player step height to a minimum of `1.25F` (scaling up to `3.0F` based on ball radius) while in ball mode. Overcomes water level drops and steps onto shorelines without colliding or needing to jump.
- **Third-person and spectator rolling direction inversion**:
  - Fixed inverted rolling axis/signs in third-person view and specific camera angles where the ball appeared to roll backward. Corrected rotation calculation in `QuaternionUtils.java` and `KatamariPlayerRenderer.java`.
- **Attached entity (mob) synchronization discrepancy between self and other players**:
  - Fixed vanilla Minecraft entity tracker omitting `ClientboundSetPassengersPacket` for the vehicle entity itself. In `KatamariEntityAttacher.java`, passenger packets are now explicitly sent to the vehicle player's connection upon attaching or releasing entities.
  - Added fallback rendering in `EntityRenderDispatcherMixin.java` to render attached entities directly from synced player data during minor packet delays.
  - Introduced client-to-server rotation sync packet (`C2SPlayerRotationPacket.java`). The server now tracks the true ball rotation quaternion and distributes it via `EntityData` to all tracking clients, completely eliminating desynchronization where mobs appeared at their original positions on the host client while rolling on spectator clients.
- **Boat and vehicle passenger rotation jitter**:
  - Fixed severe block cluster jitter during boat/vehicle turns caused by missing frame interpolation (`partialTicks`). Added `getInterpolatedQuaternionFromEntity` with shortest-path interpolation (`rotLerp`) to smoothly follow vehicle turning.
- **Inability to ascend in water**:
  - Fixed `isAffectedByFluids` disabling vanilla fluid swimming (`jumpInFluid`) in `LivingEntityMixin.java`. Added upward thrust (`0.28D`) on Space key and falling speed limit (`-0.15D`) in fluid.
- **Water diving and surface control**:
  - Allowed natural diving into water by switching `canStandOnFluid` to `false` when sneaking (Shift key) or pitching steeply downwards (> 50 degrees), providing smooth downward propulsion (`-0.28D`).
- **Passenger camera and parent ball jitter when attached**:
  - Removed redundant `passenger.setOldPosAndRot()` calls in `EntityMixin.java` that wiped previous tick positions and broke vanilla's frame interpolation (`Mth.lerp`).
  - In `CameraMixin.java`, synchronized passenger camera position to the parent sphere's frame-interpolated position and rotation per frame (60–144fps), completely eliminating camera shaking. Automatically adjusted third-person camera distance according to parent sphere size.
- **Parent-passenger relationship cleanup on disconnect/death**:
  - Added `KatamariEntityAttacher.onPlayerLogout` and `detachPassenger` to cleanly detach relationships whenever a passenger or vehicle player disconnects, dies, or changes dimensions. Dispatches explicit dismount packets to restore gravity, physics, and AI.
- **Sneak dismount-reattach loop prevention**:
  - Suppressed `shiftKeyDown` in `LocalPlayerMixin.java` while riding as a katamari passenger, preventing unintended dismount and immediate re-attachment loops.

### Added
- **Fibonacci Spherical Grid Collision System**:
  - Replaced latitude/longitude grid (`collisionLatBins` / `collisionLonBins`) with a uniform golden-angle Fibonacci lattice across the sphere surface.
  - Completely eliminated polar singularities and high-latitude cell shrinking, ensuring equal-area sampling for all 360-degree directions.
  - Implemented $O(1)$ fast directional bin lookup via uniform linear $Y$-distribution with local window neighborhood search.
  - Consolidated grid configuration into a single setting: `/katamari config collisionBins <count>` (Range: 16 to 512, default: `64`).
- **Reset Subcommands**:
  - Added `/katamari resetAll <targets>` to clear blocks, release entities, and reset diameter back to default in a single command.
  - Added `/katamari resetEntities <targets>` to safely release only attached entities while preserving collected blocks.
- **3D rotational tracking for attached entities (mobs)**:
  - Implemented `EntityRenderDispatcherMixin.java` to synchronize attached mob render positions and rotations directly with the sphere surface. Mobs roll, flip, and tumble matching the sphere's 3D rotation (ΔQ = ballRot × attachQuat⁻¹).
- **Dynamic reach scaling based on ball size**:
  - Dynamically scales block breaking and interaction reach based on ball radius (`KatamariReach.java`, `ServerGamePacketListenerImplMixin`, `ServerPlayerGameModeMixin`, `LocalPlayerMixin`).
- **Movement physics and collision optimization**:
  - Step height scaling, fall damage negation, and knockback resistance adjustments (`BlockCollisionsMixin.java`, `EntityMixin.java`, `LivingEntityMixin.java`).
- **Command System Reorganization & Clean Separation**:
  - Restructured the `/katamari` command tree with strict, intuitive separation of responsibilities:
    - **Player Commands (`/katamari <subcommand>`)**: Strictly confined to target-player dynamic physics actions (`size`, `ball`, `restitution`, `resetBlocks`, `help`). Querying current player state is supported by omitting value arguments (`/katamari size <targets>`, `/katamari restitution <targets>`, `/katamari ball <targets>`).
    - **Server & World Configuration (`/katamari config <subcommand>`)**: Cleanly consolidated all persistent settings (`config/katamariio.json`), operational modes (`mode`), performance presets (`preset`), attachment toggles, and filters (`filter`) under `/katamari config`. Removed cluttered duplicate shortcuts from the root `/katamari` command.
    - Every configuration option consistently supports querying the current setting when value arguments are omitted (e.g. `/katamari config attachBlocks`, `/katamari config maxSize`, `/katamari config maxAttachedEntities`).
    - Redesigned `/katamari help` into clearly demarcated Player and Server sections.
- **Ball-to-ball attachment option (`attachBalls`)**:
  - Added `/katamari attachBalls [true|false]` and `/katamari config attachBalls [true|false]` (default: `false`).
  - When enabled, only the larger ball can attach the smaller ball upon collision; equal or smaller balls are protected.
- **Attachment while riding option (`attachWhileRiding`)**:
  - Added `/katamari attachWhileRiding [true|false]` and `/katamari config attachWhileRiding [true|false]` (default: `false`).
  - Configures whether players can pick up blocks or attach living entities while riding a vehicle (boats, minecarts, horses) or another entity. When set to `false`, terrain and mobs are safely left untouched while riding.
- **Full Internationalization (i18n) Support (`en_us` & `ja_jp`)**:
  - Eliminated hardcoded literals in commands and system messages across `KatamariCommands.java`.
  - Added native Minecraft language resource files `assets/katamariio/lang/en_us.json` and `assets/katamariio/lang/ja_jp.json` using `Component.translatable`.
  - Command feedback, status summaries, filter listings, and `/katamari help` automatically localize according to each client's selected language settings in real time.
- **Entity Size-Based Attachment Filtering**:
  - Restricts attached entities to only those whose dimensions (max width or height) are smaller than the player's ball diameter (`mySize > Math.max(entity.getBbWidth(), entity.getBbHeight())`).
  - Larger mobs (e.g. Iron Golems, Endermen, Ravagers, Ghasts, Wardens) cannot be attached initially until the katamari grows larger.
- **Unlimited Attached Entity Capacity by Default**:
  - Changed the default value of `maxAttachedEntities` from `16` to `-1` (unlimited).
- **Entity Release Reattachment Prevention (Pop-Out & Cooldown)**:
  - Resolved an issue where releasing attached mobs (via `/katamari resetBlocks`, disabling ball mode, or detachment) caused them to be immediately re-absorbed on the next tick due to remaining inside the ball's bounding box.
  - Implemented outward pop-out mechanics upon release: shifts the entity outside the ball's outer boundary (`radius + entitySize/2 + 0.4`) and applies an outward, slightly upward velocity impulse.
  - Introduced a 3-second (60 ticks) re-attachment cooldown for released entities, allowing them to freely escape without being instantly vacuumed back onto the ball.

---

## [1.1.0] - 2026-07-03

### Performance Overhaul
- **Networking (Delta Sync)**:
  - Introduced delta block synchronization (`S2CKatamariDeltaPacket`). Sends only newly attached blocks to tracking clients instead of the entire block list every tick, with periodic full keyframe sync to correct drift.
- **Storage (Compact Struct-of-Arrays)**:
  - Redesigned `KatamariBlocks` container into Struct-of-Arrays (SoA) layout. Compresses network payload to 16-bit positions (shared scale), 8-bit quaternion rotations, and varint block IDs. Saves NBT using block palettes and primitive byte/int arrays.
- **Pruning (Inner Shell Bounds)**:
  - Automatically prunes hidden inner blocks using radius-scaled angular bins once the ball exceeds a threshold radius, bounding memory and rendering overhead.
- **Rendering (VBO Mesh Baking)**:
  - Bakes collected blocks into a cached VertexBuffer (VBO) for single-draw-call performance with per-block fallback. Pre-caches base sphere geometry per segment count.
- **Collision Optimization**:
  - Skips out-of-sphere grid cells and replaces heavy `VoxelShape.closestPointTo` calls with lightweight AABB clamping. Added tick rate limits for block collection (`maxRemovalsPerTick`).
- **Gameplay Improvements**:
  - Fluid surface walking support (`canStandOnFluid`) and guaranteed minimum block light when buried in blocks.

---

## [1.0.1] - 2026-06-06

### Initial Stable Release
- **Multiloader Support**: Full compatibility with Fabric and Forge for Minecraft 1.20.1.
- **Core Katamari Mechanics**:
  - Smooth spherical rolling physics using quaternions.
  - Terrain block pickup, attachment, and ball scaling.
  - Fall damage immunity and bounce physics with adjustable restitution coefficient.
- **Commands**:
  - `/katamari size`: View or modify ball size.
  - `/katamari ball`: Toggle ball mode on/off.
  - `/katamari resetBlocks`: Clear all attached blocks.
  - `/katamari restitution`: Adjust bounce restitution.
- **Rendering & Optimization**:
  - Back-face culling (Render Culling) and initial renderer implementation.
  - Replaced deprecated registry accesses.
