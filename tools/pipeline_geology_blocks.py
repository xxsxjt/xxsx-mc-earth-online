#!/usr/bin/env python3
"""Build and verify connected geology block assets independently of other art."""

from __future__ import annotations

import argparse
import importlib.util
import json
import pathlib
import subprocess
import sys

from PIL import Image, ImageChops


ROOT = pathlib.Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
OUTPUT = ROOT / "output" / "quality"
GENERATOR = TOOLS / "generate_connected_ore_assets.py"
PREVIEW = TOOLS / "render_connected_ore_previews.py"


def load_generator():
    spec = importlib.util.spec_from_file_location("earth_connected_ores", GENERATOR)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load {GENERATOR}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def edges_match(image: Image.Image) -> bool:
    left = image.crop((0, 0, 1, image.height))
    right = image.crop((image.width - 1, 0, image.width, image.height))
    top = image.crop((0, 0, image.width, 1))
    bottom = image.crop((0, image.height - 1, image.width, image.height))
    return ImageChops.difference(left, right).getbbox() is None and ImageChops.difference(top, bottom).getbbox() is None


def audit() -> dict[str, object]:
    module = load_generator()
    block_ids = module.ore_ids()
    failures: list[str] = []
    dimensions: dict[str, list[int]] = {}
    for block_id in block_ids:
        source = module.TEXTURES / f"{block_id}.png"
        center = module.CONNECTED_TEXTURES / f"{block_id}_center.png"
        if not source.exists() or not center.exists():
            failures.append(f"missing source or connected center for {block_id}")
            continue
        with Image.open(source).convert("RGBA") as source_image, Image.open(center).convert("RGBA") as center_image:
            dimensions[block_id] = [center_image.width, center_image.height]
            if source_image.size != center_image.size:
                failures.append(f"size mismatch for {block_id}: {source_image.size} != {center_image.size}")
            if not edges_match(center_image):
                failures.append(f"opposite center-texture edges do not match for {block_id}")
        expected_models = [
            module.CONNECTED_MODELS / f"{block_id}_center.json",
            *(module.CONNECTED_MODELS / f"{block_id}_edge_{direction}.json"
              for direction in ("down", "up", "north", "south", "west", "east")),
        ]
        failures.extend(f"missing model {path.relative_to(ROOT)}" for path in expected_models if not path.exists())
    return {
        "pipeline": "geology_blocks",
        "schema_version": 1,
        "connected_blocks": len(block_ids),
        "model_files": len(block_ids) * 7,
        "edge_border_units": module.EDGE_BORDER,
        "center_crop_units": module.CENTER_CROP,
        "edge_blend_units": module.CENTER_EDGE_BLEND,
        "dimensions": dimensions,
        "failures": failures,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="verify committed assets without regenerating them")
    args = parser.parse_args()
    if not args.check:
        subprocess.run([sys.executable, str(GENERATOR)], cwd=ROOT, check=True)
        subprocess.run([sys.executable, str(PREVIEW)], cwd=ROOT, check=True)
    report = audit()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "geology-pipeline-report.json"
    path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if report["failures"]:
        for failure in report["failures"]:
            print(f"ERROR: {failure}")
        raise SystemExit(1)
    print(f"GEOLOGY_PIPELINE_OK blocks={report['connected_blocks']} models={report['model_files']} report={path}")


if __name__ == "__main__":
    main()
