#!/usr/bin/env python3
"""Create a reproducible asset inventory and enforce player-visible coverage."""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import pathlib

from PIL import Image


ROOT = pathlib.Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / "earth_on_minecraft"
PROFILES = RESOURCES / "data" / "earth_on_minecraft" / "earth" / "quality" / "machine_profiles.json"
UI_MATRIX = ROOT / "docs" / "quality" / "ui-state-matrix.json"
OUTPUT = ROOT / "output" / "quality"


def local_model_textures(model_path: pathlib.Path) -> tuple[pathlib.Path, ...]:
    if not model_path.exists():
        return ()
    data = json.loads(model_path.read_text(encoding="utf-8-sig"))
    paths: list[pathlib.Path] = []
    for texture_id in data.get("textures", {}).values():
        namespace, separator, texture_path = str(texture_id).partition(":")
        if separator and namespace == "earth_on_minecraft":
            paths.append(ASSETS / "textures" / f"{texture_path}.png")
    return tuple(dict.fromkeys(paths))


def pipeline_for(path: pathlib.Path, machine_ids: set[str]) -> str:
    relative = path.relative_to(ASSETS).as_posix()
    if relative.startswith("textures/block/connected/"):
        return "geology_blocks"
    if relative.startswith("textures/item/"):
        return "item_icons"
    if (relative.startswith("sounds/") or relative == "sounds.json"
            or relative.startswith("particles/") or relative.startswith("textures/particle/")):
        return "machine_feedback"
    model_stem = path.stem.removesuffix("_active").removesuffix("_fault")
    if model_stem in machine_ids and (
            relative.startswith("models/block/") or relative.startswith("textures/block/")):
        return "blockbench_models"
    if relative.startswith("textures/gui/"):
        return "ui"
    return "shared_resources"


def inventory() -> tuple[list[dict[str, object]], list[str], dict[str, int]]:
    profile_data = json.loads(PROFILES.read_text(encoding="utf-8"))
    profiles = [
        *(("machine", profile) for profile in profile_data["machines"]),
        *(("device", profile) for profile in profile_data.get("devices", [])),
    ]
    machine_ids = {str(profile["id"]) for _, profile in profiles}
    failures: list[str] = []
    records: list[dict[str, object]] = []
    counts: collections.Counter[str] = collections.Counter()
    for path in sorted(file for file in ASSETS.rglob("*") if file.is_file()):
        record: dict[str, object] = {
            "path": path.relative_to(ROOT).as_posix(),
            "bytes": path.stat().st_size,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "pipeline": pipeline_for(path, machine_ids),
        }
        if path.suffix.lower() == ".png":
            with Image.open(path) as image:
                record["dimensions"] = [image.width, image.height]
                record["mode"] = image.mode
        records.append(record)
        counts[str(record["pipeline"])] += 1

    sounds = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8")) if (ASSETS / "sounds.json").exists() else {}
    for group, profile in profiles:
        machine_id = str(profile["id"])
        event = str(profile["sound_event"]).removeprefix("earth_on_minecraft:")
        sound_file = ASSETS / "sounds" / group / machine_id / "run.ogg"
        particle_id = f"{group}_{machine_id}_process"
        idle_model = ASSETS / "models" / "block" / f"{machine_id}.json"
        active_model = ASSETS / "models" / "block" / f"{machine_id}_active.json"
        fault_model = ASSETS / "models" / "block" / f"{machine_id}_fault.json"
        required = (
            ASSETS / "blockstates" / f"{machine_id}.json",
            idle_model,
            active_model,
            fault_model,
            sound_file,
            ASSETS / "particles" / f"{particle_id}.json",
            ASSETS / "textures" / "particle" / group / f"{machine_id}.png",
            *local_model_textures(idle_model),
            *local_model_textures(active_model),
            *local_model_textures(fault_model),
        )
        failures.extend(f"missing required machine asset {path.relative_to(ROOT)}" for path in required if not path.exists())
        if event not in sounds:
            failures.append(f"missing sounds.json event {event}")

    matrix = json.loads(UI_MATRIX.read_text(encoding="utf-8"))
    state_count = sum(len(screen["states"]) for screen in matrix["screens"])
    if state_count < 20:
        failures.append(f"UI state matrix is too narrow: {state_count} states")
    return records, failures, dict(sorted(counts.items()))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.parse_args()
    records, failures, counts = inventory()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    report = {
        "schema_version": 1,
        "asset_count": len(records),
        "pipeline_counts": counts,
        "assets": records,
        "failures": failures,
    }
    report_path = OUTPUT / "asset-inventory.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    summary = ["# Asset Inventory", "", f"Total tracked runtime assets: {len(records)}", ""]
    summary.extend(f"- `{pipeline}`: {count}" for pipeline, count in counts.items())
    summary.extend(["", f"Failures: {len(failures)}", ""])
    summary.extend(f"- {failure}" for failure in failures)
    (OUTPUT / "asset-summary.md").write_text("\n".join(summary) + "\n", encoding="utf-8")
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        raise SystemExit(1)
    print(f"ASSET_AUDIT_OK assets={len(records)} pipelines={len(counts)} report={report_path}")


if __name__ == "__main__":
    main()
