package local.villagers.language;

import java.util.List;
import java.util.random.RandomGenerator;

/** What villagers say in common situations. Short, drive-led phrases, in the spirit of Creatures. */
public final class Phrases {
    private Phrases() {}

    public enum Situation { FORAGE, REST, SOCIALIZE, EXPLORE, DANGER, GREETING, THANKS, REFUSE, AGREE, HURT }

    public static List<Concept> concepts(Situation situation, RandomGenerator random) {
        return switch (situation) {
            case FORAGE -> random.nextBoolean() ? List.of(Concept.HUNGRY) : List.of(Concept.WANT, Concept.FOOD);
            case REST -> random.nextBoolean() ? List.of(Concept.TIRED) : List.of(Concept.WANT, Concept.BED);
            case SOCIALIZE -> random.nextBoolean() ? List.of(Concept.LONELY) : List.of(Concept.WANT, Concept.FRIEND);
            case EXPLORE -> List.of(Concept.WALK);
            case DANGER -> random.nextBoolean() ? List.of(Concept.DANGER) : List.of(Concept.DANGER, Concept.MONSTER);
            case GREETING -> List.of(Concept.HELLO);
            case THANKS -> List.of(Concept.THANKS);
            case REFUSE -> List.of(Concept.NO);
            case AGREE -> List.of(Concept.YES);
            case HURT -> List.of(Concept.HURT, Concept.AFRAID);
        };
    }
}
