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

## NBT command examples
- `/data get entity @s SPM_BlockCount`
- `/data get entity @s SPM_Size`
