package farmlife.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class SproutAssets {
    private final Map<String, BufferedImage> sheets = new HashMap<>(), frames = new HashMap<>();

    public SproutAssets() {
        String[] files = {
            "Characters/Basic Charakter Actions.png",
            "Characters/Basic Charakter Spritesheet.png",
            "Characters/Free Chicken Sprites.png",
            "Characters/Free Cow Sprites.png",
            "Objects/Basic Grass Biom things 1.png",
            "Objects/Basic Plants.png",
            "Objects/Free_Chicken_House.png",
            "Objects/Wood Bridge.png",
            "Objects/Basic Furniture.png",
            "Tilesets/Grass.png",
            "Tilesets/Hills.png",
            "Tilesets/Water.png",
            "Tilesets/Tilled Dirt.png",
            "Tilesets/Wooden House.png"
        };
        for (String file : files)
            try {
                BufferedImage image = ImageIO.read(Path.of("assets", "sprout", file).toFile());
                if (image == null) throw new IOException("Unsupported image");
                sheets.put(file, image);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Missing game asset: "
                                + file
                                + ". Run from the project directory using run.bat.",
                        e);
            }
    }

    public BufferedImage frame(String file, int x, int y, int w, int h) {
        String key = file + ":" + x + ":" + y + ":" + w + ":" + h;
        return frames.computeIfAbsent(key, k -> sheets.get(file).getSubimage(x, y, w, h));
    }

    public void draw(
            Graphics2D g, String file, int sx, int sy, int sw, int sh, int x, int y, int w, int h) {
        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(frame(file, sx, sy, sw, sh), x, y, w, h, null);
    }

    public void grass(Graphics2D g, int x, int y, int s, int variant) {
        draw(g, "Tilesets/Grass.png", 16 * (variant % 4), 80, 16, 16, x, y, s, s);
    }

    public void water(Graphics2D g, int x, int y, int s, int tick) {
        draw(g, "Tilesets/Water.png", 16 * (tick % 4), 0, 16, 16, x, y, s, s);
    }

    public void rock(Graphics2D g, int x, int y, int s) {
        draw(g, "Objects/Basic Grass Biom things 1.png", 128, 16, 16, 16, x, y, s, s);
    }

    public void dirt(Graphics2D g, int x, int y, int w, int h) {
        draw(g, "Tilesets/Tilled Dirt.png", 16, 64, 32, 16, x, y, w, h);
    }

    public void bridge(Graphics2D g, int x, int y, int s) {
        draw(g, "Objects/Wood Bridge.png", 32, 0, 48, 16, x - s / 3, y + s / 4, s * 5 / 3, s / 2);
    }

    public void tree(Graphics2D g, int x, int y, int s, boolean fruit) {
        draw(g, "Objects/Basic Grass Biom things 1.png", fruit ? 48 : 16, 0, 32, 32, x, y, s, s);
    }

    public void house(Graphics2D g, int x, int y, int s) {
        draw(g, "Tilesets/Wooden House.png", 16, 32, 16, 16, x, y + s / 3, s, s * 2 / 3);
        draw(
                g,
                "Tilesets/Wooden House.png",
                80,
                16,
                16,
                16,
                x - s / 8,
                y - s / 6,
                s * 5 / 4,
                s * 2 / 3);
        draw(g, "Tilesets/Wooden House.png", 16, 0, 16, 16, x + s / 8, y + s / 2, s / 3, s / 3);
        draw(g, "Tilesets/Wooden House.png", 48, 16, 16, 16, x + s / 2, y + s / 2, s / 3, s / 2);
    }

    public void farmingCat(Graphics2D g, int x, int y, int size, boolean watering, int tick) {
        draw(
                g,
                "Characters/Basic Charakter Actions.png",
                (tick % 2) * 48,
                watering ? 480 : 96,
                48,
                48,
                x - size,
                y - size / 2,
                size * 3,
                size * 3);
    }

    public void cat(Graphics2D g, int x, int y, int size, int tick, int direction) {
        draw(
                g,
                "Characters/Basic Charakter Spritesheet.png",
                (tick % 4) * 48 + 16,
                direction * 48 + 12,
                16,
                24,
                x,
                y,
                size,
                size * 3 / 2);
    }
}
