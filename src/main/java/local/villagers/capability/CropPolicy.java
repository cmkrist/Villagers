package local.villagers.capability;

/** Narrow farming exception; neither immature crops nor general terrain are eligible. */
public final class CropPolicy {
    public enum PlantMaterial { WHEAT_SEED, CARROT, POTATO, BEETROOT_SEED, NONE }
    public enum CropKind {
        WHEAT(PlantMaterial.WHEAT_SEED), CARROT(PlantMaterial.CARROT), POTATO(PlantMaterial.POTATO),
        BEETROOT(PlantMaterial.BEETROOT_SEED), OTHER(PlantMaterial.NONE);
        private final PlantMaterial plantingMaterial;
        CropKind(PlantMaterial plantingMaterial) { this.plantingMaterial = plantingMaterial; }
        public PlantMaterial plantingMaterial() { return plantingMaterial; }
    }
    private CropPolicy() {}
    /** A ripe native crop may be inspected; actual matching planting payment is still mandatory at commit. */
    public static boolean mayInspectHarvest(CropKind crop,int age,int maxAge,boolean permittedHarvester,boolean intactFarmland,boolean withinReach) {
        return crop!=null && crop!=CropKind.OTHER && maxAge>0 && age==maxAge && permittedHarvester && intactFarmland && withinReach;
    }
    public static boolean mayHarvest(CropKind crop, int age, int maxAge, boolean permittedHarvester,
                                     PlantMaterial availableMaterial, boolean intactFarmland, boolean withinReach) {
        return mayInspectHarvest(crop,age,maxAge,permittedHarvester,intactFarmland,withinReach)
            && availableMaterial == crop.plantingMaterial();
    }
}
