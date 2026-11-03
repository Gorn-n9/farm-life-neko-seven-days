package farmlife.model;

public final class Player {
    private int money = 80, energy = 85, happiness = 60, knowledge;

    public int money() {
        return money;
    }

    public int energy() {
        return energy;
    }

    public int happiness() {
        return happiness;
    }

    public int knowledge() {
        return knowledge;
    }

    public void change(int cash, int stamina, int joy, int learning) {
        if (money + cash < 0) throw new IllegalArgumentException("Not enough money");
        money += cash;
        energy = Math.clamp(energy + stamina, 0, 100);
        happiness = Math.clamp(happiness + joy, 0, 100);
        knowledge = Math.clamp(knowledge + learning, 0, 100);
    }
}
