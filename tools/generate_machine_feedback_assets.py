#!/usr/bin/env python3
"""Generate deterministic custom machine sounds and process-particle sprites."""

from __future__ import annotations

import hashlib
import json
import math
import pathlib
import random
import shutil
import struct
import subprocess
import wave

from PIL import Image, ImageDraw, ImageFont


ROOT = pathlib.Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "neoforge-26.2" / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / "earth_on_minecraft"
PROFILES = RESOURCES / "data" / "earth_on_minecraft" / "earth" / "quality" / "machine_profiles.json"
TMP = ROOT / "tmp" / "generated-machine-feedback"
OUTPUT = ROOT / "output" / "quality"
SAMPLE_RATE = 22_050
DURATION = 1.4

FAMILY_COLORS = {
    "comminution": (174, 145, 102),
    "classification": (115, 170, 190),
    "wet_process": (74, 190, 175),
    "thermal": (246, 117, 54),
    "electrochemical": (92, 165, 255),
    "forming": (194, 160, 219),
    "reaction": (111, 196, 126),
    "column": (139, 204, 217),
    "mixing": (173, 184, 101),
    "crystallization": (156, 188, 255),
    "combustion_power": (238, 128, 62),
    "rotating_power": (82, 184, 214),
}


def deterministic_seed(value: str) -> int:
    return int.from_bytes(hashlib.sha256(value.encode("utf-8")).digest()[:8], "big")


def synthesize(profile: dict[str, object]) -> list[int]:
    machine_id = str(profile["id"])
    family = str(profile["family"])
    rng = random.Random(deterministic_seed(machine_id))
    sample_count = round(SAMPLE_RATE * DURATION)
    base = 42.0 + rng.random() * 36.0
    pulse_rate = 1.6 + rng.random() * 4.8
    noise_state = 0.0
    phase_shift = rng.random() * math.tau
    samples: list[int] = []
    for index in range(sample_count):
        t = index / SAMPLE_RATE
        fade = min(1.0, t / 0.08, (DURATION - t) / 0.08)
        motor = 0.34 * math.sin(math.tau * base * t + phase_shift)
        motor += 0.16 * math.sin(math.tau * base * 2.01 * t)
        raw_noise = rng.uniform(-1.0, 1.0)
        noise_state = noise_state * 0.91 + raw_noise * 0.09
        texture = 0.0
        if family == "comminution":
            impact = max(0.0, math.sin(math.tau * pulse_rate * t)) ** 18
            texture = 0.38 * impact * rng.uniform(-1.0, 1.0) + 0.17 * noise_state
        elif family == "classification":
            texture = 0.15 * math.sin(math.tau * pulse_rate * 7.0 * t) + 0.12 * noise_state
        elif family == "wet_process":
            bubble = max(0.0, math.sin(math.tau * pulse_rate * t + 0.7)) ** 10
            texture = 0.20 * noise_state + 0.16 * bubble * math.sin(math.tau * 240 * t)
        elif family == "thermal":
            texture = 0.30 * noise_state + 0.10 * math.sin(math.tau * 17.0 * t)
        elif family == "electrochemical":
            texture = 0.18 * math.sin(math.tau * 100.0 * t) + 0.12 * math.sin(math.tau * 300.0 * t)
        elif family == "forming":
            stroke = max(0.0, math.sin(math.tau * pulse_rate * 0.55 * t)) ** 28
            texture = 0.34 * stroke * math.sin(math.tau * 72.0 * t) + 0.08 * noise_state
        elif family == "reaction":
            texture = 0.13 * math.sin(math.tau * pulse_rate * 3.0 * t) + 0.13 * noise_state
        elif family == "column":
            texture = 0.21 * noise_state + 0.08 * math.sin(math.tau * pulse_rate * 2.0 * t)
        elif family == "mixing":
            texture = 0.18 * math.sin(math.tau * pulse_rate * t) + 0.10 * noise_state
        elif family == "crystallization":
            glint = max(0.0, math.sin(math.tau * pulse_rate * 0.7 * t)) ** 22
            texture = 0.10 * noise_state + 0.16 * glint * math.sin(math.tau * (510 + base) * t)
        elif family == "combustion_power":
            firing = max(0.0, math.sin(math.tau * pulse_rate * 0.62 * t)) ** 12
            texture = 0.19 * noise_state + 0.22 * firing * math.sin(math.tau * (base * 1.4) * t)
        elif family == "rotating_power":
            texture = 0.16 * math.sin(math.tau * (base * 4.2) * t)
            texture += 0.08 * math.sin(math.tau * (base * 7.1) * t) + 0.06 * noise_state
        value = max(-1.0, min(1.0, (motor + texture) * 0.48 * fade))
        samples.append(round(value * 32767))
    return samples


def synthesize_signal(kind: str) -> list[int]:
    rng = random.Random(deterministic_seed(kind))
    duration = 0.75 if kind == "fault" else 0.48
    samples: list[int] = []
    for index in range(round(SAMPLE_RATE * duration)):
        t = index / SAMPLE_RATE
        fade = min(1.0, t / 0.025, (duration - t) / 0.08)
        if kind == "fault":
            tone = math.sin(math.tau * 310 * t) * (0.55 if int(t * 7) % 2 == 0 else 0.12)
        else:
            tone = 0.45 * math.sin(math.tau * (480 + 320 * t / duration) * t)
            tone += 0.18 * math.sin(math.tau * 960 * t)
        tone += rng.uniform(-0.025, 0.025)
        samples.append(round(max(-1.0, min(1.0, tone * fade)) * 32767))
    return samples


def write_ogg(samples: list[int], target: pathlib.Path) -> None:
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise RuntimeError("ffmpeg is required to encode generated machine sounds")
    TMP.mkdir(parents=True, exist_ok=True)
    wav_path = TMP / f"{target.stem}-{deterministic_seed(str(target))}.wav"
    with wave.open(str(wav_path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(b"".join(struct.pack("<h", sample) for sample in samples))
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([
        ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav_path),
        "-c:a", "libvorbis", "-q:a", "4", str(target),
    ], check=True)


def draw_particle(profile: dict[str, object], target: pathlib.Path) -> None:
    machine_id = str(profile["id"])
    family = str(profile["family"])
    rng = random.Random(deterministic_seed("particle:" + machine_id))
    base = FAMILY_COLORS[family]
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for index in range(5):
        x = rng.randint(7, 24)
        y = rng.randint(7, 24)
        radius = rng.randint(1, 3)
        light = min(255, 35 + index * 12)
        color = (min(255, base[0] + light), min(255, base[1] + light), min(255, base[2] + light), 210 - index * 18)
        if family in {"thermal", "column", "wet_process", "reaction"}:
            draw.ellipse((x - radius, y - radius * 2, x + radius, y + radius * 2), fill=color)
        elif family in {"electrochemical", "crystallization"}:
            draw.polygon(((x, y - radius * 2), (x + radius * 2, y), (x, y + radius * 2), (x - radius * 2, y)), fill=color)
        else:
            draw.rectangle((x - radius, y - radius, x + radius, y + radius), fill=color)
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, optimize=True)


def load_profiles() -> list[tuple[str, dict[str, object]]]:
    data = json.loads(PROFILES.read_text(encoding="utf-8"))
    return [
        *(("machine", profile) for profile in data["machines"]),
        *(("device", profile) for profile in data.get("devices", [])),
    ]


def render_contact_sheet(profiles: list[tuple[str, dict[str, object]]]) -> pathlib.Path:
    columns = 6
    cell_w = 188
    cell_h = 86
    rows = math.ceil(len(profiles) / columns)
    sheet = Image.new("RGBA", (columns * cell_w, rows * cell_h), "#171b1f")
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for index, (group, profile) in enumerate(profiles):
        machine_id = str(profile["id"])
        x = index % columns * cell_w
        y = index // columns * cell_h
        draw.rectangle((x, y, x + cell_w - 1, y + cell_h - 1), outline="#46515a")
        texture = ASSETS / "textures" / "particle" / group / f"{machine_id}.png"
        if texture.exists():
            with Image.open(texture).convert("RGBA") as image:
                preview = image.resize((56, 56), Image.Resampling.NEAREST)
            sheet.alpha_composite(preview, (x + 8, y + 8))
        draw.text((x + 70, y + 10), machine_id[:24], fill="#f2f4f5", font=font)
        draw.text((x + 70, y + 29), str(profile["family"]), fill="#78b8d6", font=font)
        draw.text((x + 70, y + 48), str(profile["particle_profile"])[:25], fill="#c6ced3", font=font)
        draw.text((x + 70, y + 65), group, fill="#e0aa63", font=font)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / "machine-feedback-contact-sheet.png"
    sheet.convert("RGB").save(path, quality=95)
    return path


def main() -> None:
    profiles = load_profiles()
    sounds: dict[str, object] = {}
    particles_root = ASSETS / "particles"
    for group, profile in profiles:
        machine_id = str(profile["id"])
        event = str(profile["sound_event"]).removeprefix("earth_on_minecraft:")
        sound_name = f"earth_on_minecraft:{group}/{machine_id}/run"
        write_ogg(synthesize(profile), ASSETS / "sounds" / group / machine_id / "run.ogg")
        sounds[event] = {
            "subtitle": "subtitles.earth_on_minecraft.machine.running",
            "sounds": [{"name": sound_name, "volume": 0.72}],
        }
        particle_id = f"{group}_{machine_id}_process"
        draw_particle(profile, ASSETS / "textures" / "particle" / group / f"{machine_id}.png")
        (particles_root / f"{particle_id}.json").parent.mkdir(parents=True, exist_ok=True)
        (particles_root / f"{particle_id}.json").write_text(
            json.dumps({"textures": [f"earth_on_minecraft:{group}/{machine_id}"]}, indent=2) + "\n",
            encoding="utf-8")

    write_ogg(synthesize_signal("fault"), ASSETS / "sounds" / "machine" / "fault.ogg")
    write_ogg(synthesize_signal("complete"), ASSETS / "sounds" / "machine" / "complete.ogg")
    sounds["machine.fault"] = {
        "subtitle": "subtitles.earth_on_minecraft.machine.fault",
        "sounds": [{"name": "earth_on_minecraft:machine/fault", "volume": 0.72}],
    }
    sounds["machine.complete"] = {
        "subtitle": "subtitles.earth_on_minecraft.machine.complete",
        "sounds": [{"name": "earth_on_minecraft:machine/complete", "volume": 0.62}],
    }
    (ASSETS / "sounds.json").write_text(json.dumps(sounds, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    contact_sheet = render_contact_sheet(profiles)
    report = {
        "schema_version": 1,
        "profiles": len(profiles),
        "machine_profiles": sum(group == "machine" for group, _ in profiles),
        "device_profiles": sum(group == "device" for group, _ in profiles),
        "sound_events": len(sounds),
        "particle_types": len(profiles),
        "contact_sheet": contact_sheet.relative_to(ROOT).as_posix(),
    }
    (OUTPUT / "machine-feedback-report.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"MACHINE_FEEDBACK_ASSETS_OK sounds={len(sounds)} particles={len(profiles)} contactSheet={contact_sheet}")


if __name__ == "__main__":
    main()
