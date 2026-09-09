from __future__ import annotations

import re
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont


ROOT = Path(__file__).resolve().parent
SVG_DIR = ROOT / "svg"
FONT_PATH = ROOT.parent / "frontend" / "node_modules" / "@fontsource-variable" / "archivo" / "files" / "archivo-latin-wght-normal.woff2"

RED = "#E93645"
RED_BRIGHT = "#FF4D59"
INK = "#0B0B0D"
PAPER = "#F4F0E8"


def rounded_number(match: re.Match[str]) -> str:
    number = round(float(match.group()), 1)
    if number == 0:
        return "0"
    return f"{number:.1f}".rstrip("0").rstrip(".")


def build_wordmark_path() -> str:
    if not FONT_PATH.exists():
        raise FileNotFoundError(f"Fonte Archivo não encontrada em {FONT_PATH}")

    font = TTFont(FONT_PATH)
    font = instantiateVariableFont(font, {"wght": 700}, inplace=False)
    glyph_set = font.getGlyphSet()
    character_map = font.getBestCmap()
    metrics = font["hmtx"]

    cursor = 0
    paths: list[str] = []
    for character in "CineGiro":
        glyph_name = character_map[ord(character)]
        pen = SVGPathPen(glyph_set)
        glyph_set[glyph_name].draw(TransformPen(pen, (1, 0, 0, 1, cursor, 0)))
        paths.append(pen.getCommands())
        cursor += metrics[glyph_name][0] - 14

    path = " ".join(paths)
    return re.sub(r"-?\d+\.\d+", rounded_number, path)


def symbol(color: str) -> str:
    return f'''<path d="M71.3 24.7A33 33 0 1 0 71.3 71.3" fill="none" stroke="{color}" stroke-width="15" stroke-linecap="round"/>
<path d="M39.1 34.2c-1.8-1-4 .3-4 2.4v22.8c0 2.1 2.2 3.4 4 2.4l20.7-11.6c2-1.1 2-3.9 0-5z" fill="{color}" transform="translate(2.5)"/>'''


def svg_document(view_box: str, body: str, label: str = "CineGiro") -> str:
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view_box}" width="100%" height="100%" role="img" aria-label="{label}" focusable="false">
{body}
</svg>
'''


def horizontal_lockup(wordmark_path: str, symbol_color: str, text_color: str) -> str:
    body = f'''<g transform="translate(8 4) scale(.833333)">
{symbol(symbol_color)}
</g>
<path d="{wordmark_path}" fill="{text_color}" transform="translate(88 60) scale(.045 -.045)"/>'''
    return svg_document("0 0 300 88", body)


def stacked_lockup(wordmark_path: str, symbol_color: str, text_color: str) -> str:
    body = f'''<g transform="translate(66.8 0) scale(.9)">
{symbol(symbol_color)}
</g>
<path d="{wordmark_path}" fill="{text_color}" transform="translate(22.9 126) scale(.042 -.042)"/>'''
    return svg_document("0 0 220 145", body)


def write_asset(name: str, content: str) -> None:
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    (SVG_DIR / name).write_text(content, encoding="utf-8", newline="\n")


def main() -> None:
    wordmark_path = build_wordmark_path()

    write_asset("cinegiro-simbolo.svg", svg_document("0 0 96 96", symbol(RED)))
    write_asset("cinegiro-simbolo-branco.svg", svg_document("0 0 96 96", symbol("#FFFFFF")))
    write_asset("cinegiro-simbolo-preto.svg", svg_document("0 0 96 96", symbol(INK)))
    write_asset("cinegiro-simbolo-currentcolor.svg", svg_document("0 0 96 96", symbol("currentColor")))

    write_asset("cinegiro-wordmark-currentcolor.svg", svg_document(
        "0 -724 4150 736",
        f'<path d="{wordmark_path}" fill="currentColor" transform="scale(1 -1)"/>',
    ))

    write_asset("cinegiro-lockup-horizontal-escuro.svg", horizontal_lockup(wordmark_path, RED, PAPER))
    write_asset("cinegiro-lockup-horizontal-claro.svg", horizontal_lockup(wordmark_path, RED, INK))
    write_asset("cinegiro-lockup-horizontal-branco.svg", horizontal_lockup(wordmark_path, "#FFFFFF", "#FFFFFF"))
    write_asset("cinegiro-lockup-horizontal-preto.svg", horizontal_lockup(wordmark_path, INK, INK))
    write_asset("cinegiro-lockup-horizontal-currentcolor.svg", horizontal_lockup(wordmark_path, "currentColor", "currentColor"))

    write_asset("cinegiro-lockup-empilhado-escuro.svg", stacked_lockup(wordmark_path, RED, PAPER))
    write_asset("cinegiro-lockup-empilhado-claro.svg", stacked_lockup(wordmark_path, RED, INK))

    app_symbol = f'''<rect width="512" height="512" rx="112" fill="{INK}"/>
<g transform="translate(73.6 73.6) scale(3.8)">
{symbol(RED_BRIGHT)}
</g>'''
    write_asset("cinegiro-icone-app.svg", svg_document("0 0 512 512", app_symbol))

    maskable_symbol = f'''<rect width="512" height="512" fill="{INK}"/>
<g transform="translate(97.6 97.6) scale(3.3)">
{symbol(RED_BRIGHT)}
</g>'''
    write_asset("cinegiro-icone-maskable.svg", svg_document("0 0 512 512", maskable_symbol))

    favicon = f'''<path d="M23.3 8.7A10.3 10.3 0 1 0 23.3 23.3" fill="none" stroke="{RED}" stroke-width="5.2" stroke-linecap="round"/>
<path d="M13 11.4c-.8-.5-1.8.1-1.8 1v7.2c0 .9 1 1.5 1.8 1l6.6-3.6c.8-.4.8-1.5 0-2z" fill="{RED}" transform="translate(1)"/>'''
    write_asset("cinegiro-favicon.svg", svg_document("0 0 32 32", favicon))

    print(f"{len(list(SVG_DIR.glob('*.svg')))} SVGs gerados em {SVG_DIR}")


if __name__ == "__main__":
    main()
