#!/usr/bin/env python3
"""Verify deterministic M7 reference geometry and side-region culling."""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
from pathlib import Path
from typing import Any, Callable

from surface_geometry import read_png, write_png

INPUT_SCHEMA = "thords.m7-surface-sequence.v1"
ASPECT_TOLERANCE_PERCENT = 2.0
REFERENCE_FRAMES = (9, 15)
SIDE_FRAME_RANGE = range(5, 15)


def component_boxes(
    pixels: list[tuple[int, ...]],
    width: int,
    roi: tuple[int, int, int, int],
    predicate: Callable[[tuple[int, ...]], bool],
) -> list[dict[str, Any]]:
    x0, y0, x1, y1 = roi
    pending = {
        (x, y)
        for y in range(y0, y1)
        for x in range(x0, x1)
        if predicate(pixels[y * width + x])
    }
    boxes = []
    while pending:
        stack = [pending.pop()]
        xs: list[int] = []
        ys: list[int] = []
        while stack:
            x, y = stack.pop()
            xs.append(x)
            ys.append(y)
            for neighbor in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if neighbor in pending:
                    pending.remove(neighbor)
                    stack.append(neighbor)
        box_width = max(xs) - min(xs) + 1
        box_height = max(ys) - min(ys) + 1
        boxes.append(
            {
                "pixelCount": len(xs),
                "x": min(xs),
                "y": min(ys),
                "width": box_width,
                "height": box_height,
                "aspect": round(box_width / box_height, 6),
                "centerX": round((min(xs) + max(xs)) / 2.0, 3),
                "centerY": round((min(ys) + max(ys)) / 2.0, 3),
            }
        )
    return sorted(boxes, key=lambda box: int(box["pixelCount"]), reverse=True)


def frame_entry(manifest: dict[str, Any], index: int) -> dict[str, Any]:
    frame = next(
        (item for item in manifest.get("frames", []) if int(item.get("index", -1)) == index),
        None,
    )
    if frame is None:
        raise ValueError(f"frame {index} is absent")
    return frame


def top_presenter(frame: dict[str, Any]) -> dict[str, Any]:
    presenter = next(
        (
            record
            for record in frame.get("presenter", {}).get("records", [])
            if record.get("surfaceRole") == "top"
        ),
        None,
    )
    if presenter is None:
        raise ValueError(f"top presenter is absent for frame {frame.get('index')}")
    return presenter


def sequence_structure(
    manifest: dict[str, Any],
    expected_rect: dict[str, int],
    expected_draw_mask: int,
) -> bool:
    frames = manifest.get("frames", [])
    return (
        manifest.get("schema") == INPUT_SCHEMA
        and manifest.get("target") == "main"
        and manifest.get("result") == "PASS"
        and len(frames) >= 16
        and all(
            frame.get("inputHandled") is True
            and frame.get("frameReady") is True
            and frame.get("frameAdvanced") is True
            and frame.get("presenterComplete") is True
            and int(frame.get("presenterRecordCount", 0)) == 2
            and int(frame.get("endFrame", 0)) - int(frame.get("startFrame", 0)) == 1
            and top_presenter(frame).get("topRect") == {"enabled": True, **expected_rect}
            and int(top_presenter(frame).get("drawModeMask", 0)) == expected_draw_mask
            for frame in frames
        )
    )


def load_frame(manifest_path: Path, frame: dict[str, Any]) -> tuple[int, int, list[tuple[int, ...]]]:
    path = manifest_path.parent / str(frame.get("file", ""))
    if not path.is_file():
        raise ValueError(f"frame PNG does not exist: {path}")
    return read_png(path, include_alpha=True)


def opaque(pixels: list[tuple[int, ...]]) -> bool:
    return all(len(pixel) == 4 and pixel[3] == 255 for pixel in pixels)


def analyze_reference(
    native_path: Path,
    probe_path: Path,
) -> tuple[dict[str, Any], bool]:
    for path in (native_path, probe_path):
        if not path.is_file():
            raise ValueError(f"manifest does not exist: {path}")
    native = json.loads(native_path.read_text())
    probe = json.loads(probe_path.read_text())
    structural_pass = (
        sequence_structure(
            native,
            {"x": 240, "y": 0, "width": 1440, "height": 1080},
            1 << 1,
        )
        and sequence_structure(
            probe,
            {"x": 0, "y": 0, "width": 1920, "height": 1080},
            (1 << 7) | (1 << 8),
        )
        and [
            (int(frame["startFrame"]), int(frame["endFrame"]))
            for frame in native["frames"]
        ]
        == [
            (int(frame["startFrame"]), int(frame["endFrame"]))
            for frame in probe["frames"]
        ]
    )

    measurements = []
    for index in REFERENCE_FRAMES:
        native_frame = frame_entry(native, index)
        probe_frame = frame_entry(probe, index)
        native_width, native_height, native_pixels = load_frame(native_path, native_frame)
        probe_width, probe_height, probe_pixels = load_frame(probe_path, probe_frame)
        if (native_width, native_height) != (1920, 1080) or (
            probe_width,
            probe_height,
        ) != (1920, 1080):
            raise ValueError("reference surfaces must be 1920x1080")

        green = lambda pixel: (
            pixel[1] >= 75
            and pixel[1] >= pixel[0] * 1.22
            and pixel[1] >= pixel[2] * 1.18
        )
        roi = (800, 520, 1120, 692)
        native_box = component_boxes(native_pixels, native_width, roi, green)[0]
        probe_box = component_boxes(probe_pixels, probe_width, roi, green)[0]
        delta = (
            abs(float(native_box["aspect"]) - float(probe_box["aspect"]))
            / float(native_box["aspect"])
            * 100.0
        )
        measurements.append(
            {
                "frameIndex": index,
                "emulatorFrame": native_frame.get("endFrame"),
                "native": native_box,
                "probe": probe_box,
                "aspectDeltaPercent": round(delta, 4),
                "opaque": opaque(native_pixels) and opaque(probe_pixels),
                "pass": delta <= ASPECT_TOLERANCE_PERCENT
                and opaque(native_pixels)
                and opaque(probe_pixels),
            }
        )

    passed = structural_pass and all(item["pass"] for item in measurements)
    return (
        {
            "status": "PASS" if passed else "FAIL",
            "structuralPass": structural_pass,
            "referenceObject": "Yoshi green head silhouette",
            "maskRoi": {"x": 800, "y": 520, "width": 320, "height": 172},
            "aspectTolerancePercent": ASPECT_TOLERANCE_PERCENT,
            "measurements": measurements,
        },
        passed,
    )


def analyze_side_landmark(manifest_path: Path) -> tuple[dict[str, Any], bool]:
    if not manifest_path.is_file():
        raise ValueError(f"manifest does not exist: {manifest_path}")
    manifest = json.loads(manifest_path.read_text())
    structural_pass = sequence_structure(
        manifest,
        {"x": 0, "y": 0, "width": 1920, "height": 1080},
        (1 << 7) | (1 << 8),
    )
    roi = (1680, 200, 1920, 360)
    measurements = []
    for index in SIDE_FRAME_RANGE:
        frame = frame_entry(manifest, index)
        width, height, pixels = load_frame(manifest_path, frame)
        if (width, height) != (1920, 1080):
            raise ValueError("side-landmark surface must be 1920x1080")
        bright_neutral = lambda pixel: (
            min(pixel[:3]) >= 205 and max(pixel[:3]) - min(pixel[:3]) <= 35
        )
        candidates = [
            box
            for box in component_boxes(pixels, width, roi, bright_neutral)
            if 500 <= int(box["pixelCount"]) <= 1200
            and 20 <= int(box["width"]) <= 50
            and 25 <= int(box["height"]) <= 60
            and int(box["x"]) > roi[0]
            and int(box["x"]) + int(box["width"]) < roi[2]
            and int(box["y"]) > roi[1]
            and int(box["y"]) + int(box["height"]) < roi[3]
        ]
        box = candidates[0] if candidates else None
        measurements.append(
            {
                "frameIndex": index,
                "emulatorFrame": frame.get("endFrame"),
                "bbox": box,
                "opaque": opaque(pixels),
                "pass": box is not None and opaque(pixels),
            }
        )

    centers = [
        float(item["bbox"]["centerX"])
        for item in measurements
        if item["bbox"] is not None
    ]
    distinct_centers = len(set(centers))
    displacement = centers[-1] - centers[0] if len(centers) == len(measurements) else 0.0
    passed = (
        structural_pass
        and all(item["pass"] for item in measurements)
        and distinct_centers >= 5
        and abs(displacement) >= 30.0
    )
    return (
        {
            "status": "PASS" if passed else "FAIL",
            "structuralPass": structural_pass,
            "landmark": "Castle Garden waterfall mist puff",
            "rightSideRoi": {"x": 1680, "y": 200, "width": 240, "height": 160},
            "uiSafeBoundaryX": 1680,
            "consecutiveFrameCount": len(measurements),
            "distinctCenterCount": distinct_centers,
            "centerDisplacementPixels": round(displacement, 3),
            "measurements": measurements,
        },
        passed,
    )


def single_frame_structure(
    manifest: dict[str, Any],
    expected_rect: dict[str, int],
    expected_draw_mask: int,
) -> bool:
    frames = manifest.get("frames", [])
    if (
        manifest.get("schema") != INPUT_SCHEMA
        or manifest.get("target") != "main"
        or manifest.get("result") != "PASS"
        or len(frames) != 1
    ):
        return False
    frame = frames[0]
    return (
        frame.get("inputHandled") is True
        and frame.get("frameReady") is True
        and frame.get("frameAdvanced") is True
        and frame.get("presenterComplete") is True
        and int(frame.get("presenterRecordCount", 0)) == 2
        and int(frame.get("uiOverlayPngBytes", 0)) > 0
        and int(frame.get("uiControlPngBytes", 0)) > 0
        and top_presenter(frame).get("topRect") == {"enabled": True, **expected_rect}
        and int(top_presenter(frame).get("drawModeMask", 0)) == expected_draw_mask
    )


def aspect_delta(reference: dict[str, Any], candidate: dict[str, Any]) -> float:
    return (
        abs(float(reference["aspect"]) - float(candidate["aspect"]))
        / float(reference["aspect"])
        * 100.0
    )


def first_component(
    pixels: list[tuple[int, ...]],
    width: int,
    roi: tuple[int, int, int, int],
    predicate: Callable[[tuple[int, ...]], bool],
    name: str,
) -> dict[str, Any]:
    boxes = component_boxes(pixels, width, roi, predicate)
    if not boxes:
        raise ValueError(f"{name} component is absent")
    return boxes[0]


def analyze_ui(
    native_path: Path,
    probe_path: Path,
) -> tuple[dict[str, Any], bool]:
    for path in (native_path, probe_path):
        if not path.is_file():
            raise ValueError(f"manifest does not exist: {path}")
    native = json.loads(native_path.read_text())
    probe = json.loads(probe_path.read_text())
    structural_pass = single_frame_structure(
        native,
        {"x": 240, "y": 0, "width": 1440, "height": 1080},
        1 << 1,
    ) and single_frame_structure(
        probe,
        {"x": 0, "y": 0, "width": 1920, "height": 1080},
        (1 << 7) | (1 << 8),
    )

    native_frame = frame_entry(native, 0)
    probe_frame = frame_entry(probe, 0)
    native_width, native_height, native_pixels = load_frame(native_path, native_frame)
    probe_width, probe_height, probe_pixels = load_frame(probe_path, probe_frame)
    if (native_width, native_height) != (1920, 1080) or (
        probe_width,
        probe_height,
    ) != (1920, 1080):
        raise ValueError("UI reference surfaces must be 1920x1080")

    overlay_file = probe_path.parent / str(probe_frame["uiOverlayFile"])
    control_file = probe_path.parent / str(probe_frame["uiControlFile"])
    overlay_width, overlay_height, overlay_pixels = read_png(
        overlay_file,
        include_alpha=True,
    )
    control_width, control_height, control_pixels = read_png(
        control_file,
        include_alpha=True,
    )
    if (overlay_width, overlay_height) != (256, 192) or (
        control_width,
        control_height,
    ) != (256, 192):
        raise ValueError("UI source planes must be 256x192")

    checks = (
        (
            "W04",
            "pause star-count zero ring",
            (980, 480, 1060, 600),
            (130, 84, 148, 108),
            lambda pixel: (
                pixel[0] >= 120
                and pixel[0] >= pixel[1] * 1.45
                and pixel[0] >= pixel[2] * 1.35
            ),
            lambda pixel: (
                pixel[2] >= 120
                and pixel[2] >= pixel[1] * 1.45
                and pixel[2] >= pixel[0] * 1.35
            ),
        ),
        (
            "W05",
            "pause instruction glyph O",
            (450, 920, 560, 1070),
            (36, 163, 58, 192),
            lambda pixel: pixel[0] >= 160 and pixel[1] >= 50 and pixel[2] <= 90,
            lambda pixel: pixel[2] >= 160 and pixel[1] >= 50 and pixel[0] <= 90,
        ),
    )
    reports: dict[str, Any] = {}
    for gate, name, final_roi, source_roi, final_predicate, source_predicate in checks:
        native_box = first_component(
            native_pixels,
            native_width,
            final_roi,
            final_predicate,
            f"{gate} native",
        )
        probe_box = first_component(
            probe_pixels,
            probe_width,
            final_roi,
            final_predicate,
            f"{gate} probe",
        )
        source_box = first_component(
            overlay_pixels,
            overlay_width,
            source_roi,
            source_predicate,
            f"{gate} source",
        )
        final_delta = aspect_delta(native_box, probe_box)
        source_delta = aspect_delta(source_box, probe_box)
        gate_pass = (
            final_delta <= ASPECT_TOLERANCE_PERCENT
            and source_delta <= ASPECT_TOLERANCE_PERCENT
            and opaque(native_pixels)
            and opaque(probe_pixels)
            and opaque(overlay_pixels)
        )
        reports[gate] = {
            "status": "PASS" if structural_pass and gate_pass else "FAIL",
            "reference": name,
            "native4x3": native_box,
            "probeUiSafe": probe_box,
            "internalOverlaySource": source_box,
            "nativeToProbeAspectDeltaPercent": round(final_delta, 4),
            "sourceToProbeAspectDeltaPercent": round(source_delta, 4),
            "opaque": opaque(native_pixels)
            and opaque(probe_pixels)
            and opaque(overlay_pixels),
        }

    control_diverse = len(set(control_pixels)) > 1
    passed = (
        structural_pass
        and control_diverse
        and all(report["status"] == "PASS" for report in reports.values())
    )
    return (
        {
            "structuralPass": structural_pass,
            "controlPlaneDiverse": control_diverse,
            "aspectTolerancePercent": ASPECT_TOLERANCE_PERCENT,
            "gates": reports,
        },
        passed,
    )


def self_test() -> bool:
    with tempfile.TemporaryDirectory() as directory_name:
        root = Path(directory_name)
        size = (1920, 1080)

        def image(
            name: str,
            marks: list[tuple[int, int, int, int, tuple[int, ...]]],
            dimensions: tuple[int, int] = size,
        ) -> str:
            width, height = dimensions
            pixels = [(0, 0, 0, 255)] * (width * height)
            for x0, y0, width, height, color in marks:
                for y in range(y0, y0 + height):
                    for x in range(x0, x0 + width):
                        pixels[y * dimensions[0] + x] = color
            path = root / name
            write_png(path, *dimensions, pixels)
            return path.name

        def presenter(rect: dict[str, int], mask: int) -> dict[str, Any]:
            return {
                "surfaceRole": "top",
                "topRect": {"enabled": True, **rect},
                "drawModeMask": mask,
            }

        def frame(index: int, file: str, rect: dict[str, int], mask: int, **extra: Any) -> dict[str, Any]:
            return {
                "index": index,
                "file": file,
                "inputHandled": True,
                "frameReady": True,
                "frameAdvanced": True,
                "presenterComplete": True,
                "presenterRecordCount": 2,
                "startFrame": index * 2,
                "endFrame": index * 2 + 1,
                "presenter": {"records": [presenter(rect, mask)]},
                **extra,
            }

        native_rect = {"x": 240, "y": 0, "width": 1440, "height": 1080}
        probe_rect = {"x": 0, "y": 0, "width": 1920, "height": 1080}
        native_green = image("native-green.png", [(900, 560, 40, 40, (20, 180, 20, 255))])
        probe_green = image("probe-green.png", [(900, 560, 40, 40, (20, 180, 20, 255))])
        native_frames = [
            frame(index, native_green, native_rect, 1 << 1) for index in range(16)
        ]
        probe_frames = [
            frame(index, probe_green, probe_rect, (1 << 7) | (1 << 8))
            for index in range(16)
        ]
        native_path = root / "native.json"
        probe_path = root / "probe.json"
        native_path.write_text(json.dumps({"schema": INPUT_SCHEMA, "target": "main", "result": "PASS", "frames": native_frames}))
        probe_path.write_text(json.dumps({"schema": INPUT_SCHEMA, "target": "main", "result": "PASS", "frames": probe_frames}))

        side_frames = []
        for index in range(16):
            landmark = image(
                f"side-{index}.png",
                [(1690 + index * 5, 220, 30, 30, (220, 220, 220, 255))],
            )
            side_frames.append(frame(index, landmark, probe_rect, (1 << 7) | (1 << 8)))
        side_path = root / "side.json"
        side_path.write_text(json.dumps({"schema": INPUT_SCHEMA, "target": "main", "result": "PASS", "frames": side_frames}))

        native_ui = image(
            "native-ui.png",
            [(1000, 500, 40, 40, (220, 20, 20, 255)), (500, 950, 15, 29, (200, 80, 20, 255))],
        )
        probe_ui = image(
            "probe-ui.png",
            [(1000, 500, 40, 40, (220, 20, 20, 255)), (500, 950, 15, 29, (200, 80, 20, 255))],
        )
        overlay = image(
            "overlay.png",
            [
                (130, 84, 18, 18, (20, 80, 200, 255)),
                (36, 163, 15, 29, (20, 80, 200, 255)),
            ],
            (256, 192),
        )
        control = image(
            "control.png",
            [(0, 0, 1, 1, (1, 2, 3, 255)), (1, 1, 1, 1, (4, 5, 6, 255))],
            (256, 192),
        )
        ui_native_frame = frame(
            0, native_ui, native_rect, 1 << 1, uiOverlayPngBytes=1, uiControlPngBytes=1
        )
        ui_probe_frame = frame(
            0, probe_ui, probe_rect, (1 << 7) | (1 << 8),
            uiOverlayPngBytes=1, uiControlPngBytes=1,
            uiOverlayFile=overlay, uiControlFile=control,
        )
        ui_native_path = root / "ui-native.json"
        ui_probe_path = root / "ui-probe.json"
        ui_native_path.write_text(json.dumps({"schema": INPUT_SCHEMA, "target": "main", "result": "PASS", "frames": [ui_native_frame]}))
        ui_probe_path.write_text(json.dumps({"schema": INPUT_SCHEMA, "target": "main", "result": "PASS", "frames": [ui_probe_frame]}))

        reference, reference_pass = analyze_reference(native_path, probe_path)
        side, side_pass = analyze_side_landmark(side_path)
        ui, ui_pass = analyze_ui(ui_native_path, ui_probe_path)
        if not (reference_pass and side_pass and ui_pass):
            return False
        negative = json.loads(side_path.read_text())
        negative["frames"][5]["inputHandled"] = False
        negative_path = root / "negative-side.json"
        negative_path.write_text(json.dumps(negative))
        _, negative_pass = analyze_side_landmark(negative_path)
        return not negative_pass


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--native", type=Path)
    parser.add_argument("--probe", type=Path)
    parser.add_argument("--side", type=Path)
    parser.add_argument("--ui-native", type=Path)
    parser.add_argument("--ui-probe", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--legacy-thords-v1", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        if not self_test():
            print("widescreen_proof self-test: FAIL", file=sys.stderr)
            return 1
        print("widescreen_proof self-test: PASS")
        return 0
    if (
        args.native is None
        or args.probe is None
        or args.side is None
        or args.ui_native is None
        or args.ui_probe is None
    ):
        parser.error("--native, --probe, --side, --ui-native and --ui-probe are required")
    for path in (args.native, args.probe, args.side, args.ui_native, args.ui_probe):
        if not path.is_file():
            parser.error(f"manifest does not exist: {path}")
    manifests = [args.native, args.probe, args.side, args.ui_native, args.ui_probe]
    schemas = [json.loads(path.read_text()).get("schema") for path in manifests]
    if any(schema != INPUT_SCHEMA for schema in schemas):
        parser.error(f"unsupported input schema: {next(schema for schema in schemas if schema != INPUT_SCHEMA)!r}")
    if not args.legacy_thords_v1:
        parser.error("ThorDS v1 input requires --legacy-thords-v1")

    reference, reference_pass = analyze_reference(args.native, args.probe)
    side, side_pass = analyze_side_landmark(args.side)
    ui, ui_pass = analyze_ui(args.ui_native, args.ui_probe)
    passed = reference_pass and side_pass and ui_pass
    report = {
        "schema": "sm64ds-eu-widescreen.widescreen-proof.v1",
        "result": "PASS" if passed else "FAIL",
        "gates": {
            "W01": reference,
            "W03": side,
            "W04": ui["gates"]["W04"],
            "W05": ui["gates"]["W05"],
        },
        "uiSource": {
            "structuralPass": ui["structuralPass"],
            "controlPlaneDiverse": ui["controlPlaneDiverse"],
        },
    }
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(rendered)
    else:
        sys.stdout.write(rendered)
    return 0 if passed else 2


if __name__ == "__main__":
    raise SystemExit(main())
