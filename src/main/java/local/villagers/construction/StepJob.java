package local.villagers.construction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Separate finite dirt acquisition and AIR-only construction authority; never inherited from soil jobs. */
public record StepJob(UUID id, UUID owner, UUID worker, List<Source> sources,
                      List<WorkSites.Point> blocks, int gathered, int placed) {
    public static final int MAX_BLOCKS=16;
    public record Source(WorkSites.Point point,String kind) {
        public Source {
            if(point==null || kind==null || !List.of("minecraft:dirt","minecraft:grass_block").contains(kind))
                throw new IllegalArgumentException("Need an exact native dirt source");
            vertical(point);
        }
    }
    private static void vertical(WorkSites.Point p) {
        if(p.y() < -2031 || p.y()>2029)throw new IllegalArgumentException("Step watch exceeds height bounds");
    }
    public StepJob {
        if(id==null || owner==null || worker==null || sources==null || blocks==null || blocks.isEmpty()
                || blocks.size()>MAX_BLOCKS || sources.size()!=blocks.size() || gathered<0 || gathered>sources.size()
                || placed<0 || placed>blocks.size() || placed>0 && gathered!=sources.size())
            throw new IllegalArgumentException("Invalid finite step work");
        sources=List.copyOf(sources);blocks=List.copyOf(blocks);
        if(new HashSet<>(blocks).size()!=blocks.size()
                || sources.stream().map(Source::point).distinct().count()!=sources.size())
            throw new IllegalArgumentException("Duplicate step resource or target");
        var sourceWatch=new HashSet<WorkSites.Point>();var targetWatch=new HashSet<WorkSites.Point>();
        var origin=sources.getFirst().point();var first=blocks.getFirst();
        for(var source:sources) {
            var p=source.point();
            if(!p.dimension().equals(origin.dimension()) || Math.abs(p.x()-origin.x())>8
                    || Math.abs(p.z()-origin.z())>8 || Math.abs(p.y()-origin.y())>8)
                throw new IllegalArgumentException("Source cluster exceeds bounds");
            for(int y=-1;y<=2;y++)sourceWatch.add(p.above(y));
        }
        for(var p:blocks) {
            vertical(p);
            if(!p.dimension().equals(origin.dimension()) || Math.abs(p.x()-first.x())>8 || Math.abs(p.z()-first.z())>8
                    || Math.abs(p.y()-first.y())>8 || Math.abs((long)p.x())>=30_000_000 || Math.abs((long)p.z())>=30_000_000)
                throw new IllegalArgumentException("Construction cluster exceeds bounds");
            for(var source:sources)if(Math.abs(p.x()-source.point().x())>96 || Math.abs(p.z()-source.point().z())>96
                    || Math.abs(p.y()-source.point().y())>16)throw new IllegalArgumentException("Step job spans too far");
            targetWatch.addAll(watch(p));
        }
        if(sourceWatch.stream().anyMatch(targetWatch::contains))throw new IllegalArgumentException("Source overlaps construction");
    }
    /** Support/attachment neighbours and headroom remain watched for the entire pending structure. */
    private static List<WorkSites.Point> watch(WorkSites.Point p) {
        var result=new ArrayList<WorkSites.Point>();
        for(int y=-1;y<=2;y++)result.add(p.above(y));
        for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})
            result.add(new WorkSites.Point(p.dimension(),p.x()+d[0],p.y(),p.z()+d[1]));
        return result;
    }
    public List<WorkSites.Point> footprint() {
        var result=new HashSet<WorkSites.Point>();
        for(int i=gathered;i<sources.size();i++)for(int y=-1;y<=2;y++)result.add(sources.get(i).point().above(y));
        for(var p:blocks)result.addAll(watch(p));
        return List.copyOf(result);
    }
    public StepJob nextGathered() {
        if(gathered==sources.size())throw new IllegalStateException("All step resources consumed");
        return new StepJob(id,owner,worker,sources,blocks,gathered+1,placed);
    }
    public StepJob nextPlaced() {
        if(gathered!=sources.size() || placed==blocks.size())throw new IllegalStateException("Unfunded or completed construction");
        return new StepJob(id,owner,worker,sources,blocks,gathered,placed+1);
    }
    public boolean follows(StepJob old) {
        return old!=null && id.equals(old.id) && owner.equals(old.owner) && worker.equals(old.worker)
                && sources.equals(old.sources) && blocks.equals(old.blocks)
                && (gathered==old.gathered+1 && placed==old.placed || gathered==old.gathered && placed==old.placed+1);
    }
    public boolean complete() { return placed==blocks.size(); }
}
