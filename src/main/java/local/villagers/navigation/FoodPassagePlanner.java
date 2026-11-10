package local.villagers.navigation;

import java.util.*;
import net.minecraft.core.BlockPos;

/** A bounded staircase search. Every blocked foot/head cell must have separate excavation authority. */
public final class FoodPassagePlanner {
    public interface Grid {
        /** 0 = clear/openable, positive = real digging cost, negative = forbidden. */
        int clearance(BlockPos p);
        boolean support(BlockPos p);
        boolean allowed(BlockPos feet);
        default void visited(BlockPos feet){}
        default List<BlockPos> excavation(BlockPos from,List<BlockPos> blocks){return blocks;}
        default List<BlockPos> excavation(BlockPos from,List<BlockPos> blocks,Set<BlockPos> cleared){return excavation(from,blocks);}
    }
    public record Step(BlockPos feet,List<BlockPos> dig) {}
    public record Plan(List<Step> steps,int digCount,int visited) {}
    /** Already-clear stretches can use native navigation; never walk through pending excavation. */
    public static int clearWalkEnd(List<Step> steps,int from,java.util.function.Predicate<BlockPos> blocked){
        int end=from;
        for(int i=from+1;i<steps.size();i++){
            if(steps.get(i).dig().stream().anyMatch(blocked))break;
            end=i;
        }
        return end;
    }
    private record Key(BlockPos p,int digs) {}
    private record Node(BlockPos p,int digs,double cost,double priority,Node parent,List<BlockPos> dig,Set<BlockPos> cleared) {}
    private static int clearance(Grid grid,Node node,BlockPos p){return node.cleared.contains(p)?0:grid.clearance(p);}
    public static Plan find(Grid grid,BlockPos start,Set<BlockPos> goals,int maxDigs,int maxVisited) {
        if(goals.isEmpty())return null;
        var open=new PriorityQueue<Node>(Comparator.comparingDouble(Node::priority));
        var costs=new HashMap<Key,Double>();
        var first=new Node(start.immutable(),0,0,estimate(start,goals)*8,null,List.of(),Set.of());
        open.add(first);costs.put(new Key(first.p,0),0.0);int visited=0;
        while(!open.isEmpty()&&visited++<maxVisited){
            var node=open.remove();if(node.cost>costs.getOrDefault(new Key(node.p,node.digs),Double.MAX_VALUE))continue;
            grid.visited(node.p);
            if(goals.contains(node.p)){
                var path=new ArrayList<Step>();for(var n=node;n.parent!=null;n=n.parent)path.add(new Step(n.p,n.dig));
                Collections.reverse(path);return new Plan(List.copyOf(path),node.digs,visited);
            }
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dz==0)continue;
                for(int dy=-3;dy<=1;dy++){
                    var p=node.p.offset(dx,dy,dz);if(!grid.allowed(p)||node.cleared.contains(p.below())||!grid.support(p.below()))continue;
                    int feet=clearance(grid,node,p),head=clearance(grid,node,p.above());if(feet<0||head<0)continue;
                    if(dy>0&&clearance(grid,node,node.p.above(2))!=0)continue;
                    if(dy<-1){
                        boolean clear=true;
                        for(int y=p.getY();y<=node.p.getY()+1;y++)if(clearance(grid,node,new BlockPos(p.getX(),y,p.getZ()))!=0){clear=false;break;}
                        if(!clear)continue;
                    }
                    // Diagonal steps cannot cut through an uncleared corner.
                    if(dx!=0&&dz!=0){
                        var a=node.p.offset(dx,0,0);var b=node.p.offset(0,0,dz);
                        if(clearance(grid,node,a)!=0||clearance(grid,node,a.above())!=0||clearance(grid,node,b)!=0||clearance(grid,node,b.above())!=0)continue;
                    }
                    var initial=new ArrayList<BlockPos>();
                    // A body must move past the lip before it falls. Clear the old head height too,
                    // otherwise native navigation correctly rejects an apparently two-block stair.
                    if(dy==-1){
                        int sweep=clearance(grid,node,p.above(2));if(sweep<0)continue;
                        if(sweep>0)initial.add(p.above(2));
                    }
                    if(head>0)initial.add(p.above());if(feet>0)initial.add(p);
                    var dig=grid.excavation(node.p,initial,node.cleared);if(dig==null)continue;
                    dig=dig.stream().filter(q->!node.cleared.contains(q)).distinct().toList();
                    int total=node.digs+dig.size();if(total>maxDigs)continue;
                    double digging=0;for(var block:dig)digging+=grid.clearance(block);
                    double cost=node.cost+(dx!=0&&dz!=0?1.42:1)+Math.abs(dy)*.2+digging*.12;
                    var key=new Key(p,total);if(cost>=costs.getOrDefault(key,Double.MAX_VALUE))continue;
                    var cleared=node.cleared;
                    if(!dig.isEmpty()){var changed=new HashSet<>(cleared);changed.addAll(dig);cleared=Set.copyOf(changed);}
                    costs.put(key,cost);open.add(new Node(p,total,cost,cost+estimate(p,goals)*8,node,List.copyOf(dig),cleared));
                }
            }
        }
        return null;
    }
    private static double estimate(BlockPos p,Set<BlockPos> goals){
        double best=Double.MAX_VALUE;
        for(var g:goals)best=Math.min(best,Math.max(Math.max(Math.abs(p.getX()-g.getX()),Math.abs(p.getZ()-g.getZ())),Math.abs(p.getY()-g.getY())));
        return best;
    }
    private FoodPassagePlanner(){}
}
