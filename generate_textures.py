#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw

def create_loader_textures():
    # 1. Entity Texture: 512x512 texture atlas
    img = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Palette ───────────────────────────────────────────────────────
    c_yellow = (245, 184, 0, 255)            # CAT/Komatsu/SDLG industrial yellow
    c_yellow_light = (255, 212, 50, 255)      # Highlight bevel
    c_yellow_dark = (195, 142, 0, 255)        # Shaded yellow
    c_yellow_groove = (145, 102, 0, 255)      # Panel seam

    c_dark_iron = (44, 45, 48, 255)           # Chassis & frame
    c_iron_light = (70, 72, 78, 255)          # Edge highlight
    c_iron_recess = (24, 25, 27, 255)

    c_tire_rubber = (30, 31, 34, 255)         # Massive loader tire rubber
    c_tire_lug = (48, 50, 55, 255)            # Deep tread lugs
    c_tire_tread_groove = (18, 19, 21, 255)   # Sipe groove

    c_rim_yellow = (245, 184, 0, 255)         # Wheel rim yellow
    c_hub_cap = (210, 155, 0, 255)            # Planetary hub cap
    c_bolt = (60, 62, 66, 255)                # Wheel lug nuts

    c_bucket_steel = (68, 72, 78, 255)        # Loader bucket steel
    c_cutting_edge = (198, 204, 214, 255)     # Hardened cutting edge / wear lip
    c_tooth_forged = (165, 172, 184, 255)     # Forged teeth steel
    c_bucket_rib = (50, 52, 56, 255)          # Reinforcing ribs

    c_glass_pane = (140, 210, 245, 38)        # Tinted safety glass (alpha ~15%)
    c_glass_frame = (24, 24, 26, 255)         # Frame gasket
    c_glass_streak = (220, 245, 255, 65)      # Glass reflections

    c_seat_leather = (35, 36, 40, 255)
    c_exhaust_metal = (55, 56, 60, 255)
    c_chrome_ram = (225, 230, 238, 255)       # Hydraulic chrome cylinder rod

    c_beacon_amber = (255, 146, 0, 255)
    c_beacon_bright = (255, 225, 40, 255)

    c_light_white = (255, 255, 240, 255)      # Work headlights
    c_light_red = (220, 30, 20, 255)          # Tail lights

    c_dirt = (134, 90, 61, 255)               # Granular soil in bucket
    c_sand = (219, 207, 153, 255)
    c_gravel = (128, 126, 124, 255)

    # ── Section 1: Massive Tire Rubber Tread (u=0..160, v=0..80) ────
    draw.rectangle([0, 0, 160, 80], fill=c_tire_rubber)
    # Heavy directional chevron / block tread lugs
    for x in range(0, 160, 8):
        draw.rectangle([x, 0, x + 4, 80], fill=c_tire_lug)
        draw.line([(x, 0), (x, 80)], fill=c_tire_tread_groove)
        draw.line([(x + 4, 0), (x + 4, 80)], fill=c_tire_tread_groove)
    for y in range(0, 80, 10):
        draw.line([(0, y), (160, y)], fill=c_tire_tread_groove)

    # ── Section 2: Industrial Yellow Wheel Rims & Hubs (u=164..260, v=0..80)
    draw.rectangle([164, 0, 260, 80], fill=c_rim_yellow)
    draw.rectangle([164, 0, 260, 80], outline=c_yellow_dark)
    # Circular rim bevels & central planetary hub
    draw.rectangle([184, 15, 240, 65], fill=c_hub_cap, outline=c_yellow_dark)
    for bx, by in [(190, 25), (234, 25), (190, 55), (234, 55), (212, 20), (212, 60), (186, 40), (238, 40)]:
        draw.rectangle([bx - 2, by - 2, bx + 2, by + 2], fill=c_bolt)

    # ── Section 3: Yellow Body / Hood / Cab (u=0..180, v=84..170) ───
    draw.rectangle([0, 84, 180, 170], fill=c_yellow)
    draw.rectangle([0, 84, 180, 170], outline=c_yellow_dark)
    # Bevel lines and panel grooves
    draw.line([(0, 94), (180, 94)], fill=c_yellow_groove)
    draw.line([(0, 150), (180, 150)], fill=c_yellow_groove)
    draw.line([(90, 84), (90, 170)], fill=c_yellow_groove)

    # ── Section 4: Engine Side Louvers & Radiator (u=184..260, v=84..170)
    draw.rectangle([184, 84, 260, 170], fill=c_dark_iron)
    # Engine hood cooling louvers
    for y in range(92, 160, 4):
        draw.rectangle([190, y, 254, y + 2], fill=c_iron_light)
        draw.line([(190, y + 2), (254, y + 2)], fill=c_iron_recess)

    # ── Section 5: Chassis & Heavy Counterweight (u=264..400, v=0..80)
    draw.rectangle([264, 0, 400, 80], fill=c_dark_iron)
    draw.rectangle([264, 0, 400, 80], outline=c_iron_light)
    # Rear tail lights and reflectors
    draw.rectangle([270, 10, 295, 25], fill=c_light_red, outline=c_iron_recess)
    draw.rectangle([300, 10, 325, 25], fill=c_beacon_amber, outline=c_iron_recess)
    draw.rectangle([365, 10, 390, 25], fill=c_light_red, outline=c_iron_recess)

    # ── Section 6: Panoramic Cab Glass (u=264..360, v=84..160) ───────
    draw.rectangle([264, 84, 360, 160], fill=c_glass_pane)
    draw.rectangle([264, 84, 360, 160], outline=c_glass_frame)
    draw.line([(275, 155), (345, 90)], fill=c_glass_streak, width=2)
    draw.line([(285, 158), (355, 95)], fill=c_glass_streak, width=1)

    # ── Section 7: Lift Arms & Cross-Tube (u=0..180, v=174..230) ────
    draw.rectangle([0, 174, 180, 230], fill=c_yellow)
    draw.rectangle([0, 174, 180, 230], outline=c_yellow_dark)
    # Pin bushings & grease points
    for px in [15, 60, 110, 160]:
        draw.ellipse([px - 4, 198, px + 4, 206], fill=c_dark_iron, outline=c_iron_light)

    # ── Section 8: Loader Bucket Steel & Cutting Edge (u=184..360, v=174..250)
    draw.rectangle([184, 174, 360, 250], fill=c_bucket_steel)
    draw.rectangle([184, 174, 360, 250], outline=c_bucket_rib)
    # Cutting edge lip plate & wear plates
    draw.rectangle([184, 235, 360, 250], fill=c_cutting_edge)
    for x in range(190, 355, 14):
        # Heavy forged bucket teeth
        draw.rectangle([x, 240, x + 8, 250], fill=c_tooth_forged, outline=c_dark_iron)

    # ── Section 9: Hydraulic Cylinders (u=364..450, v=84..160) ──────
    draw.rectangle([364, 84, 450, 160], fill=c_yellow)
    draw.rectangle([364, 84, 450, 160], outline=c_yellow_dark)
    # Mirror chrome hydraulic ram
    draw.rectangle([400, 84, 430, 160], fill=c_chrome_ram, outline=c_iron_light)

    # ── Section 10: Interior Seat, Dials, Headlights (u=364..450, v=164..230)
    draw.rectangle([364, 164, 450, 230], fill=c_seat_leather)
    draw.rectangle([370, 170, 400, 200], fill=c_dark_iron)
    # Headlight reflectors
    draw.ellipse([410, 170, 440, 200], fill=c_light_white, outline=c_dark_iron)

    # ── Section 11: Safety Warning Beacon (u=454..500, v=0..40) ─────
    draw.rectangle([454, 0, 500, 40], fill=c_beacon_amber)
    draw.rectangle([462, 8, 492, 32], fill=c_beacon_bright)

    # ── Section 12: Granular Soil inside Bucket (u=0..120, v=234..300)
    draw.rectangle([0, 234, 120, 300], fill=c_dirt)
    for x in range(0, 120, 4):
        for y in range(234, 300, 4):
            if (x + y) % 8 == 0:
                draw.point((x, y), fill=(160, 110, 80, 255))
            elif (x + y) % 12 == 0:
                draw.point((x, y), fill=(100, 65, 40, 255))

    # Save entity texture
    out_dir = "src/main/resources/assets/pw_groundworks_loader/textures/entity"
    os.makedirs(out_dir, exist_ok=True)
    img.save(os.path.join(out_dir, "loader.png"))
    print("Saved entity texture: loader.png")

    # 2. Item Texture: 32x32 pixel art icon
    item_img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    idraw = ImageDraw.Draw(item_img)

    # Tires
    idraw.rectangle([3, 19, 10, 27], fill=c_tire_rubber, outline=(15, 16, 18, 255))
    idraw.rectangle([5, 21, 8, 25], fill=c_rim_yellow)
    idraw.rectangle([18, 19, 25, 27], fill=c_tire_rubber, outline=(15, 16, 18, 255))
    idraw.rectangle([20, 21, 23, 25], fill=c_rim_yellow)

    # Chassis & Counterweight
    idraw.rectangle([1, 16, 6, 21], fill=c_dark_iron)
    idraw.rectangle([6, 15, 18, 20], fill=c_yellow)

    # Cab
    idraw.rectangle([11, 8, 17, 15], fill=c_yellow)
    idraw.rectangle([12, 9, 16, 13], fill=(160, 225, 255, 200), outline=c_dark_iron)
    idraw.rectangle([10, 7, 18, 8], fill=c_yellow_dark)

    # Exhaust & Beacon
    idraw.line([(7, 15), (7, 10)], fill=c_exhaust_metal, width=1)
    idraw.rectangle([13, 5, 15, 7], fill=c_beacon_amber)

    # Boom Arm (sloping forward-down)
    idraw.line([(16, 14), (25, 20)], fill=c_yellow, width=2)
    idraw.line([(15, 15), (20, 17)], fill=c_yellow_dark, width=1)

    # Bucket
    idraw.polygon([(24, 18), (29, 14), (30, 22), (25, 23)], fill=c_yellow, outline=c_bucket_rib)
    idraw.line([(29, 21), (31, 22)], fill=c_cutting_edge)

    item_dir = "src/main/resources/assets/pw_groundworks_loader/textures/item"
    os.makedirs(item_dir, exist_ok=True)
    item_img.save(os.path.join(item_dir, "loader.png"))
    print("Saved item icon: loader.png")

if __name__ == '__main__':
    create_loader_textures()
