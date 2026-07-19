#!/usr/bin/env python3
"""Audit item silhouettes and render an inventory-scale contact sheet."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "neoforge-26.2" / "src" / "main" / "resources" / "assets" / "earth_on_minecraft"
TEXTURES = ASSETS / "textures" / "item"
ITEM_MODELS = ASSETS / "items"
OUTPUT = ROOT / "output" / "quality"
PREPARE = ROOT / "tools" / "prepare_image2_item_batch.py"
FINAL_SIZE = 128


def alpha_bbox(image: Image.Image):
    return image.getchannel("A").getbbox()


def audit() -> tuple[list[dict[str, object]], list[str]]:
    records: list[dict[str, object]] = []
    failures: list[str] = []
    for path in sorted(TEXTURES.glob("*.png")):
        with Image.open(path).convert("RGBA") as image:
            bbox = alpha_bbox(image)
            if image.size != (FINAL_SIZE, FINAL_SIZE):
                failures.append(f"{path.name} is {image.width}x{image.height}, expected {FINAL_SIZE}x{FINAL_SIZE}")
            if bbox is None:
                failures.append(f"{path.name} has no visible pixels")
                bbox = (0, 0, 0, 0)
            margin = min(bbox[0], bbox[1], image.width - bbox[2], image.height - bbox[3])
            coverage = 0.0 if bbox == (0, 0, 0, 0) else ((bbox[2] - bbox[0]) * (bbox[3] - bbox[1])) / (image.width * image.height)
        model = ITEM_MODELS / f"{path.stem}.json"
        if not model.exists():
            failures.append(f"missing item model definition for {path.stem}")
        records.append({
            "id": path.stem,
            "path": str(path.relative_to(ROOT)).replace("\\", "/"),
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "size": [FINAL_SIZE, FINAL_SIZE],
            "alpha_bbox": list(bbox),
            "minimum_margin": margin,
            "bbox_coverage": round(coverage, 4),
        })
    return records, failures


def render_contact_sheet(records: list[dict[str, object]]) -> pathlib.Path:
    columns = 12
    cell = 82
    label_height = 18
    rows = math.ceil(len(records) / columns)
    sheet = Image.new("RGBA", (columns * cell, rows * (cell + label_height)), "#171b1f")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for index, record in enumerate(records):
        x = (index % columns) * cell
        y = (index // columns) * (cell + label_height)
        draw.rectangle((x, y, x + cell - 1, y + cell + label_height - 1), outline="#46515a")
        with Image.open(ROOT / str(record["path"])).convert("RGBA") as image:
            preview = image.resize((64, 64), Image.Resampling.NEAREST)
        sheet.alpha_composite(preview, (x + 9, y + 4))
        label = str(record["id"])
        if len(label) > 12:
            label = label[:11] + "…"
        draw.text((x + 3, y + cell), label, fill="#f2f4f5", font=font)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "item-icon-contact-sheet.png"
    sheet.convert("RGB").save(path, quality=95)
    return path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--prepare-image2", action="store_true", help="prepare prompts for icons below the final-size threshold")
    args = parser.parse_args()
    if args.prepare_image2:
        subprocess.run([sys.executable, str(PREPARE), "prepare", "--min-size", str(FINAL_SIZE)], cwd=ROOT, check=True)
    records, failures = audit()
    contact_sheet = render_contact_sheet(records)
    report = {
        "pipeline": "item_icons",
        "schema_version": 1,
        "final_size": FINAL_SIZE,
        "items": records,
        "failures": failures,
        "contact_sheet": str(contact_sheet.relative_to(ROOT)).replace("\\", "/"),
    }
    report_path = OUTPUT / "item-icon-pipeline-report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        raise SystemExit(1)
    print(f"ITEM_ICON_PIPELINE_OK items={len(records)} contactSheet={contact_sheet} report={report_path}")


if __name__ == "__main__":
    main()
