# Tracker Version — Upstream Lua Sync

## Pinned upstream commit
```
Repository : besteon/Ironmon-Tracker
Commit     : (not yet synced — update this when you do the first diff)
Date synced: (pending)
```

## Files monitored for porting
| Lua source file | Kotlin mirror | Notes |
|---|---|---|
| `GameSettings.lua` | `data/GameSettings.kt` | Game codes, ROM header addresses |
| `DataHelper.lua` | `data/DataHelper.kt` | Per-game memory address tables (PRIMARY porting target) |
| `RouteData.lua` | (not yet ported) | Route-specific encounter data |

## How to sync upstream

1. Browse to `besteon/Ironmon-Tracker` on GitHub, find the latest release commit
2. Open `GameSettings.lua` and `DataHelper.lua` in the upstream repo
3. Diff against our Kotlin equivalents using a side-by-side viewer
4. Update `data/GameSettings.kt` for new game codes or ROM header changes
5. Update `data/DataHelper.kt` for address table changes
6. Update `data/tables/SpeciesNames.kt` / `MoveNames.kt` if new games added new entries
7. Update the commit hash below and commit

## Porting checklist (per upstream release)
- [ ] New game codes in `GameSettings.lua` → `GameSettings.kt` FIRE_RED_CODES etc.
- [ ] Address changes in `DataHelper.lua` → `DataHelper.kt` FIRE_RED / LEAF_GREEN / etc.
- [ ] New tracked fields → `PokemonData.kt` + `PokemonDecoder.kt`
- [ ] Move or species name table changes → `MoveNames.kt` / `SpeciesNames.kt`
- [ ] Update pinned commit hash above

## Address verification reference

Use this Python script to verify addresses from a live mGBA (over forwarded port 7777):

```python
import socket, struct

def read32(addr):
    s = socket.socket()
    s.connect(('127.0.0.1', 7777))
    s.sendall(struct.pack('<IB', addr, 4) + b'\x00')
    data = s.recv(4)
    s.close()
    return struct.unpack('<I', data)[0]

# Game code check (should return 'BPRE' for Fire Red)
code_raw = read32(0x080000AC)
code = struct.pack('<I', code_raw).decode('ascii', errors='replace')
print(f"Game code: {code}")

# Party count (should be 1-6 in game, 0 in title screen)
party_count = read32(0x02024029) & 0xFF
print(f"Party count: {party_count}")
```
