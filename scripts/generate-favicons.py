import os
import io
import base64
from PIL import Image, ImageDraw

def generate_favicons():
    size = 512
    # Create mask for rounded rectangle (20% corner radius)
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    r = int(size * 0.20)
    draw.rounded_rectangle([(0, 0), (size - 1, size - 1)], radius=r, fill=255)

    # Dark background #07080b matching theme-color
    badge = Image.new('RGBA', (size, size), (7, 8, 11, 255))
    gold_img = Image.open('frontend/src/assets/images/logo-wb-agency-gold.png')
    # Bounding box of the WB monogram
    wb_gold = gold_img.crop((28, 20, 730, 395))
    gw, gh = wb_gold.size

    # Scale monogram to fit comfortably in 76% width
    scale = (size * 0.76) / gw
    nw, nh = int(gw * scale), int(gh * scale)
    wb_scaled = wb_gold.resize((nw, nh), Image.Resampling.LANCZOS)

    px = (size - nw) // 2
    py = (size - nh) // 2
    badge.paste(wb_scaled, (px, py), wb_scaled)

    # Master image with rounded corners
    master = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    master.paste(badge, (0, 0), mask)

    # Embed PNG in SVG
    buffer = io.BytesIO()
    master.save(buffer, format='PNG')
    b64_png = base64.b64encode(buffer.getvalue()).decode('utf-8')

    svg_content = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <image width="512" height="512" href="data:image/png;base64,{b64_png}"/>
</svg>'''

    targets = [
        'frontend/public',
        'deploy-wbagency',
        'frontend/dist/frontend/browser'
    ]

    for t in targets:
        if not os.path.exists(t):
            continue
        # Multi-size ICO (16, 32, 48, 64)
        master.save(os.path.join(t, 'favicon.ico'), format='ICO', sizes=[(16, 16), (32, 32), (48, 48), (64, 64)])
        # PNG sizes
        master.resize((32, 32), Image.Resampling.LANCZOS).save(os.path.join(t, 'favicon-32x32.png'))
        master.resize((16, 16), Image.Resampling.LANCZOS).save(os.path.join(t, 'favicon-16x16.png'))
        master.resize((180, 180), Image.Resampling.LANCZOS).save(os.path.join(t, 'apple-touch-icon.png'))
        # SVG
        with open(os.path.join(t, 'favicon.svg'), 'w', encoding='utf-8') as f:
            f.write(svg_content)
        print(f'Successfully generated favicons for {t}')

    # Also root or frontend/src if favicon.ico exists there
    if os.path.exists('frontend/src/favicon.ico'):
        master.save('frontend/src/favicon.ico', format='ICO', sizes=[(16, 16), (32, 32), (48, 48), (64, 64)])
        print('Saved to frontend/src/favicon.ico')

if __name__ == '__main__':
    generate_favicons()
