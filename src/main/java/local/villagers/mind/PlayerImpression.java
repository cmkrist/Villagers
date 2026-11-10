package local.villagers.mind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An individual's fallible impression of one player, separate from utility learning or obedience. */
public record PlayerImpression(UUID playerId, double awe, double trust, double fear, double confidence,
                               long lastObservedTick, long lastExposureTick, long lastGiftTick,
                               long lastProtectionTick, long evidenceCount, List<Note> notes) {
    public static final int MAX_NOTES = 8;
    public static final long EXPOSURE_COOLDOWN = 200;
    public static final long GIFT_COOLDOWN = 600;
    public static final long PROTECTION_COOLDOWN = 200;

    public enum Evidence { EXPOSURE, USEFUL_GIFT, PLAYER_HURT, WITNESSED_PROTECTION }
    public enum Reaction { ATTEND, APPROACH, AVOID, NEUTRAL }
    public record Note(long tick, Evidence evidence) {
        public Note {
            if (tick < 0) throw new IllegalArgumentException("Evidence tick cannot be negative");
            Objects.requireNonNull(evidence);
        }
    }

    public PlayerImpression {
        Objects.requireNonNull(playerId);
        finiteRange(awe, 0, 1);
        finiteRange(trust, -1, 1);
        finiteRange(fear, 0, 1);
        finiteRange(confidence, 0, 1);
        if (lastObservedTick < -1 || lastExposureTick < -1 || lastGiftTick < -1 || lastProtectionTick < -1
                || lastExposureTick > lastObservedTick || lastGiftTick > lastObservedTick
                || lastProtectionTick > lastObservedTick || evidenceCount < 0)
            throw new IllegalArgumentException("Invalid impression chronology");
        notes = List.copyOf(Objects.requireNonNull(notes));
        if (notes.size() > MAX_NOTES || notes.size() > evidenceCount
                || (evidenceCount == 0 && lastObservedTick != -1)
                || (evidenceCount > 0 && lastObservedTick < 0))
            throw new IllegalArgumentException("Invalid impression history");
        long previous = -1;
        for (Note note : notes) {
            if (note.tick() < previous || note.tick() > lastObservedTick)
                throw new IllegalArgumentException("Non-monotonic impression notes");
            previous = note.tick();
        }
    }

    private static void finiteRange(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException("Impression values must be finite and bounded");
    }

    /** High regard for unfamiliar players does not imply strong trust or a confident belief. */
    public static PlayerImpression prior(UUID playerId) {
        return new PlayerImpression(playerId, .85, .10, .15, .05, -1, -1, -1, -1, 0, List.of());
    }

    public double regard() { return awe; }

    /** null means rejected: backdated evidence or a non-harm event still on cooldown. */
    PlayerImpression observe(Evidence evidence, long gameTick) {
        Objects.requireNonNull(evidence);
        if (gameTick < 0 || gameTick < lastObservedTick) return null;
        long last = switch (evidence) {
            case EXPOSURE -> lastExposureTick;
            case USEFUL_GIFT -> lastGiftTick;
            case WITNESSED_PROTECTION -> lastProtectionTick;
            case PLAYER_HURT -> -1;
        };
        long cooldown = switch (evidence) {
            case EXPOSURE -> EXPOSURE_COOLDOWN;
            case USEFUL_GIFT -> GIFT_COOLDOWN;
            case WITNESSED_PROTECTION -> PROTECTION_COOLDOWN;
            case PLAYER_HURT -> 0;
        };
        if (last >= 0 && gameTick - last < cooldown) return null;
        double nextAwe = awe, nextTrust = trust, nextFear = fear, nextConfidence = confidence;
        switch (evidence) {
            case EXPOSURE -> {
                // Familiarity alone is weak evidence. Standing nearby cannot manufacture high trust.
                nextTrust = Math.max(trust, Math.min(.25, trust + .005));
                nextFear = Math.max(0, fear - .005);
                nextConfidence += .015;
            }
            case USEFUL_GIFT -> {
                nextAwe += .03;
                nextTrust += .18;
                nextFear -= .10;
                nextConfidence += .13;
            }
            case WITNESSED_PROTECTION -> {
                nextAwe += .035;
                nextTrust += .22;
                nextFear -= .10;
                nextConfidence += .18;
            }
            case PLAYER_HURT -> {
                nextAwe -= .18;
                nextTrust -= .65;
                nextFear += .65;
                nextConfidence += .35;
            }
        }
        ArrayList<Note> history = new ArrayList<>(notes);
        if (history.size() == MAX_NOTES) history.removeFirst();
        history.add(new Note(gameTick, evidence));
        return new PlayerImpression(playerId, Math.clamp(nextAwe, 0, 1), Math.clamp(nextTrust, -1, 1),
                Math.clamp(nextFear, 0, 1), Math.clamp(nextConfidence, 0, 1), gameTick,
                evidence == Evidence.EXPOSURE ? gameTick : lastExposureTick,
                evidence == Evidence.USEFUL_GIFT ? gameTick : lastGiftTick,
                evidence == Evidence.WITNESSED_PROTECTION ? gameTick : lastProtectionTick,
                evidenceCount == Long.MAX_VALUE ? evidenceCount : evidenceCount + 1, history);
    }

    /**
     * Being told what a trusted neighbour thinks of this player. Trust and fear move toward the teller's by
     * {@code weight}, and confidence grows a little, up to 0.5: a reputation, but weaker than first-hand evidence.
     * Cooldowns and evidence counts are untouched.
     */
    public PlayerImpression hearsay(PlayerImpression told, double weight) {
        Objects.requireNonNull(told);
        if (!(weight > 0)) return this;
        double w = Math.min(weight, 1);
        return new PlayerImpression(playerId, awe, Math.clamp(trust + w * (told.trust - trust), -1, 1),
                Math.clamp(fear + w * (told.fear - fear), 0, 1), Math.max(confidence, Math.min(0.5, confidence + 0.1 * w)),
                lastObservedTick, lastExposureTick, lastGiftTick, lastProtectionTick, evidenceCount, notes);
    }

    /** An offered reaction; the controller's danger, household, sleep and trade priorities still take precedence. */
    public Reaction reaction(Observation observation) {
        Objects.requireNonNull(observation);
        if (observation.danger() || observation.night()) return Reaction.NEUTRAL;
        if (confidence >= .25 && (fear >= .55 || trust <= -.35)) return Reaction.AVOID;
        if (observation.hunger() >= .85 || observation.fatigue() >= .85 || observation.health() <= .3)
            return Reaction.NEUTRAL;
        if (confidence >= .25 && trust >= .35 && fear < .4) return Reaction.APPROACH;
        if (awe >= .6 && fear < .55) return Reaction.ATTEND;
        return Reaction.NEUTRAL;
    }
}
