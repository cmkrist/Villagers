package local.villagers.lab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LabCommandPolicyTest {
    @Test void playerLanguageCommandsAndExistingInspectionCommandsAreAllowed() {
        for (String command : new String[]{"glossary", "teach bread mora", "villagers status", "villagers language",
                "tick query", "tp @a -240 90 768 135 35"}) assertTrue(LabCommandPolicy.allowed(command), command);
    }

    @Test void unsafeRootsExtraArgumentsAndInvalidWordsRemainRejected() {
        for (String command : new String[]{"execute as @a run kill @e", "give @a wheat 64", "data get entity @e",
                "glossary extra", "teach bread a", "teach bread Upper", "teach bread toolongtoolong",
                "teach bread mora\nkill @e", "villagers status;stop", "villagers status\nstop"})
            assertFalse(LabCommandPolicy.allowed(command), command);
    }
}
