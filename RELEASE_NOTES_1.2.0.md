# KatamariIO 1.2.0 — Changes since 1.0.1

For Minecraft 1.21.1 (Fabric, Forge and NeoForge).

## Added

- Entity attachment: the katamari can pick up living entities smaller than the ball. Attached entities move and rotate with its surface.
- Controls for entity attachment, including size filtering and a maximum attachment count. The default maximum is now unlimited.
- Optional attachment between katamari balls. Only a larger ball can attach a smaller one.
- An `attachWhileRiding` option to control whether blocks and entities can be collected while riding a vehicle or another entity.
- `/katamari resetEntities` to release attached entities without clearing collected blocks, and `/katamari resetAll` to reset blocks, entities, and ball size together.
- A reorganized `/katamari config` command for persistent settings, modes, presets, attachment options, and filters. Commands can also display their current values.
- English and Japanese translations for commands, help text, and status messages.
- Block breaking and interaction reach that scales with ball size.

## Changed

- Replaced the latitude/longitude collision grid with a Fibonacci sphere grid for more even collision coverage. The number of collision bins is configurable.
- Improved collision and movement behavior as the ball grows, including step height, fall damage, and knockback handling.
- Released entities are moved outside the ball and cannot be attached again for three seconds.

## Performance

- Added delta synchronization for collected blocks, with periodic full synchronization.
- Reworked collected-block storage and NBT serialization to reduce data size.
- Added pruning of hidden inner blocks and cached mesh rendering for collected blocks.
- Reduced collision work and limited how many blocks can be collected per tick.

## Fixed

- Fixed repeated block pickups forming protrusions far beyond the ball's collision boundary. Attached blocks and entities now stay aligned with the collision sphere.
- Fixed gaps and incorrect collision growth when attaching thin or partial blocks such as snow layers, carpets, and slabs.
- Fixed stuttering and speed loss while rolling over water, improved travel across flowing water, and corrected swimming, diving, and surfacing controls.
- Fixed the ball rolling in the wrong direction from some third-person and spectator camera angles.
- Fixed attached entities appearing at different positions for the host player and other players. Their position and rotation now synchronize with the ball.
- Fixed camera shaking and passenger position jitter while riding a katamari, as well as block jitter during boat and vehicle turns.
- Fixed passenger relationships remaining after disconnecting, dying, or changing dimensions.
- Fixed sneaking causing an unintended dismount and immediate reattachment loop.
