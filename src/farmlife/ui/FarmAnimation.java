package farmlife.ui;

import farmlife.model.Game;

final class FarmAnimation {
    static final int DURATION = 1800;
    private Game.Action action;
    private int elapsed;

    boolean active() {
        return action != null;
    }

    Game.Action action() {
        return action;
    }

    double progress() {
        return Math.min(1.0, elapsed / (double) DURATION);
    }

    void start(Game.Action value) {
        action = value;
        elapsed = 0;
    }

    boolean advance(int milliseconds) {
        if (!active()) return false;
        elapsed += Math.max(0, milliseconds);
        if (elapsed >= DURATION) {
            clear();
            return true;
        }
        return false;
    }

    void clear() {
        action = null;
        elapsed = 0;
    }
}
