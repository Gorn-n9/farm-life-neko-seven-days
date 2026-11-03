package farmlife.model;

public abstract class Obstacle {
    private final String name;
    protected Obstacle(String name) { this.name = name; }
    public String name() { return name; }
    public abstract boolean blocked();
    public abstract int travelMinutes();
    public static final class Rock extends Obstacle {
        public Rock() { super("Rock"); }
        public boolean blocked() { return true; }
        public int travelMinutes() { return 0; }
    }
    public static final class Water extends Obstacle {
        public Water() { super("River"); }
        public boolean blocked() { return true; }
        public int travelMinutes() { return 0; }
    }
    public static final class Mud extends Obstacle {
        public Mud() { super("Mud"); }
        public boolean blocked() { return false; }
        public int travelMinutes() { return 20; }
    }
    public static final class Slope extends Obstacle {
        public Slope() { super("Steep trail"); }
        public boolean blocked() { return false; }
        public int travelMinutes() { return 15; }
    }
}
