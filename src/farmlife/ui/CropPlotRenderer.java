package farmlife.ui;

import farmlife.model.Crop;
import farmlife.model.Game;
import java.awt.*;

final class CropPlotRenderer {
    static Rectangle bounds(int x,int y,int cell){return new Rectangle(x-cell/4,y,cell*3/2,cell);}
    static Rectangle bed(Rectangle plot,int index){
        int pad=Math.max(2,plot.height/12),w=plot.width-2*pad,h=plot.height-2*pad;
        int col=index%3,row=index/3;
        int left=plot.x+pad+col*w/3,top=plot.y+pad+row*h/2;
        return new Rectangle(left,top,(col+1)*w/3-col*w/3,(row+1)*h/2-row*h/2);
    }
    static void draw(Graphics2D target,SproutAssets art,Crop crop,Rectangle plot,FarmAnimation animation){
        Graphics2D g=(Graphics2D)target.create();g.clip(plot);
        g.setColor(new Color(158,112,75));g.fillRect(plot.x,plot.y,plot.width,plot.height);
        g.setColor(new Color(204,162,107));g.drawRect(plot.x,plot.y,plot.width-1,plot.height-1);
        boolean active=animation.active();Game.Action action=animation.action();
        int completed=active?(int)(animation.progress()*6):6;
        for(int i=0;i<6;i++){
            Rectangle b=bed(plot,i);
            boolean wet=crop.watered()&&(!active||action!=Game.Action.WATER||i<completed);
            g.setColor(wet?new Color(125,95,70):new Color(183,139,91));
            g.fillRect(b.x+1,b.y+1,b.width-2,b.height-2);
            boolean visible=crop.planted();
            if(active&&action==Game.Action.PLANT)visible=i<completed;
            if(active&&action==Game.Action.HARVEST)visible=i>=completed;
            if(visible){
                int size=Math.max(1,Math.min(b.width-2,b.height-2));
                int stage=active&&action==Game.Action.HARVEST?3:crop.ripe()?3:crop.growth()+1;
                art.draw(g,"Objects/Basic Plants.png",stage*16,0,16,16,b.x+(b.width-size)/2,b.y+(b.height-size)/2,size,size);
            }
            if(wet){g.setColor(new Color(96,183,199));g.fillRect(b.x+b.width-3,b.y+b.height-3,2,2);}
        }
        if(active){
            Rectangle b=bed(plot,Math.min(5,completed));double phase=animation.progress()*6-completed;
            if(action==Game.Action.WATER){
                g.setColor(new Color(124,216,235));
                for(int k=0;k<4;k++){int dx=b.x+2+k*Math.max(1,(b.width-4)/4),dy=b.y+(int)((phase+k*.19)%1*b.height);g.fillRect(dx,dy,2,3);}
            }else{
                g.setColor(action==Game.Action.PLANT?new Color(249,223,136):new Color(255,240,171));
                int cy=b.y+(int)(phase*Math.max(1,b.height-3));g.fillRect(b.x+b.width/2,cy,3,3);
            }
        }
        g.dispose();
    }
}
