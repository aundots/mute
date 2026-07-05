"""Generate Play Store phone screenshots (1080x2340, 9:19.5)."""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 2340
OUT = Path(__file__).resolve().parent.parent / "store-assets" / "screenshots" / "phone"
FONT = r"C:\Windows\Fonts\malgun.ttf"
FONT_BOLD = r"C:\Windows\Fonts\malgunbd.ttf"

# Material 3 light-ish palette
BG = (255, 251, 254)
SURFACE = (255, 255, 255)
SURFACE_VARIANT = (231, 224, 236)
PRIMARY = (103, 80, 164)
ON_SURFACE = (28, 27, 31)
ON_SURFACE_VARIANT = (73, 69, 79)
OUTLINE = (202, 196, 208)
SAMSUNG_BG = (248, 248, 248)
SAMSUNG_BLUE = (0, 114, 198)
STATUS_BAR = (255, 255, 255)


def load(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_BOLD if bold else FONT, size)


def rounded_rect(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    radius: int,
    fill: tuple[int, int, int],
    outline: tuple[int, int, int] | None = None,
    width: int = 2,
) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_status_bar(img: Image.Image, time: str = "3:05", dark: bool = False) -> None:
    draw = ImageDraw.Draw(img)
    color = (0, 0, 0) if not dark else (255, 255, 255)
    f = load(34)
    draw.text((48, 36), time, fill=color, font=f)
    # signal icons placeholder
    draw.text((W - 180, 36), "5G  ▮▮▮▮ 100%", fill=color, font=load(28))


def draw_nav_bar(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, H - 72, W, H), fill=(255, 255, 255))
    cx = W // 2
    draw.rounded_rectangle((cx - 60, H - 36, cx + 60, H - 24), radius=6, fill=(28, 27, 31))


def new_frame() -> Image.Image:
    img = Image.new("RGB", (W, H), BG)
    draw_status_bar(img)
    draw_nav_bar(img)
    return img


def draw_app_header(draw: ImageDraw.ImageDraw, title: str, y: int = 120) -> int:
    draw.text((56, y), title, fill=ON_SURFACE, font=load(56, bold=True))
    return y + 90


def draw_status_card(draw: ImageDraw.ImageDraw, y: int, text: str, highlight: bool = False) -> int:
    h = 140
    rounded_rect(draw, (56, y, W - 56, y + h), 28, SURFACE, OUTLINE, 2)
    color = PRIMARY if highlight else ON_SURFACE
    tw = draw.textlength(text, font=load(48, bold=True))
    draw.text(((W - tw) / 2, y + 42), text, fill=color, font=load(48, bold=True))
    return y + h + 36


def draw_text_field(
    draw: ImageDraw.ImageDraw,
    y: int,
    label: str,
    value: str,
    card: bool = True,
) -> int:
    if card:
        rounded_rect(draw, (56, y, W - 56, y + 220), 24, SURFACE_VARIANT, None)
        inner_y = y + 24
    else:
        inner_y = y
    draw.text((80, inner_y), label, fill=ON_SURFACE_VARIANT, font=load(28))
    rounded_rect(draw, (80, inner_y + 44, W - 80, inner_y + 130), 16, SURFACE, OUTLINE, 2)
    draw.text((104, inner_y + 72), value, fill=ON_SURFACE, font=load(40))
    return y + 250 if card else y + 180


def draw_filled_button(draw: ImageDraw.ImageDraw, y: int, text: str) -> int:
    rounded_rect(draw, (56, y, W - 56, y + 104), 52, PRIMARY, None)
    tw = draw.textlength(text, font=load(40, bold=True))
    draw.text(((W - tw) / 2, y + 28), text, fill=(255, 255, 255), font=load(40, bold=True))
    return y + 130


def draw_text_button(draw: ImageDraw.ImageDraw, y: int, text: str) -> int:
    tw = draw.textlength(text, font=load(36))
    draw.text(((W - tw) / 2, y), text, fill=PRIMARY, font=load(36))
    return y + 60


def draw_banner_ad(draw: ImageDraw.ImageDraw, y: int) -> None:
    rounded_rect(draw, (56, y, W - 56, y + 120), 8, (240, 240, 240), OUTLINE, 1)
    draw.text((W // 2 - 80, y + 40), "광고", fill=ON_SURFACE_VARIANT, font=load(32))


def screenshot_01_setup() -> None:
    img = new_frame()
    d = ImageDraw.Draw(img)
    y = draw_app_header(d, "카메라 셔터 무음")
    y = draw_status_card(d, y, "설정 필요")
    y = draw_text_field(d, y, "페어링 포트", "37841")
    y = draw_text_field(d, y, "6자리 PIN", "482916", card=False)
    y += 20
    draw_filled_button(d, y, "처음 설정하기")
    draw_text_button(d, y + 140, "고급 설정")
    img.save(OUT / "01-app-setup.png", optimize=True)


def screenshot_04_muted() -> None:
    img = new_frame()
    d = ImageDraw.Draw(img)
    y = draw_app_header(d, "카메라 셔터 무음")
    y = draw_status_card(d, y, "셔터음 꺼짐", highlight=True)
    y += 40
    draw_filled_button(d, y, "다시 적용")
    draw_text_button(d, y + 140, "고급 설정")
    img.save(OUT / "04-app-muted.png", optimize=True)


def screenshot_05_free_muted() -> None:
    img = new_frame()
    d = ImageDraw.Draw(img)
    y = draw_app_header(d, "카메라 셔터 무음 (무료)")
    y = draw_status_card(d, y, "셔터음 꺼짐", highlight=True)
    y += 40
    draw_filled_button(d, y, "다시 적용")
    draw_banner_ad(d, H - 72 - 140)
    img.save(OUT / "05-free-app-muted.png", optimize=True)


def samsung_header(draw: ImageDraw.ImageDraw, title: str, back: bool = True) -> int:
    draw.rectangle((0, 88, W, 220), fill=STATUS_BAR)
    if back:
        draw.text((48, 130), "←", fill=ON_SURFACE, font=load(44))
    tw = draw.textlength(title, font=load(40, bold=True))
    draw.text(((W - tw) / 2, 132), title, fill=ON_SURFACE, font=load(40, bold=True))
    return 240


def samsung_list_item(
    draw: ImageDraw.ImageDraw,
    y: int,
    title: str,
    subtitle: str | None = None,
    toggle_on: bool | None = None,
    chevron: bool = False,
) -> int:
    h = 120 if subtitle is None else 150
    draw.rectangle((0, y, W, y + h), fill=SURFACE)
    draw.line((48, y + h - 1, W, y + h - 1), fill=(230, 230, 230), width=1)
    draw.text((48, y + 28 if subtitle else 38), title, fill=ON_SURFACE, font=load(36))
    if subtitle:
        draw.text((48, y + 78), subtitle, fill=ON_SURFACE_VARIANT, font=load(28))
    if toggle_on is not None:
        tx, ty = W - 130, y + (h - 56) // 2
        rounded_rect(draw, (tx, ty, tx + 96, ty + 56), 28, SAMSUNG_BLUE if toggle_on else (200, 200, 200), None)
        if toggle_on:
            draw.ellipse((tx + 48, ty + 6, tx + 90, ty + 50), fill=(255, 255, 255))
        else:
            draw.ellipse((tx + 6, ty + 6, tx + 48, ty + 50), fill=(255, 255, 255))
    if chevron:
        draw.text((W - 60, y + 42), "›", fill=ON_SURFACE_VARIANT, font=load(48))
    return y + h


def screenshot_02_dev_options() -> None:
    img = Image.new("RGB", (W, H), SAMSUNG_BG)
    draw_status_bar(img)
    draw_nav_bar(img)
    d = ImageDraw.Draw(img)
    y = samsung_header(d, "개발자 옵션")
    items = [
        ("USB 디버깅", None, False, False),
        ("무선 디버깅", "Wi‑Fi를 통해 기기에 연결", True, True),
        ("기본 USB 구성", None, None, True),
        ("버그 신고 바로가기", None, None, True),
    ]
    for title, sub, toggle, chev in items:
        y = samsung_list_item(d, y, title, sub, toggle, chev)
    # highlight box
    rounded_rect(d, (24, 318, W - 24, 478), 20, (103, 80, 164, 0), PRIMARY, 4)
    d.text((48, 500), "↑ 무선 디버깅을 켠 뒤 항목 이름을 탭하세요", fill=PRIMARY, font=load(30))
    img.save(OUT / "02-developer-options.png", optimize=True)


def screenshot_03_pairing() -> None:
    base = Image.new("RGB", (W, H), SAMSUNG_BG)
    draw_status_bar(base)
    draw_nav_bar(base)
    d = ImageDraw.Draw(base)
    y = samsung_header(d, "무선 디버깅")
    y = samsung_list_item(d, y, "무선 디버깅", None, True, False)
    samsung_list_item(d, y, "페어링 코드로 기기 페어링", None, None, True)

    # dim overlay
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 100))
    base = base.convert("RGBA")
    base = Image.alpha_composite(base, overlay)
    d = ImageDraw.Draw(base)

    dx1, dy1, dx2, dy2 = 80, 580, W - 80, 1420
    rounded_rect(d, (dx1, dy1, dx2, dy2), 32, (255, 255, 255), None)
    d.text((120, dy1 + 48), "Wi‑Fi 페어링 코드", fill=ON_SURFACE, font=load(44, bold=True))
    d.text((120, dy1 + 120), "이 코드로 기기를 페어링하세요", fill=ON_SURFACE_VARIANT, font=load(30))
    d.text((120, dy1 + 210), "Wi‑Fi 페어링 포트", fill=ON_SURFACE_VARIANT, font=load(28))
    d.text((120, dy1 + 252), "37841", fill=ON_SURFACE, font=load(64, bold=True))
    d.text((120, dy1 + 370), "페어링 코드", fill=ON_SURFACE_VARIANT, font=load(28))
    d.text((120, dy1 + 412), "482916", fill=PRIMARY, font=load(72, bold=True))
    rounded_rect(d, (120, dy1 + 560, dx2 - 40, dy1 + 640), 24, SURFACE_VARIANT, None)
    tw = d.textlength("확인", font=load(36, bold=True))
    d.text(((W - tw) / 2, dy1 + 582), "확인", fill=PRIMARY, font=load(36, bold=True))
    d.text((120, dy1 + 680), "※ 이 화면을 연 채 앱에 포트·PIN 입력", fill=PRIMARY, font=load(28))
    base.convert("RGB").save(OUT / "03-pairing-code.png", optimize=True)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    screenshot_01_setup()
    screenshot_02_dev_options()
    screenshot_03_pairing()
    screenshot_04_muted()
    screenshot_05_free_muted()
    print(f"Saved 5 screenshots to {OUT}")


if __name__ == "__main__":
    main()
