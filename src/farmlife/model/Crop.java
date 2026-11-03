package farmlife.model;

public final class Crop {
    private int growth;
    private boolean planted, watered;
    public boolean planted() { return planted; }
    public boolean watered() { return watered; }
    public boolean ripe() { return planted && growth >= 2; }
    public int growth() { return growth; }
    public void plant() { planted = true; growth = 0; watered = false; }
    public void water() { watered = true; }
    public void overnight(boolean drought) {
        if (planted && watered) growth = Math.min(2, growth + 1);
        else if (planted && drought) growth = Math.max(0, growth - 1);
        watered = false;
    }
    public void harvest() { planted = false; growth = 0; watered = false; }
}
