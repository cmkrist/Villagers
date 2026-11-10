package local.villagers.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActivityLabelTest {
    @Test void writesOnlyOverAbsentOrOwnLabels() {
        assertTrue(ActivityLabel.ownedBy("Fern-ab12", null));
        assertTrue(ActivityLabel.ownedBy("Fern-ab12", ActivityLabel.text("Fern-ab12", "rest")));
        assertFalse(ActivityLabel.ownedBy("Fern-ab12", "Steve's farmer"), "player name tags are never overwritten");
        assertFalse(ActivityLabel.ownedBy("Fern-ab12", ActivityLabel.text("Moss-cd34", "rest")));
    }

    @Test void releaseClearsOnlyOwnLabels() {
        assertFalse(ActivityLabel.isLabel("Fern-ab12", null));
        assertFalse(ActivityLabel.isLabel("Fern-ab12", "Steve's farmer"));
        assertTrue(ActivityLabel.isLabel("Fern-ab12", "Fern-ab12 · forage"));
    }
}
