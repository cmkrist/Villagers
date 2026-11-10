package local.villagers.construction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Explicit finite ground permissions. Unknown land is denied; protection always wins. */
public record WorkSites(int schemaVersion, List<Farm> farms, List<Protection> protections, List<Station> stations, List<Forestry> forestry, List<Soil> soilJobs, List<StepJob> stepJobs) {
    public static final int SCHEMA = 7, MAX_FARMS = 8, MAX_CELLS = 81, MAX_PROTECTIONS = 32, MAX_STATIONS = 8, MAX_FORESTRY = 8, MAX_SOIL = 4, MAX_STEPS = 2;
    private static final java.util.regex.Pattern DIMENSION = java.util.regex.Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");
    public record Point(String dimension, int x, int y, int z) {
        public Point {
            if (dimension == null || dimension.length() > 128 || !DIMENSION.matcher(dimension).matches()
                    || Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000 || y < -2032 || y > 2032)
                throw new IllegalArgumentException("Invalid site position");
        }
        public Point above(int n) { return new Point(dimension, x, y + n, z); }
    }
    public record Farm(UUID id, UUID owner, List<Point> cells) {
        public Farm {
            if (id == null || owner == null || cells == null || cells.isEmpty() || cells.size() > MAX_CELLS)
                throw new IllegalArgumentException("Invalid farm grant");
            cells = List.copyOf(cells);
            if (new HashSet<>(cells).size() != cells.size()) throw new IllegalArgumentException("Duplicate cells");
            Point first = cells.getFirst();
            int minX = cells.stream().mapToInt(Point::x).min().orElseThrow(), maxX = cells.stream().mapToInt(Point::x).max().orElseThrow();
            int minZ = cells.stream().mapToInt(Point::z).min().orElseThrow(), maxZ = cells.stream().mapToInt(Point::z).max().orElseThrow();
            if (maxX - minX > 8 || maxZ - minZ > 8 || cells.stream().anyMatch(p -> !p.dimension().equals(first.dimension())
                    || p.y() != first.y() || p.y() > 2029)) throw new IllegalArgumentException("Farm exceeds bounds");
        }
    }
    public record Protection(UUID id, UUID owner, Point min, Point max) {
        public Protection {
            if (id == null || owner == null || min == null || max == null || !min.dimension().equals(max.dimension()))
                throw new IllegalArgumentException("Invalid protection");
            long x = (long) max.x() - min.x() + 1, y = (long) max.y() - min.y() + 1, z = (long) max.z() - min.z() + 1;
            if (x <= 0 || y <= 0 || z <= 0 || x > 64 || y > 64 || z > 64 || x * y * z > 4096)
                throw new IllegalArgumentException("Protection exceeds bounds");
        }
        public boolean contains(Point p) { return min.dimension().equals(p.dimension()) && p.x() >= min.x() && p.x() <= max.x()
                && p.y() >= min.y() && p.y() <= max.y() && p.z() >= min.z() && p.z() <= max.z(); }
    }
    public record Station(UUID id, UUID owner, Point target) {
        public Station {
            if (id == null || owner == null || target == null || target.y() < -2031 || target.y() > 2031
                    || Math.abs((long) target.x()) >= 30_000_000 || Math.abs((long) target.z()) >= 30_000_000)
                throw new IllegalArgumentException("Invalid station grant");
        }
        /** Only the center may receive a table. Support, headroom and four access cells are watched for edits. */
        public List<Point> footprint() {
            var points = new ArrayList<Point>(); points.add(target.above(-1)); points.add(target); points.add(target.above(1));
            for (int[] d : new int[][] {{-1,0},{1,0},{0,-1},{0,1}}) for (int y = 0; y <= 1; y++)
                points.add(new Point(target.dimension(), target.x() + d[0], target.y() + y, target.z() + d[1]));
            return List.copyOf(points);
        }
    }
    /** One approved native soil source, one sand replacement, then the same finite till/plant footprint. */
    public record Soil(UUID id, UUID owner, Point source, Point target, String sourceKind, boolean gathered) {
        public Soil {
            if (id==null || owner==null || source==null || target==null || !source.dimension().equals(target.dimension())
                    || source.equals(target) || source.y() < -2031 || target.y() < -2031 || source.y()>2029 || target.y()>2029
                    || Math.abs(source.x()-target.x())>96 || Math.abs(source.z()-target.z())>96 || Math.abs(source.y()-target.y())>16
                    || sourceKind==null || !List.of("minecraft:dirt","minecraft:grass_block").contains(sourceKind))
                throw new IllegalArgumentException("Invalid finite soil transfer");
            var cells=new HashSet<Point>();
            for (Point p:List.of(source,target)) for(int y=-1;y<=2;y++)
                if(!cells.add(p.above(y))) throw new IllegalArgumentException("Overlapping soil roles");
        }
        public List<Point> footprint() {
            var cells=new ArrayList<Point>();
            if(!gathered) for(int y=-1;y<=2;y++)cells.add(source.above(y));
            for(int y=-1;y<=2;y++)cells.add(target.above(y));
            return List.copyOf(cells);
        }
        public Soil markGathered() { return new Soil(id,owner,source,target,sourceKind,true); }
    }
    public WorkSites(int schemaVersion,List<Farm> farms,List<Protection> protections,List<Station> stations,List<Forestry> forestry) {
        this(schemaVersion,farms,protections,stations,forestry,List.of());
    }
    public WorkSites(int schemaVersion,List<Farm> farms,List<Protection> protections,List<Station> stations,List<Forestry> forestry,List<Soil> soilJobs) {
        this(schemaVersion,farms,protections,stations,forestry,soilJobs,List.of());
    }
    public WorkSites(int schemaVersion, List<Farm> farms, List<Protection> protections) {
        this(schemaVersion, farms, protections, List.of(), List.of());
    }
    public WorkSites(int schemaVersion, List<Farm> farms, List<Protection> protections, List<Station> stations) {
        this(schemaVersion, farms, protections, stations, List.of());
    }
    public record Forestry(UUID id, UUID owner, Point seed, List<Point> logs, Point plant, String soil, boolean seedTaken, List<Point> remaining, List<Point> clearance, List<Point> uncleared, List<Point> roots, List<Point> unrooted) {
        public Forestry(UUID id,UUID owner,Point seed,List<Point> logs,Point plant,String soil,boolean seedTaken,List<Point> remaining) {
            this(id,owner,seed,logs,plant,soil,seedTaken,remaining,List.of(),List.of());
        }
        public Forestry(UUID id,UUID owner,Point seed,List<Point> logs,Point plant,String soil,boolean seedTaken,List<Point> remaining,List<Point> clearance,List<Point> uncleared) {
            this(id,owner,seed,logs,plant,soil,seedTaken,remaining,clearance,uncleared,List.of(),List.of());
        }
        public Forestry {
            if (clearance==null && uncleared==null) { clearance=List.of();uncleared=List.of(); }
            if (roots==null && unrooted==null) { roots=List.of();unrooted=List.of(); }
            if (id == null || owner == null || seed == null || plant == null || soil == null || soil.length() > 128
                    || !DIMENSION.matcher(soil).matches() || logs == null || logs.isEmpty() || logs.size() > 4 || remaining == null)
                throw new IllegalArgumentException("Invalid forestry grant");
            if (clearance==null || uncleared==null || clearance.size()>4 || new HashSet<>(clearance).size()!=clearance.size()
                    || new HashSet<>(uncleared).size()!=uncleared.size() || !clearance.containsAll(uncleared)
                    || !uncleared.isEmpty() && !remaining.equals(logs) || !seedTaken && !uncleared.equals(clearance))
                throw new IllegalArgumentException("Invalid finite clearance progress");
            if (roots==null || unrooted==null || roots.size()>1 || new HashSet<>(roots).size()!=roots.size()
                    || new HashSet<>(unrooted).size()!=unrooted.size() || !roots.containsAll(unrooted)
                    || !unrooted.isEmpty() && !remaining.equals(logs) || !seedTaken && !unrooted.equals(roots))
                throw new IllegalArgumentException("Invalid explicit single-root progress");
            logs = List.copyOf(logs); remaining = List.copyOf(remaining);clearance=List.copyOf(clearance);uncleared=List.copyOf(uncleared);
            roots=List.copyOf(roots);unrooted=List.copyOf(unrooted);
            var positions = new HashSet<Point>(); positions.add(seed); positions.add(plant);
            if (positions.size() != 2 || new HashSet<>(logs).size() != logs.size() || new HashSet<>(remaining).size() != remaining.size()
                    || !logs.containsAll(remaining) || !seedTaken && !remaining.equals(logs)) throw new IllegalArgumentException("Invalid forestry progress");
            for (Point p : logs) if (!positions.add(p)) throw new IllegalArgumentException("Overlapping forestry roles");
            for (Point p : clearance) if (!positions.add(p)) throw new IllegalArgumentException("Overlapping clearance roles");
            for (Point p : roots) if (!positions.add(p)) throw new IllegalArgumentException("Overlapping root roles");
            for (Point p : positions) if (!p.dimension().equals(seed.dimension()) || Math.abs(p.x()-seed.x()) > 16
                    || Math.abs(p.z()-seed.z()) > 16 || Math.abs(p.y()-seed.y()) > 12 || p.y() < -2031 || p.y() > 2030)
                throw new IllegalArgumentException("Forestry job exceeds local bounds");
        }
        public List<Point> footprint() {
            var points = new ArrayList<Point>(remaining);
            points.addAll(uncleared);
            points.addAll(unrooted);
            if (!seedTaken) { points.add(seed); points.add(seed.above(1)); }
            points.add(plant.above(-1)); points.add(plant); points.add(plant.above(1));
            return points.stream().distinct().toList();
        }
        public Forestry gatheredSeed() { return new Forestry(id,owner,seed,logs,plant,soil,true,remaining,clearance,uncleared,roots,unrooted); }
        public Forestry cleared(Point p) {
            if (!seedTaken || !uncleared.contains(p)) throw new IllegalArgumentException("Unauthorized clearance progress");
            return new Forestry(id,owner,seed,logs,plant,soil,true,remaining,clearance,uncleared.stream().filter(q -> !q.equals(p)).toList(),roots,unrooted);
        }
        public Forestry clearedRoot(Point p) {
            if (!seedTaken || !uncleared.isEmpty() || !unrooted.contains(p)) throw new IllegalArgumentException("Unauthorized root progress");
            return new Forestry(id,owner,seed,logs,plant,soil,true,remaining,clearance,uncleared,roots,unrooted.stream().filter(q -> !q.equals(p)).toList());
        }
        public Forestry cut(Point p) {
            if (!seedTaken || !uncleared.isEmpty() || !unrooted.isEmpty() || !remaining.contains(p)) throw new IllegalArgumentException("Unauthorized log progress");
            return new Forestry(id,owner,seed,logs,plant,soil,true,remaining.stream().filter(q -> !q.equals(p)).toList(),clearance,uncleared,roots,unrooted);
        }
    }
    public WorkSites {
        if(schemaVersion>=1 && schemaVersion<=6) {
            if(stepJobs!=null && !stepJobs.isEmpty())throw new IllegalArgumentException("Legacy schema cannot grant step construction");
            stepJobs=List.of();
        }
        if(schemaVersion>=1 && schemaVersion<=5) {
            if(soilJobs!=null && !soilJobs.isEmpty()) throw new IllegalArgumentException("Legacy schema cannot grant soil work");
            soilJobs=List.of();
        }
        if (schemaVersion == 1 && stations == null && forestry == null) { schemaVersion = SCHEMA; stations = List.of(); forestry = List.of(); }
        if (schemaVersion == 2 && forestry == null) { schemaVersion = SCHEMA; forestry = List.of(); }
        if (schemaVersion == 3 || schemaVersion == 4 || schemaVersion == 5 || schemaVersion == 6) schemaVersion=SCHEMA;
        if (schemaVersion != SCHEMA || farms == null || protections == null || farms.size() > MAX_FARMS
                || protections.size() > MAX_PROTECTIONS || stations == null || stations.size() > MAX_STATIONS || forestry == null || forestry.size() > MAX_FORESTRY || soilJobs==null || soilJobs.size()>MAX_SOIL || farms.size()+soilJobs.size()>MAX_FARMS
                || stepJobs==null || stepJobs.size()>MAX_STEPS)
            throw new IllegalArgumentException("Unsupported or invalid sites");
        farms = List.copyOf(farms); protections = List.copyOf(protections); stations = List.copyOf(stations); forestry = List.copyOf(forestry); soilJobs=List.copyOf(soilJobs);
        stepJobs=List.copyOf(stepJobs);
        var ids = new HashSet<UUID>(); var cells = new HashSet<Point>();
        for (Farm f : farms) {
            if (!ids.add(f.id())) throw new IllegalArgumentException("Duplicate site identity");
            for (Point p : f.cells()) for (int y = 0; y <= 2; y++)
                if (!cells.add(p.above(y))) throw new IllegalArgumentException("Overlapping farm footprints");
        }
        for (Protection p : protections) if (!ids.add(p.id())) throw new IllegalArgumentException("Duplicate site identity");
        for (Station s : stations) {
            if (!ids.add(s.id())) throw new IllegalArgumentException("Duplicate site identity");
            for (Point p : s.footprint()) if (!cells.add(p)) throw new IllegalArgumentException("Overlapping work-site footprints");
        }
        for(Soil job:soilJobs) {
            if(!ids.add(job.id())) throw new IllegalArgumentException("Duplicate soil identity");
            for(Point p:job.footprint()) if(!cells.add(p)) throw new IllegalArgumentException("Overlapping soil footprint");
        }
        for(var job:stepJobs) {
            if(!ids.add(job.id()))throw new IllegalArgumentException("Duplicate step identity");
            for(var p:job.footprint())if(!cells.add(p))throw new IllegalArgumentException("Overlapping step footprint");
        }
        for (Forestry f : forestry) {
            if (!ids.add(f.id())) throw new IllegalArgumentException("Duplicate site identity");
            for (Point p : f.footprint()) if (!cells.add(p)) throw new IllegalArgumentException("Overlapping resource footprints");
        }
    }
    public boolean allowsSoil(UUID id) {
        return soilJobs.stream().filter(j -> j.id().equals(id)).anyMatch(j -> j.footprint().stream()
                .noneMatch(p -> protections.stream().anyMatch(zone -> zone.contains(p))));
    }
    public boolean allowsSteps(UUID id) {
        return stepJobs.stream().filter(j -> j.id().equals(id)).anyMatch(j -> j.footprint().stream()
                .noneMatch(p -> protections.stream().anyMatch(zone -> zone.contains(p))));
    }
    public WorkSites add(StepJob job) {
        var next=new ArrayList<>(stepJobs);next.add(job);
        return new WorkSites(SCHEMA,farms,protections,stations,forestry,soilJobs,next);
    }
    public WorkSites progress(StepJob next) {
        var old=stepJobs.stream().filter(j -> j.id().equals(next.id())).findFirst().orElseThrow();
        if(!next.follows(old))throw new IllegalArgumentException("Step authority cannot grow, skip or replay progress");
        return new WorkSites(SCHEMA,farms,protections,stations,forestry,soilJobs,
                stepJobs.stream().map(j -> j.id().equals(next.id())?next:j).filter(j -> !j.complete()).toList());
    }
    public WorkSites add(Soil job) { var next=new ArrayList<>(soilJobs);next.add(job);return new WorkSites(SCHEMA,farms,protections,stations,forestry,next, stepJobs); }
    public WorkSites progress(Soil next) {
        var old=soilJobs.stream().filter(j -> j.id().equals(next.id())).findFirst().orElseThrow();
        if(!old.owner().equals(next.owner()) || !old.source().equals(next.source()) || !old.target().equals(next.target())
                || !old.sourceKind().equals(next.sourceKind()) || old.gathered() && !next.gathered())
            throw new IllegalArgumentException("Soil authority cannot grow during progress");
        return new WorkSites(SCHEMA,farms,protections,stations,forestry,soilJobs.stream().map(j -> j.id().equals(next.id())?next:j).toList(), stepJobs);
    }
    public WorkSites soilReadyForPlanting(UUID id) {
        var job=soilJobs.stream().filter(j -> j.id().equals(id)).findFirst().orElseThrow();
        if(!job.gathered()) throw new IllegalArgumentException("Soil has not been gathered");
        return remove(id).add(new Farm(UUID.randomUUID(),job.owner(),List.of(job.target())));
    }
    public static WorkSites empty() { return new WorkSites(SCHEMA, List.of(), List.of()); }
    public boolean allows(Point soil) {
        return farms.stream().anyMatch(f -> f.cells().contains(soil)) && !protectedFootprint(soil);
    }
    public boolean protectedFootprint(Point soil) {
        for (Protection p : protections) if (p.min().dimension().equals(soil.dimension())
                && soil.x() >= p.min().x() && soil.x() <= p.max().x()
                && soil.z() >= p.min().z() && soil.z() <= p.max().z()
                && soil.y() <= p.max().y() && soil.y() + 2 >= p.min().y()) return true;
        return false;
    }
    public boolean allowsStation(Point target) {
        return stations.stream().anyMatch(s -> s.target().equals(target)
                && s.footprint().stream().noneMatch(p -> protections.stream().anyMatch(zone -> zone.contains(p))));
    }
    public boolean allowsForestry(UUID id) {
        return forestry.stream().filter(f -> f.id().equals(id)).anyMatch(f -> f.footprint().stream()
                .noneMatch(p -> protections.stream().anyMatch(zone -> zone.contains(p))));
    }
    public WorkSites add(Farm f) { var next = new ArrayList<>(farms); next.add(f); return new WorkSites(SCHEMA, next, protections, stations, forestry, soilJobs, stepJobs); }
    public WorkSites add(Protection p) { var next = new ArrayList<>(protections); next.add(p); return new WorkSites(SCHEMA, farms, next, stations, forestry, soilJobs, stepJobs); }
    public WorkSites add(Station s) { var next = new ArrayList<>(stations); next.add(s); return new WorkSites(SCHEMA, farms, protections, next, forestry, soilJobs, stepJobs); }
    public WorkSites add(Forestry f) { var next = new ArrayList<>(forestry); next.add(f); return new WorkSites(SCHEMA, farms, protections, stations, next, soilJobs, stepJobs); }
    public WorkSites progress(Forestry next) {
        var old = forestry.stream().filter(f -> f.id().equals(next.id())).findFirst().orElseThrow();
        if (!old.owner().equals(next.owner()) || !old.seed().equals(next.seed()) || !old.logs().equals(next.logs())
                || !old.plant().equals(next.plant()) || !old.soil().equals(next.soil()) || old.seedTaken() && !next.seedTaken()
                || !old.remaining().containsAll(next.remaining()) || !old.clearance().equals(next.clearance())
                || !old.uncleared().containsAll(next.uncleared()) || !old.roots().equals(next.roots())
                || !old.unrooted().containsAll(next.unrooted())) throw new IllegalArgumentException("Forestry authority cannot grow during progress");
        return new WorkSites(SCHEMA,farms,protections,stations,forestry.stream().map(f -> f.id().equals(next.id()) ? next : f).toList(), soilJobs, stepJobs);
    }
    /** Explicit fresh operator authority, not ordinary progress: preserve paid seed/planting and never increase outstanding quota. */
    public WorkSites moveLogs(UUID id,Forestry next) {
        var old=forestry.stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
        if (id.equals(next.id()) || !old.seedTaken() || !next.seedTaken() || old.remaining().isEmpty()
                || next.logs().size()>old.remaining().size() || !next.remaining().equals(next.logs())
                || !old.owner().equals(next.owner()) || !old.seed().equals(next.seed()) || !old.plant().equals(next.plant()) || !old.soil().equals(next.soil())
                || !old.clearance().equals(next.clearance()) || !old.uncleared().equals(next.uncleared())
                || !old.roots().equals(next.roots()) || !old.unrooted().equals(next.unrooted()))
            throw new IllegalArgumentException("Explicit log reassignment must retain seed/planting and cannot grow quota");
        return remove(id).add(next);
    }
    public WorkSites clearance(UUID id,List<Point> leaves) {
        var old=forestry.stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
        if (leaves.isEmpty() || leaves.stream().anyMatch(old.clearance()::contains)) throw new IllegalArgumentException("Need fresh explicit leaf cells");
        var all=new ArrayList<>(old.clearance());all.addAll(leaves);var pending=new ArrayList<>(old.uncleared());pending.addAll(leaves);
        var next=new Forestry(UUID.randomUUID(),old.owner(),old.seed(),old.logs(),old.plant(),old.soil(),old.seedTaken(),old.remaining(),all,pending,old.roots(),old.unrooted());
        return remove(id).add(next);
    }
    /** Fresh operator permission for one exact dry mangrove root; never inherited from leaf grants. */
    public WorkSites root(UUID id,Point root) {
        var old=forestry.stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
        if (!old.roots().isEmpty()) throw new IllegalArgumentException("Single-root quota already assigned");
        var next=new Forestry(UUID.randomUUID(),old.owner(),old.seed(),old.logs(),old.plant(),old.soil(),old.seedTaken(),old.remaining(),old.clearance(),old.uncleared(),List.of(root),List.of(root));
        return remove(id).add(next);
    }
    public WorkSites remove(UUID id) { return new WorkSites(SCHEMA, farms.stream().filter(f -> !f.id().equals(id)).toList(),
            protections.stream().filter(p -> !p.id().equals(id)).toList(), stations.stream().filter(s -> !s.id().equals(id)).toList(), forestry.stream().filter(f -> !f.id().equals(id)).toList(), soilJobs.stream().filter(j -> !j.id().equals(id)).toList(), stepJobs.stream().filter(j -> !j.id().equals(id)).toList()); }
    /** An outside block edit or a completed plot retires this finite permission, including its headroom. */
    public WorkSites touch(Point block) {
        var next = new ArrayList<Farm>();
        for (Farm f : farms) {
            var cells = f.cells().stream().filter(p -> !p.equals(block) && !p.above(1).equals(block) && !p.above(2).equals(block)).toList();
            if (!cells.isEmpty()) next.add(new Farm(f.id(), f.owner(), cells));
        }
        return new WorkSites(SCHEMA, next, protections, stations.stream().filter(s -> !s.footprint().contains(block)).toList(),forestry.stream().filter(f -> !f.footprint().contains(block)).toList(), soilJobs.stream().filter(j -> !j.footprint().contains(block)).toList(), stepJobs.stream().filter(j -> !j.footprint().contains(block)).toList());
    }
    public WorkSites revokeEdits() { return new WorkSites(SCHEMA, List.of(), protections, List.of(), List.of(), List.of(), List.of()); }
}
