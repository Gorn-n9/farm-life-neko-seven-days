package farmlife.ui;

import farmlife.model.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.prefs.Preferences;

public final class GameWindow extends JPanel {
    private static final Color INK = new Color(36, 65, 54),
            CREAM = new Color(248, 246, 234),
            GREEN = new Color(50, 100, 73);
    private Game game;
    private final SproutAssets art = new SproutAssets();
    private int animationFrame;
    private final FarmAnimation farming = new FarmAnimation();
    private long lastAnimationTime;
    private final Preferences prefs = Preferences.userNodeForPackage(GameWindow.class);
    private int unlocked = Math.clamp(prefs.getInt("unlocked", 0), 0, 2);
    private final JLabel title = new JLabel(), clock = new JLabel(), objective = new JLabel();
    private final JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
    private final JPanel actions = new JPanel();
    private final JTextArea info = new JTextArea(), journal = new JTextArea();
    private final JLabel location = new JLabel(), inventory = new JLabel();
    private final MapPanel map = new MapPanel();
    private final Timer animation =
            new Timer(
                    50,
                    e -> {
                        long now = System.nanoTime();
                        advanceAnimation(
                                (int) Math.min(250, (now - lastAnimationTime) / 1_000_000));
                        lastAnimationTime = now;
                    });
    private final JComboBox<String> levels = new JComboBox<>();
    private boolean changingLevel;

    public GameWindow() {
        this(new Game(0));
    }

    public GameWindow(Game initialGame) {
        game = initialGame;
        setLayout(new BorderLayout(18, 18));
        setBackground(CREAM);
        setBorder(new EmptyBorder(24, 28, 22, 28));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(3, 1, 0, 4));
        heading.setOpaque(false);
        JLabel brand = new JLabel("F A R M   L I F E     /     NEKO'S SEVEN DAYS");
        brand.setFont(font(11, Font.BOLD));
        brand.setForeground(GREEN);
        title.setFont(font(29, Font.BOLD));
        title.setForeground(INK);
        objective.setFont(font(13, Font.PLAIN));
        heading.add(brand);
        heading.add(title);
        heading.add(objective);
        header.add(heading, BorderLayout.CENTER);
        JPanel controls = new JPanel(new GridLayout(3, 1, 4, 6));
        controls.setOpaque(false);
        clock.setFont(font(19, Font.BOLD));
        clock.setHorizontalAlignment(SwingConstants.RIGHT);
        controls.add(clock);
        for (int i = 0; i < 3; i++) levels.addItem("0" + (i + 1) + "  " + new Level(i).name);
        levels.addActionListener(
                e -> {
                    if (changingLevel) return;
                    if (farming.active()) {
                        selectLevel(game.level.index);
                        return;
                    }
                    int i = levels.getSelectedIndex();
                    if (i > unlocked) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Finish the previous farm with its target score to unlock this map.");
                        selectLevel(game.level.index);
                        return;
                    }
                    if (i != game.level.index && confirmRestart()) {
                        game = new Game(i);
                        refresh();
                    } else selectLevel(game.level.index);
                });
        controls.add(levels);
        JButton help =
                button(
                        "How to play",
                        () ->
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Click a map destination to travel, then choose an activity.\nTravel and activities consume time; the day ends at 22:00.\nPlant and water on day 1. After two watered nights, harvest and sell.\nCare for animals and study at most once per day.\n30 knowledge unlocks larger harvests. Rest at the park or eat at the market.\nSleep at home; ending a day elsewhere costs up to 10 coins and 8 happiness.\nScore = coins + happiness x2 + knowledge x3 + harvest batches x40.\nReach the target after seven days to unlock the next farm.\nOnly unlocked maps are saved. An active week is not saved.\n\nMap: water = river, rocks = blocked, dirt = mud, steps = slope.\nThe dotted line shows your latest route.\n\nArt: Sprout Lands by Cup Nooble (Basic Pack).\n\n"
                                                + game.level.briefing,
                                        "Field guide",
                                        JOptionPane.INFORMATION_MESSAGE));
        controls.add(help);
        header.add(controls, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        stats.setOpaque(false);
        top.add(stats, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.add(map, BorderLayout.CENTER);
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setPreferredSize(new Dimension(320, 500));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel context = new JPanel(new GridLayout(3, 1, 0, 4));
        context.setPreferredSize(new Dimension(280, 74));
        context.setOpaque(false);
        JLabel tag = new JLabel("YOUR NEXT MOVE");
        tag.setFont(font(11, Font.BOLD));
        tag.setForeground(GREEN);
        context.add(tag);
        location.setFont(font(23, Font.BOLD));
        context.add(location);
        inventory.setFont(font(12, Font.PLAIN));
        context.add(inventory);
        sidebar.add(context, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setOpaque(false);
        mid.add(actions, BorderLayout.NORTH);
        setupText(info, 13);
        JScrollPane details = new JScrollPane(info);
        details.setBorder(null);
        details.setOpaque(false);
        details.getViewport().setOpaque(false);
        details.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mid.add(details, BorderLayout.CENTER);
        sidebar.add(mid, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(2, 1, 0, 8));
        bottom.setOpaque(false);
        bottom.add(
                button(
                        "End day / recover",
                        () -> {
                            if (game.finished()) return;
                            String note =
                                    game.place() == Level.Place.HOME
                                            ? "Sleep and start the next day?"
                                            : "Return home now? Rescue costs up to 10 coins and 8 happiness.";
                            if (JOptionPane.showConfirmDialog(
                                            this, note, "End day", JOptionPane.OK_CANCEL_OPTION)
                                    == JOptionPane.OK_OPTION) {
                                game.endDay();
                                refresh();
                            }
                        }));
        bottom.add(
                button(
                        "Restart this farm",
                        () -> {
                            if (confirmRestart()) {
                                game = new Game(game.level.index);
                                refresh();
                            }
                        }));
        sidebar.add(bottom, BorderLayout.SOUTH);
        center.add(sidebar, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        setupText(journal, 12);
        journal.setRows(4);
        journal.setPreferredSize(new Dimension(800, 100));
        journal.setBackground(new Color(236, 239, 225));
        journal.setBorder(new EmptyBorder(10, 14, 10, 14));
        add(journal, BorderLayout.SOUTH);
        selectLevel(game.level.index);
        refresh();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refresh();
        lastAnimationTime = System.nanoTime();
        animation.start();
    }

    @Override
    public void removeNotify() {
        animation.stop();
        farming.clear();
        super.removeNotify();
    }

    void advanceAnimation(int milliseconds) {
        animationFrame++;
        if (farming.advance(milliseconds)) refresh();
        map.repaint();
    }

    boolean farmingActive() {
        return farming.active();
    }

    private void performAction(Game.Action action) {
        if (farming.active()) return;
        int before = game.minute();
        game.act(action);
        if (game.minute() > before
                && (action == Game.Action.PLANT
                        || action == Game.Action.WATER
                        || action == Game.Action.HARVEST)) farming.start(action);
        refresh();
    }

    private void selectLevel(int i) {
        changingLevel = true;
        levels.setSelectedIndex(i);
        changingLevel = false;
    }

    private boolean confirmRestart() {
        return game.finished()
                || JOptionPane.showConfirmDialog(
                                this,
                                "The current week will be reset. Continue?",
                                "New week",
                                JOptionPane.OK_CANCEL_OPTION)
                        == JOptionPane.OK_OPTION;
    }

    private static Font font(int size, int style) {
        return new Font("SansSerif", style, size);
    }

    private static void setupText(JTextArea a, int size) {
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(font(size, Font.PLAIN));
        a.setForeground(INK);
        a.setOpaque(false);
    }

    private JButton button(String s, Runnable r) {
        JButton b = new JButton(s);
        b.setFont(font(12, Font.BOLD));
        b.setForeground(INK);
        b.setBackground(new Color(230, 236, 216));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(
                e -> {
                    if (!farming.active()) r.run();
                });
        return b;
    }

    private void refresh() {
        title.setText(game.level.name);
        clock.setText("DAY " + game.day() + " / 7     " + game.time());
        objective.setText(
                "CHAPTER 0"
                        + (game.level.index + 1)
                        + "    /    Target "
                        + game.level.target
                        + " points    /    Current "
                        + game.score());
        stats.removeAll();
        stat("COINS", game.player.money(), new Color(196, 144, 59), false);
        stat("ENERGY", game.player.energy(), GREEN, true);
        stat("HAPPINESS", game.player.happiness(), new Color(204, 124, 98), true);
        stat("KNOWLEDGE", game.player.knowledge(), new Color(100, 138, 158), true);
        location.setText(pretty(game.place()));
        inventory.setText(
                "Seeds "
                        + game.seeds()
                        + "   /   Produce "
                        + game.produce()
                        + "   /   Harvests "
                        + game.harvests());
        actions.removeAll();
        if (!game.finished())
            for (Game.Action a : Game.Action.values())
                if (game.required(a) == game.place()) {
                    JButton b = button(game.label(a), () -> performAction(a));
                    b.setEnabled(!farming.active());
                    b.setAlignmentX(Component.LEFT_ALIGNMENT);
                    b.setMaximumSize(new Dimension(300, 34));
                    b.setPreferredSize(new Dimension(280, 34));
                    actions.add(b);
                    actions.add(Box.createVerticalStrut(6));
                }
        String crop =
                !game.crop.planted()
                        ? "Empty field"
                        : game.crop.ripe()
                                ? "Ready to harvest"
                                : "Growth "
                                        + game.crop.growth()
                                        + "/2 · "
                                        + (game.crop.watered() ? "watered" : "needs water");
        info.setText(
                game.message()
                        + "\n\nFIELD STATUS\n"
                        + crop
                        + "\n\nTHIS FARM\n"
                        + game.level.briefing);
        if (game.finished()) {
            if (game.passed()) {
                unlocked = Math.max(unlocked, Math.min(2, game.level.index + 1));
                try {
                    prefs.putInt("unlocked", unlocked);
                } catch (SecurityException ignored) {
                }
            }
            location.setText("Grade " + game.grade() + " · " + game.score() + " pts");
            info.setText(
                    (game.passed() ? "TARGET REACHED!" : "A NEW WEEK AWAITS")
                            + "\n\nCoins: "
                            + game.player.money()
                            + "\nHappiness: "
                            + game.player.happiness()
                            + " x 2\nKnowledge: "
                            + game.player.knowledge()
                            + " x 3\nHarvest batches: "
                            + game.harvests()
                            + " x 40\n\n"
                            + (game.passed()
                                    ? (game.level.index == 2
                                            ? "All three farms complete. Well done, Neko!"
                                            : "The next farm is unlocked.")
                                    : "Target: "
                                            + game.level.target
                                            + ". Try planting early and studying each day."));
            if (game.passed() && game.level.index < 2)
                actions.add(
                        button(
                                "Continue to next farm →",
                                () -> {
                                    game = new Game(game.level.index + 1);
                                    selectLevel(game.level.index);
                                    refresh();
                                }));
        }
        info.setCaretPosition(0);
        journal.setText(
                "FARM JOURNAL\n" + String.join("\n", game.journal().stream().limit(3).toList()));
        revalidate();
        repaint();
    }

    private void stat(String label, int value, Color color, boolean bar) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel l = new JLabel(label);
        l.setFont(font(10, Font.BOLD));
        l.setForeground(color);
        p.add(l, BorderLayout.NORTH);
        JLabel n = new JLabel(Integer.toString(value));
        n.setFont(font(24, Font.BOLD));
        n.setForeground(INK);
        p.add(n, BorderLayout.CENTER);
        if (bar) {
            JProgressBar b = new JProgressBar(0, 100);
            b.setValue(value);
            b.setForeground(color);
            b.setPreferredSize(new Dimension(100, 5));
            p.add(b, BorderLayout.SOUTH);
        }
        stats.add(p);
    }

    private static String pretty(Level.Place p) {
        return switch (p) {
            case HOME -> "Neko's cottage";
            case FIELD -> "Crop garden";
            case BARN -> "Animal barn";
            case SCHOOL -> "Farm school";
            case MARKET -> "Village market";
            case PARK -> "Resting grove";
        };
    }

    private final class MapPanel extends JPanel {
        private int cell, ox, oy;
        private Level.Place hovered;

        MapPanel() {
            setBackground(new Color(226, 235, 205));
            setPreferredSize(new Dimension(690, 510));
            setToolTipText("Click a destination to travel");
            addMouseListener(
                    new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            Level.Place p = hit(e.getX(), e.getY());
                            if (p != null && !game.finished() && !farming.active()) {
                                game.travel(p);
                                refresh();
                            }
                        }
                    });
            addMouseMotionListener(
                    new MouseMotionAdapter() {
                        public void mouseMoved(MouseEvent e) {
                            hovered = hit(e.getX(), e.getY());
                            setCursor(
                                    Cursor.getPredefinedCursor(
                                            hovered == null
                                                    ? Cursor.DEFAULT_CURSOR
                                                    : Cursor.HAND_CURSOR));
                            setToolTipText(
                                    hovered == null
                                            ? "Click a building to travel"
                                            : pretty(hovered)
                                                    + " · "
                                                    + game.travelEstimate(hovered)
                                                    + " min travel");
                            repaint();
                        }
                    });
        }

        private Level.Place hit(int x, int y) {
            for (Level.Place p : Level.Place.values()) {
                Point q = game.level.location(p);
                if (new Rectangle(
                                ox + q.x * cell - cell / 2,
                                oy + q.y * cell - cell / 2,
                                cell * 2,
                                cell * 2)
                        .contains(x, y)) return p;
            }
            return null;
        }

        protected void paintComponent(Graphics raw) {
            super.paintComponent(raw);
            Graphics2D g = (Graphics2D) raw.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cell = Math.max(24, Math.min((getWidth() - 42) / 12, (getHeight() - 80) / 9));
            ox = (getWidth() - 12 * cell) / 2;
            oy = 38 + (getHeight() - 80 - 9 * cell) / 2;
            g.setFont(font(11, Font.BOLD));
            g.setColor(INK);
            g.drawString("THE FARM   /   CLICK A DESTINATION", 18, 23);
            g.setFont(font(10, Font.PLAIN));
            g.drawString("Sprout Lands / Cup Nooble", getWidth() - 170, 23);
            for (int y = 0; y < 9; y++)
                for (int x = 0; x < 12; x++) {
                    int px = ox + x * cell, py = oy + y * cell;
                    art.grass(g, px, py, cell, (x * 7 + y * 3) % 4);
                    Obstacle o = game.level.at(x, y, game.day());
                    if (o instanceof Obstacle.Water) art.water(g, px, py, cell, animationFrame / 4);
                    if (o instanceof Obstacle.Rock) art.rock(g, px, py, cell);
                    if (o instanceof Obstacle.Mud) {
                        art.dirt(g, px, py + cell / 5, cell, cell * 3 / 5);
                        g.setColor(new Color(125, 98, 70));
                        g.drawString("~", px + cell / 2, py + cell / 2);
                    }
                    if (o instanceof Obstacle.Slope)
                        art.draw(g, "Tilesets/Hills.png", 144, 80, 32, 32, px, py, cell, cell);
                    if (game.level.index == 1 && x == 6 && o == null) {
                        art.water(g, px, py, cell, animationFrame / 4);
                        art.bridge(g, px, py, cell);
                    }
                    if (o == null && y == 8 && x % 3 == 0)
                        art.draw(
                                g,
                                "Objects/Basic Grass Biom things 1.png",
                                112,
                                32,
                                16,
                                16,
                                px,
                                py,
                                cell * 2 / 3,
                                cell * 2 / 3);
                }
            g.setColor(new Color(249, 242, 207));
            for (Point p : game.lastRoute())
                g.fillOval(ox + p.x * cell + cell / 2 - 3, oy + p.y * cell + cell / 2 - 3, 6, 6);
            for (Level.Place p : Level.Place.values()) {
                Point q = game.level.location(p);
                int x = ox + q.x * cell, y = oy + q.y * cell;
                if (hovered == p || game.place() == p) {
                    g.setColor(new Color(252, 237, 171));
                    g.fillRoundRect(x - 8, y - 8, cell + 16, cell + 18, 15, 15);
                }
                drawBuilding(g, p, x, y, cell);
                g.setFont(font(10, Font.BOLD));
                String label =
                        switch (p) {
                            case HOME -> "COTTAGE";
                            case FIELD -> "GARDEN";
                            case BARN -> "BARN";
                            case SCHOOL -> "SCHOOL";
                            case MARKET -> "MARKET";
                            case PARK -> "GROVE";
                        };
                int w = g.getFontMetrics().stringWidth(label);
                g.setColor(new Color(250, 247, 224));
                g.fillRoundRect(x + cell / 2 - w / 2 - 5, y + cell + 3, w + 10, 17, 7, 7);
                g.setColor(INK);
                g.drawString(label, x + cell / 2 - w / 2, y + cell + 15);
            }
            Point pos = game.level.location(game.place());
            int cx = ox + pos.x * cell + cell + 2, cy = oy + pos.y * cell;
            if (farming.active()) {
                art.farmingCat(
                        g,
                        cx,
                        cy,
                        Math.max(16, cell / 2),
                        farming.action() == Game.Action.WATER,
                        animationFrame / 4);
                String caption =
                        switch (farming.action()) {
                            case PLANT -> "Planting...";
                            case WATER -> "Watering...";
                            default -> "Harvesting...";
                        };
                g.setColor(INK);
                g.setFont(font(12, Font.BOLD));
                g.drawString(caption, 18, 46);
                g.setColor(GREEN);
                g.fillRect(18, 53, (int) (110 * farming.progress()), 4);
            } else drawCat(g, cx, cy, Math.max(24, cell * 2 / 3));
            g.setColor(INK);
            g.setFont(font(11, Font.PLAIN));
            g.drawString(
                    "Rocks block · Bridges cross rivers · Mud +20m · Steps +15m",
                    18,
                    getHeight() - 14);
            g.dispose();
        }
    }

    private void drawBuilding(Graphics2D g, Level.Place p, int x, int y, int s) {
        if (p == Level.Place.FIELD) {
            CropPlotRenderer.draw(g, art, game.crop, CropPlotRenderer.bounds(x, y, s), farming);
            return;
        }
        if (p == Level.Place.PARK) {
            art.tree(g, x - s / 3, y - s / 3, s * 4 / 3, true);
            art.draw(
                    g,
                    "Objects/Basic Furniture.png",
                    48,
                    48,
                    16,
                    16,
                    x + s / 2,
                    y + s / 2,
                    s / 2,
                    s / 2);
            return;
        }
        if (p == Level.Place.BARN) {
            art.draw(
                    g,
                    "Objects/Free_Chicken_House.png",
                    0,
                    0,
                    48,
                    48,
                    x - s / 4,
                    y - s / 3,
                    s * 3 / 2,
                    s * 3 / 2);
            art.draw(
                    g,
                    "Characters/Free Chicken Sprites.png",
                    ((animationFrame / 4) % 2) * 16,
                    0,
                    16,
                    16,
                    x - s / 2,
                    y + s / 2,
                    s * 2 / 3,
                    s * 2 / 3);
            art.draw(
                    g,
                    "Characters/Free Cow Sprites.png",
                    ((animationFrame / 4) % 3) * 32,
                    0,
                    32,
                    32,
                    x + s,
                    y + s / 3,
                    s,
                    s);
            return;
        }
        art.house(g, x - s / 6, y - s / 6, s * 4 / 3);
        if (p == Level.Place.SCHOOL) {
            art.draw(g, "Objects/Basic Furniture.png", 0, 0, 16, 16, x, y + s / 3, s / 2, s / 2);
            g.setColor(new Color(83, 113, 141));
            g.fillRect(x + s - 2, y - s / 3, 3, s / 2);
            g.fillRect(x + s + 1, y - s / 3, s / 3, s / 5);
        }
        if (p == Level.Place.MARKET) {
            art.draw(
                    g,
                    "Objects/Basic Furniture.png",
                    48,
                    48,
                    16,
                    16,
                    x - s / 4,
                    y + s / 2,
                    s,
                    s / 2);
            art.draw(
                    g,
                    "Objects/Basic Plants.png",
                    80,
                    16,
                    16,
                    16,
                    x - s / 8,
                    y + s / 3,
                    s / 2,
                    s / 2);
        }
    }

    private void drawCat(Graphics2D g, int x, int y, int s) {
        if (catImage != null) {
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(catImage, x, y, s, s, null);
            return;
        }
        art.cat(g, x, y, s, animationFrame / 4, 0);
    }

    private final BufferedImage catImage = loadCat();

    private BufferedImage loadCat() {
        try {
            return javax.imageio.ImageIO.read(Path.of("assets", "neko.png").toFile());
        } catch (Exception e) {
            return null;
        }
    }

    public void renderPreview(Path path) throws Exception {
        renderPreview(path, 1160, 830);
    }

    public void renderPreview(Path path, int width, int height) throws Exception {
        setSize(width, height);
        layoutAll(this);
        BufferedImage image =
                new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        printAll(g);
        g.dispose();
        javax.imageio.ImageIO.write(image, "png", path.toFile());
    }

    private static void layoutAll(Container c) {
        c.doLayout();
        for (Component child : c.getComponents())
            if (child instanceof Container sub) layoutAll(sub);
    }
}
