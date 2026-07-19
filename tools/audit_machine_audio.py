#!/usr/bin/env python3
"""Decode and audit custom device audio without launching Minecraft."""

from __future__ import annotations

import argparse
import array
import hashlib
import json
import math
import pathlib
import shutil
import subprocess

from PIL import Image, ImageDraw, ImageFont


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "neoforge-26.2" / "src" / "main" / "resources" / "assets" / "earth_on_minecraft"
SOUNDS_JSON = ASSETS / "sounds.json"
OUTPUT = ROOT / "output" / "quality"
SAMPLE_RATE = 22_050


def decode(path: pathlib.Path) -> list[int]:
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise RuntimeError("ffmpeg is required for machine audio auditing")
    result = subprocess.run([
        ffmpeg, "-hide_banner", "-loglevel", "error", "-i", str(path),
        "-f", "s16le", "-ac", "1", "-ar", str(SAMPLE_RATE), "pipe:1",
    ], check=True, capture_output=True)
    samples = array.array("h")
    samples.frombytes(result.stdout)
    return list(samples)


def metrics(path: pathlib.Path) -> tuple[dict[str, object], list[int]]:
    samples = decode(path)
    absolute = [abs(sample) for sample in samples]
    peak = max(absolute, default=0) / 32768.0
    rms = math.sqrt(sum(sample * sample for sample in samples) / max(1, len(samples))) / 32768.0
    dc_offset = abs(sum(samples) / max(1, len(samples))) / 32768.0
    clipping = sum(value >= 32760 for value in absolute) / max(1, len(samples))
    silence = sum(value <= 96 for value in absolute) / max(1, len(samples))
    return ({
        "path": path.relative_to(ROOT).as_posix(),
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        "duration_seconds": round(len(samples) / SAMPLE_RATE, 4),
        "peak": round(peak, 5),
        "rms": round(rms, 5),
        "dc_offset": round(dc_offset, 6),
        "clipping_ratio": round(clipping, 7),
        "silence_ratio": round(silence, 5),
    }, samples)


def render_waveforms(records: list[dict[str, object]], decoded: dict[str, list[int]]) -> pathlib.Path:
    columns = 3
    cell_w = 390
    cell_h = 78
    rows = math.ceil(len(records) / columns)
    sheet = Image.new("RGB", (columns * cell_w, rows * cell_h), "#171b1f")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for index, record in enumerate(records):
        x = index % columns * cell_w
        y = index // columns * cell_h
        draw.rectangle((x, y, x + cell_w - 1, y + cell_h - 1), outline="#46515a")
        label = pathlib.Path(str(record["path"])).parent.name
        if label == "machine":
            label = pathlib.Path(str(record["path"])).stem
        draw.text((x + 7, y + 5), label[:34], fill="#f1f4f5", font=font)
        draw.text((x + 245, y + 5), f"rms {record['rms']:.3f} peak {record['peak']:.3f}", fill="#78b8d6", font=font)
        samples = decoded[str(record["path"])]
        mid = y + 48
        draw.line((x + 6, mid, x + cell_w - 7, mid), fill="#394249")
        width = cell_w - 14
        for column in range(width):
            start = column * len(samples) // width
            end = max(start + 1, (column + 1) * len(samples) // width)
            amplitude = max((abs(sample) for sample in samples[start:end]), default=0) / 32768.0
            height = max(1, round(amplitude * 23))
            draw.line((x + 7 + column, mid - height, x + 7 + column, mid + height), fill="#64b6c9")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "machine-audio-waveforms.png"
    sheet.save(path, quality=95)
    return path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.parse_args()
    sounds = json.loads(SOUNDS_JSON.read_text(encoding="utf-8-sig"))
    paths = sorted((ASSETS / "sounds").rglob("*.ogg"))
    failures: list[str] = []
    records: list[dict[str, object]] = []
    decoded: dict[str, list[int]] = {}
    for path in paths:
        record, samples = metrics(path)
        records.append(record)
        decoded[str(record["path"])] = samples
        duration = float(record["duration_seconds"])
        if not 0.35 <= duration <= 2.0:
            failures.append(f"unexpected duration {duration}s: {record['path']}")
        if float(record["peak"]) >= 0.995:
            failures.append(f"near-clipping peak {record['peak']}: {record['path']}")
        if not 0.015 <= float(record["rms"]) <= 0.42:
            failures.append(f"out-of-range RMS {record['rms']}: {record['path']}")
        if float(record["dc_offset"]) > 0.03:
            failures.append(f"excessive DC offset {record['dc_offset']}: {record['path']}")
        if float(record["clipping_ratio"]) > 0.0001:
            failures.append(f"clipping detected {record['clipping_ratio']}: {record['path']}")
        if float(record["silence_ratio"]) > 0.55:
            failures.append(f"mostly silent asset {record['silence_ratio']}: {record['path']}")

    hashes = [str(record["sha256"]) for record in records]
    if len(hashes) != len(set(hashes)):
        failures.append("duplicate OGG payloads detected")
    referenced = {
        sound["name"].removeprefix("earth_on_minecraft:") + ".ogg"
        for event in sounds.values() for sound in event.get("sounds", []) if isinstance(sound, dict)
    }
    actual = {path.relative_to(ASSETS / "sounds").as_posix() for path in paths}
    if referenced != actual:
        failures.append(f"sounds.json coverage mismatch missing={sorted(referenced - actual)} extra={sorted(actual - referenced)}")

    contact_sheet = render_waveforms(records, decoded)
    report = {
        "schema_version": 1, "sound_events": len(sounds), "ogg_files": len(paths),
        "contact_sheet": contact_sheet.relative_to(ROOT).as_posix(), "records": records, "failures": failures,
    }
    report_path = OUTPUT / "machine-audio-report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}")
        raise SystemExit(1)
    print(f"MACHINE_AUDIO_OK files={len(paths)} events={len(sounds)} report={report_path} contactSheet={contact_sheet}")


if __name__ == "__main__":
    main()
