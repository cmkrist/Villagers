package local.villagers.language;

import java.util.List;
import local.villagers.mind.Episode;

/**
 * Generic concepts for describing an experience. These do not name its subject or identify the episode;
 * hearing them must not unlock the hidden event record.
 */
public final class Telling {
    public static final double UNDERSTANDING = 0.5;

    private Telling() {}

    public static List<Concept> concepts(Episode episode) {
        return switch (episode.kind()) {
            case CLAIMED_BED -> List.of(Concept.BED, Concept.HAPPY);
            case RECEIVED_ITEM -> {
                Concept item = Concept.forItem(episode.detail());
                yield item == null ? List.of(Concept.THANKS, Concept.HAPPY) : List.of(item, Concept.THANKS);
            }
            case HURT_BY_PLAYER -> List.of(Concept.HURT, Concept.AFRAID);
            case PROTECTED_BY_PLAYER -> List.of(Concept.MONSTER, Concept.FRIEND);
            case BORN, HAD_CHILD -> List.of(Concept.FRIEND, Concept.BED, Concept.HAPPY);
            case GOD_CHILD -> List.of(Concept.HELLO, Concept.LONELY);
            case OBSERVED_FARM -> List.of(Concept.FOOD);
        };
    }
}
