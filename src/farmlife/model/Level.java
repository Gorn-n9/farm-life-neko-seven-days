package farmlife.model;

import java.awt.Point;
import java.util.*;

public final class Level {
    public enum Place { HOME, FIELD, BARN, SCHOOL, MARKET, PARK }
    public static final int WIDTH = 12, HEIGHT = 9;
    public final int index, target;
    public final String name, subtitle, briefing;
    private final Obstacle[][] terrain = new Obstacle[HEIGHT][WIDTH];
    private final Map<Place, Point> places = new EnumMap<>(Place.class);
    public Level(int index) {
        this.index = index;
        name = new String[]{"Clover Meadow", "Willow River", "Amber Highlands"}[index];
        subtitle = new String[]{"A little farm. A fresh start.", "Across the river, a tougher week.", "High ground. Higher ambitions."}[index];
        target = new int[]{420, 500, 580}[index];
        briefing = new String[]{"Rocks block paths. Mud adds 20 min per tile. Day 3: pests cost 15 coins. Day 5: market prices fall.", "Use the bridge to cross. Day 3: flooding closes the lower crossing. Day 5: animal feed costs 10 extra coins.", "Slopes add 15 min per tile. Days 2, 4, 6: drought reduces unwatered crops. Day 3: storm costs 15 energy. Day 5: market prices fall."}[index];
        put(Place.HOME,1,1); put(Place.FIELD,3,3); put(Place.BARN,2,6);
        put(Place.SCHOOL,9,1); put(Place.MARKET,10,6); put(Place.PARK,8,7);
        if (index == 0) {
            for (int y=2;y<7;y++) terrain[y][6]=new Obstacle.Rock();
            terrain[4][6]=null;
            for(int x=3;x<6;x++) terrain[5][x]=new Obstacle.Mud();
        } else if(index == 1) {
            for(int y=0;y<HEIGHT;y++) terrain[y][6]=new Obstacle.Water();
            terrain[2][6]=null; terrain[7][6]=null;
            for(int x=7;x<10;x++) terrain[3][x]=new Obstacle.Mud();
            terrain[5][4]=new Obstacle.Rock(); terrain[6][4]=new Obstacle.Rock();
        } else {
            for(int x=4;x<10;x++) { terrain[4][x]=new Obstacle.Rock(); terrain[2][x]=new Obstacle.Slope(); }
            terrain[4][8]=new Obstacle.Slope();
            for(int y=5;y<8;y++) terrain[y][5]=new Obstacle.Rock();
            terrain[6][8]=new Obstacle.Mud(); terrain[7][7]=new Obstacle.Slope();
        }
    }
    private void put(Place p,int x,int y) { places.put(p,new Point(x,y)); }
    public Point location(Place p) { return new Point(places.get(p)); }
    public Obstacle at(int x,int y,int day) {
        if(index==1 && day==3 && x==6 && y==7) return new Obstacle.Water();
        return terrain[y][x];
    }
    public boolean drought(int day) { return index==2 && day%2==0; }
    public boolean priceCrash(int day) { return index!=1 && day==5; }
    public List<Point> route(Point start, Point end,int day) {
        int[][] dist = new int[HEIGHT][WIDTH];
        Point[][] previous = new Point[HEIGHT][WIDTH];
        for(int[] row:dist) Arrays.fill(row,Integer.MAX_VALUE);
        record Step(Point p,int cost) {}
        PriorityQueue<Step> queue=new PriorityQueue<>(Comparator.comparingInt(Step::cost));
        dist[start.y][start.x]=0; queue.add(new Step(start,0));
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
        while(!queue.isEmpty()) {
            Step s=queue.remove(); Point p=s.p();
            if(s.cost()!=dist[p.y][p.x]) continue;
            if(p.equals(end)) break;
            for(int[] d:dirs) {
                int x=p.x+d[0],y=p.y+d[1];
                if(x<0||y<0||x>=WIDTH||y>=HEIGHT) continue;
                Obstacle o=at(x,y,day); if(o!=null&&o.blocked()) continue;
                int cost=s.cost()+5+(o==null?0:o.travelMinutes());
                if(cost<dist[y][x]) { dist[y][x]=cost; previous[y][x]=p; queue.add(new Step(new Point(x,y),cost)); }
            }
        }
        if(dist[end.y][end.x]==Integer.MAX_VALUE) return List.of();
        LinkedList<Point> route=new LinkedList<>();
        for(Point p=end;!p.equals(start);p=previous[p.y][p.x]) route.addFirst(p);
        return route;
    }
    public int travelCost(List<Point> route,int day) {
        return route.stream().mapToInt(p->{Obstacle o=at(p.x,p.y,day);return 5+(o==null?0:o.travelMinutes());}).sum();
    }
}
