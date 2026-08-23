#!/usr/bin/env python3
"""Generate the NatDex species-name map (internal id -> name) from the NatDexExtension source.

The upstream extension (CyanSMP64/NatDexExtension) carries the canonical internal-id -> name map as a
Lua table `self.Data.pokeNameList = { [412] = "Turtwig", ... }`. Rather than hand-transcribe it into
Kotlin, this reads it straight from a chosen git tag of the clone and emits a generated Kotlin file.

Re-run after bumping the NatDex clone to pick up new mons:

    uv run python tools/gen_natdex_names.py --tag v1.2.1

No third-party deps (stdlib only); `uv run` just gives a consistent interpreter.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
DEFAULT_CLONE = REPO / "NatDexExtension"
DEFAULT_OUT = (
    REPO
    / "mgba-android-memapi/android-fork/tracker-core/src/commonMain/kotlin"
    / "hh/game/mgba_android/tracker/tables/NatDexSpeciesNames.kt"
)

ENTRY_RE = re.compile(r'\[(\d+)\]\s*=\s*"((?:[^"\\]|\\.)*)"')


def git_show(clone: Path, tag: str, path: str) -> str:
    return subprocess.run(
        ["git", "-C", str(clone), "show", f"{tag}:{path}"],
        check=True, capture_output=True, text=True,
    ).stdout


def git_commit(clone: Path, tag: str) -> str:
    return subprocess.run(
        ["git", "-C", str(clone), "rev-parse", "--short", f"{tag}^{{commit}}"],
        check=True, capture_output=True, text=True,
    ).stdout.strip()


def extract_poke_name_list(lua: str) -> dict[int, str]:
    """Pull the `[id] = "name"` pairs out of the `self.Data.pokeNameList = { ... }` block."""
    start = lua.find("self.Data.pokeNameList")
    if start == -1:
        sys.exit("error: could not find self.Data.pokeNameList in source")
    brace = lua.find("{", start)
    # Walk to the matching closing brace so we don't spill into later tables.
    depth, i = 0, brace
    while i < len(lua):
        if lua[i] == "{":
            depth += 1
        elif lua[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    block = lua[brace : i + 1]
    names = {int(m.group(1)): m.group(2) for m in ENTRY_RE.finditer(block)}
    if not names:
        sys.exit("error: parsed pokeNameList but found no entries")
    return names


def render_kotlin(names: dict[int, str], tag: str, commit: str) -> str:
    lines = [
        "package hh.game.mgba_android.tracker.tables",
        "",
        "// GENERATED — do not edit by hand. Regenerate with:",
        "//   uv run python tools/gen_natdex_names.py --tag " + tag,
        f"// Source: CyanSMP64/NatDexExtension {tag} ({commit}), self.Data.pokeNameList.",
        "//",
        "// Internal ROM species id -> display name for NatDex-added mons (national dex #387+).",
        "// SpeciesNames.get() falls back to this for ids > 411.",
        "object NatDexSpeciesNames {",
        f"    // {len(names)} entries, ids {min(names)}–{max(names)}.",
        "    val NAMES: Map<Int, String> = mapOf(",
    ]
    for i in sorted(names):
        name = names[i].replace("\\", "\\\\").replace('"', '\\"')
        lines.append(f'        {i} to "{name}",')
    lines += ["    )", "}", ""]
    return "\n".join(lines)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--tag", default="v1.2.1", help="NatDexExtension git tag to read (default: v1.2.1)")
    ap.add_argument("--clone", type=Path, default=DEFAULT_CLONE, help="path to the NatDexExtension clone")
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT, help="Kotlin file to write")
    args = ap.parse_args()

    lua = git_show(args.clone, args.tag, "NatDexExtension.lua")
    commit = git_commit(args.clone, args.tag)
    names = extract_poke_name_list(lua)
    args.out.write_text(render_kotlin(names, args.tag, commit))
    print(f"wrote {len(names)} names (ids {min(names)}-{max(names)}) from {args.tag} ({commit}) -> {args.out}")


if __name__ == "__main__":
    main()
