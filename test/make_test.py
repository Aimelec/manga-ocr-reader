from PIL import Image, ImageDraw, ImageFont
font = ImageFont.truetype("C:/Windows/Fonts/msgothic.ttc", 40)
cols = ["明日の朝、", "駅の前で", "待っている。"]
img = Image.new("RGB", (60 * len(cols) + 40, 40 * 7 + 40), "white")
d = ImageDraw.Draw(img)
for i, col in enumerate(cols):
    x = img.width - 60 * (i + 1)
    for j, ch in enumerate(col):
        d.text((x, 20 + 40 * j), ch, font=font, fill="black")
img.save("test/vertical.png")
