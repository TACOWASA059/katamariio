# KatamariIO
![image](common/src/main/resources/icon.png)


https://github.com/user-attachments/assets/33ec2c37-4ff6-4192-b9e5-03bcf725ee65

## Commands
Permission level: `2` (OP)

- `/katamari size <targets> <value>`
Set collision size for target players.  
Range: `0.01 .. 20.0`

- `/katamari size <target>`
Show current size.

- `/katamari restitution <targets> <value>`
Set restitution coefficient.  
Range: `0.01 .. 1.2`

- `/katamari restitution <target>`
Show restitution coefficient.

- `/katamari ball <targets> <enabled>`
Enable/disable ball mode.

- `/katamari resetBlocks <targets>`
Clear attached blocks and reset collision/render size to `KatamariIO.DEFAULT_BALL_SIZE` (currently `2.0f`).

## Player NBT
Custom player data is saved in player NBT via `PlayerMixin#addAdditionalSaveData`.

- `SPM_Size` (`float`)
Collision size.

- `SPM_RenderSize` (`float`)
Render size.

- `SPM_isBall` (`boolean`)
Ball mode flag.

- `SPM_RESTITUTION` (`float`)
Restitution coefficient.

- `SPM_Quaternion` (`compound`)
Current quaternion: `x`, `y`, `z`, `w`.

- `SPM_POSITION` (`compound`)
Current position cache: `x`, `y`, `z`.

- `SPM_BlockCount` (`int`)
Total attached-block count metric.

- `SPM_AttachedBlocks` (`list<compound>`)
Attached block entries for sync/render.  
Each entry has:
`block` (resource location string), `x`, `y`, `z`, `qx`, `qy`, `qz`, `qw`.

## NBT command examples
- `/data get entity @s SPM_BlockCount`
- `/data get entity @s SPM_Size`
