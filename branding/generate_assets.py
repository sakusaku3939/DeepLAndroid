"""Generate Android branding resources from the approved, outlined SVGs."""

from io import BytesIO
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from fontTools.misc.transform import Identity, Transform
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.svgLib.path import parse_path
from PIL import Image
import resvg_py


ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
ANDROID = "http://schemas.android.com/apk/res/android"
SVG = "http://www.w3.org/2000/svg"
ET.register_namespace("android", ANDROID)
BACKGROUND = "#f9fbff"
FOREGROUND = "#123b58"
SAFE_SCALE = 0.66
SAFE_INSET = 108 * (1 - SAFE_SCALE) / 2


def transformed_paths(element, transform=Identity):
    """Flatten the translate/scale transforms used by the approved artwork."""
    for name, args in re.findall(r"(translate|scale)\(([^)]+)\)", element.get("transform", "")):
        values = [float(value) for value in re.split(r"[\s,]+", args.strip())]
        if name == "translate":
            transform = transform.translate(values[0], values[1] if len(values) == 2 else 0)
        else:
            transform = transform.scale(values[0], values[1] if len(values) == 2 else values[0])
    if element.tag == f"{{{SVG}}}path":
        pen = SVGPathPen(None, ntos=lambda value: f"{value:.4f}".rstrip("0").rstrip(".") if value else "0")
        parse_path(element.attrib["d"], TransformPen(pen, transform))
        yield pen.getCommands(), element.get("fill-rule", "nonzero")
    for child in element:
        yield from transformed_paths(child, transform)


def vector(paths, viewport, size, color):
    root = ET.Element("vector", {
        f"{{{ANDROID}}}width": f"{size[0]}dp",
        f"{{{ANDROID}}}height": f"{size[1]}dp",
        f"{{{ANDROID}}}viewportWidth": str(viewport[0]),
        f"{{{ANDROID}}}viewportHeight": str(viewport[1]),
    })
    for data, rule in paths:
        ET.SubElement(root, "path", {
            f"{{{ANDROID}}}fillColor": color,
            f"{{{ANDROID}}}pathData": data,
        })
    ET.indent(root, space="    ")
    return '<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(root, encoding="unicode") + "\n"


def svg(paths, viewport, background=None, round_background=False):
    body = ""
    if background:
        body = (f'<circle cx="54" cy="54" r="36" fill="{background}"/>' if round_background
                else f'<rect width="108" height="108" fill="{background}"/>')
    for data, rule in paths:
        body += f'<path fill="{FOREGROUND}" fill-rule="{rule}" d="{data}"/>'
    return f'<svg xmlns="{SVG}" viewBox="{viewport}">{body}</svg>'


def raster(source, size, target):
    # Render larger, then downsample so low-density text stays antialiased.
    png = resvg_py.svg_to_bytes(svg_string=source, width=size * 4, height=size * 4)
    image = Image.open(BytesIO(png)).convert("RGBA").resize((size, size), Image.Resampling.LANCZOS)
    target.parent.mkdir(parents=True, exist_ok=True)
    options = {"lossless": True} if target.suffix == ".webp" else {}
    image.save(target, **options)


def main():
    icon = ET.parse(ROOT / "branding/app-icon.svg").getroot()
    header = ET.parse(ROOT / "branding/header-logo.svg").getroot()
    icon_paths = list(transformed_paths(icon))
    safe_paths = list(transformed_paths(icon, Transform(SAFE_SCALE, 0, 0, SAFE_SCALE, SAFE_INSET, SAFE_INSET)))
    header_paths = list(transformed_paths(header, Transform(1, 0, 0, 1, -22, -18)))
    # The first group contains the speech symbol; subsequent paths are the name.
    symbol = icon.find(f"{{{SVG}}}g/{{{SVG}}}g/{{{SVG}}}path")
    symbol_paths = list(transformed_paths(symbol))

    resources = {
        "ic_launcher_foreground": vector(safe_paths, (108, 108), (108, 108), "@color/ic_launcher_foreground"),
        "ic_launcher_monochrome": vector(safe_paths, (108, 108), (108, 108), "@android:color/white"),
        "deepl_logo": vector(header_paths, (308, 72), (171, 40), "@color/app_logo_foreground"),
        "deepl_mini_logo": vector(symbol_paths, (108, 108), (64, 64), "@color/app_logo_foreground"),
        "ic_deepl_mini_logo": vector(symbol_paths, (108, 108), (64, 64), "@color/app_logo_foreground"),
        "app_logo": vector(icon_paths, (108, 108), (108, 108), "@color/ic_launcher_foreground"),
    }
    for name, content in resources.items():
        (RES / f"drawable/{name}.xml").write_text(content, encoding="utf-8")

    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        directory = RES / f"mipmap-{density}"
        raster(svg(safe_paths, "18 18 72 72", BACKGROUND), size, directory / "ic_launcher.webp")
        raster(svg(safe_paths, "18 18 72 72", BACKGROUND, True), size, directory / "ic_launcher_round.webp")
        raster(svg(safe_paths, "0 0 108 108"), size * 108 // 48, directory / "ic_launcher_foreground.png")

    store = svg(icon_paths, "0 0 108 108", BACKGROUND)
    raster(store, 512, ROOT / "app/src/main/ic_launcher-playstore.png")
    raster(store, 512, ROOT / "fastlane/metadata/android/en-US/images/icon.png")


if __name__ == "__main__":
    main()
