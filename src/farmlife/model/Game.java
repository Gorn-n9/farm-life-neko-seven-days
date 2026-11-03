package farmlife.model;

import java.awt.Point;
import java.util.*;
import static farmlife.model.Level.Place.*;

public final class Game {
    public enum Action { PLANT, WATER, HARVEST, FEED, STUDY, SELL, SEEDS, FOOD, REST, SLEEP }
    public final Player player = new Player();
    public final Crop crop = new Crop();
    public final Level level;
    private int day=1, minute=480, seeds=3, produce, harvests, animalDay, studyDay;
    private boolean finished;
    private Level.Place place=HOME;
    private String message="Welcome, Neko! Plant early, water daily, and keep time for the trip home.";
    private final Deque<String> journal=new ArrayDeque<>();
    private java.util.List<Point> lastRoute=java.util.List.of();
    public Game(int index) { level=new Level(index); log(message); }
    public int day(){return day;} public int minute(){return minute;}
    public int seeds(){return seeds;} public int produce(){return produce;} public int harvests(){return harvests;}
    public boolean finished(){return finished;} public Level.Place place(){return place;}
    public String message(){return message;} public java.util.List<String> journal(){return java.util.List.copyOf(journal);}
    public java.util.List<Point> lastRoute(){return lastRoute;}
    public String time(){return String.format("%02d:%02d",minute/60,minute%60);}
    public int score(){return player.money()+player.happiness()*2+player.knowledge()*3+harvests*40;}
    public String grade(){int s=score();return s>=900?"S":s>=720?"A":s>=580?"B":s>=420?"C":"D";}
    public boolean passed(){return finished&&score()>=level.target;}
    private void log(String s){message=s;journal.addFirst("D"+day+" "+time()+"  "+s);while(journal.size()>6)journal.removeLast();}
    public int travelEstimate(Level.Place to){return level.travelCost(level.route(level.location(place),level.location(to),day),day);}
    public void travel(Level.Place to){
        if(finished)return;
        if(to==place){log("Already here. Choose an activity below.");return;}
        var path=level.route(level.location(place),level.location(to),day);
        if(path.isEmpty()){log("That route is blocked today.");return;}
        int cost=level.travelCost(path,day),energy=(path.size()+2)/3;
        if(!canSpend(cost,energy,0))return;
        minute+=cost;player.change(0,-energy,0,0);place=to;lastRoute=path;
        log("Arrived at "+to.name().toLowerCase()+". Travel: "+cost+" min, "+energy+" energy.");
    }
    private boolean canSpend(int time,int energy,int money){
        if(minute+time>1320){log("Not enough time before 22:00. End the day to recover.");return false;}
        if(player.energy()<energy){log("Too tired. Rest, eat, or end the day.");return false;}
        if(player.money()<money){log("Not enough coins for that activity.");return false;}
        return true;
    }
    public Level.Place required(Action a){return switch(a){
        case PLANT,WATER,HARVEST->FIELD;case FEED->BARN;case STUDY->SCHOOL;
        case SELL,SEEDS,FOOD->MARKET;case REST->PARK;case SLEEP->HOME;};}
    public String label(Action a){return switch(a){
        case PLANT->"Plant batch · 60m / 12 energy";case WATER->"Water crops · 30m / 8 energy";
        case HARVEST->"Harvest · 60m / 15 energy";case FEED->"Care for animals · 60m / 10 energy";
        case STUDY->"Study · 90m / 8 energy / 10 coins";case SELL->"Sell all produce · 30m";
        case SEEDS->"Buy 3 seeds · 15m / 18 coins";case FOOD->"Eat meal · 30m / 15 coins";
        case REST->"Rest · 60m / +25 energy";case SLEEP->"Sleep until tomorrow";};}
    public void act(Action a){
        if(finished)return;
        if(place!=required(a)){log("Visit the "+required(a).name().toLowerCase()+" first.");return;}
        if(a==Action.SLEEP){endDay();return;}
        int time=0,energy=0,cash=0,joy=0,knowledge=0;String result;
        switch(a){
            case PLANT->{if(crop.planted()){log("The field already has crops.");return;}if(seeds==0){log("Buy seeds at the market first.");return;}time=60;energy=12;result="Planted a crop batch. Water it today.";}
            case WATER->{if(!crop.planted()||crop.ripe()||crop.watered()){log("No crops need watering right now.");return;}time=30;energy=8;result="Watered! Crops grow overnight.";}
            case HARVEST->{if(!crop.ripe()){log("Crops need two watered nights before harvest.");return;}time=60;energy=15;joy=5;result="Harvested! Sell your produce at the market.";}
            case FEED->{if(animalDay==day){log("Animals have already been cared for today.");return;}time=60;energy=10;cash=-(level.index==1&&day==5?15:5);joy=6;result="Happy animals! Collected 2 produce.";}
            case STUDY->{if(studyDay==day){log("You have finished today's lesson.");return;}time=90;energy=8;cash=-10;knowledge=15;result="Knowledge +15. At 30 knowledge, harvest yield improves.";}
            case SELL->{if(produce==0){log("There is no produce to sell.");return;}time=30;cash=produce*(level.priceCrash(day)?9:18);result="Sold "+produce+" produce for "+cash+" coins.";}
            case SEEDS->{time=15;cash=-18;result="Bought 3 seeds.";}
            case FOOD->{time=30;cash=-15;energy=-30;joy=5;result="A warm meal. Energy +30, happiness +5.";}
            case REST->{time=60;energy=-25;joy=8;result="A quiet moment. Energy +25, happiness +8.";}
            default->throw new IllegalStateException();
        }
        if(!canSpend(time,Math.max(0,energy),Math.max(0,-cash)))return;
        minute+=time;player.change(cash,-energy,joy,knowledge);
        switch(a){case PLANT->{seeds--;crop.plant();}case WATER->crop.water();case HARVEST->{produce+=player.knowledge()>=30?6:4;harvests++;crop.harvest();}case FEED->{animalDay=day;produce+=2;}case STUDY->studyDay=day;case SELL->produce=0;case SEEDS->seeds+=3;default->{}}
        log(result);
    }
    public void endDay(){
        if(finished)return;
        if(place!=HOME)player.change(-Math.min(10,player.money()),0,-8,0);
        crop.overnight(level.drought(day));
        if(day==7){finished=true;log("Week complete! Score "+score()+" / Grade "+grade()+".");return;}
        day++;minute=480;place=HOME;lastRoute=java.util.List.of();
        player.change(0,65,-5,0);
        String event="New day. Water your crops and plan your route.";
        if(day==3&&level.index==0){player.change(-Math.min(15,player.money()),0,-4,0);event="Pests! Treatment cost up to 15 coins. The field is safe.";}
        if(day==3&&level.index==1)event="Flood warning! The lower river crossing is closed today.";
        if(day==3&&level.index==2){player.change(0,-15,-5,0);event="Mountain storm! Energy -15 and happiness -5.";}
        if(level.drought(day))event="Drought today! Unwatered crops lose one growth stage tonight.";
        if(level.priceCrash(day))event="Market slump! Produce sells for 9 coins instead of 18 today.";
        if(day==5&&level.index==1)event="Feed shortage! Animal care costs 15 coins today.";
        log(event);
    }
}
