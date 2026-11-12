package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.villagers.language.*;

/** Predeclared syntax-only staged generations. Semantic categories and allowed omission are engine-authored. */
public final class CultureTrialSweep {
    private static final int GENERATIONS=3,POPULATION=5,EXPOSURES=8;
    private static final Clause FACT=new Clause(Concept.I,Concept.IS,Concept.HURT),NOVEL=new Clause(Concept.I,Concept.IS,Concept.WALK);
    private static Lexicon child(Lexicon parent,Random random){
        var c=Lexicon.child(parent,parent,random);
        // Explicit equal lexical priors isolate syntax transmission from word acquisition and birth mishearing.
        for(var concept:List.of(Concept.I,Concept.IS,Concept.HURT,Concept.WALK))c=c.reinforce(concept,parent.wordFor(concept),1);
        check(c.grammar().constructions().stream().noneMatch(p->p.evidence()>=Construction.MIN_EVIDENCE),"no adult construction mastery copied");return c;
    }
    private static boolean compact(Lexicon l){return Sentence.speak(List.of(NOVEL),l).sounds().size()==2;}
    private static boolean understands(Lexicon l,Lexicon speaker){var raw=Sentence.speak(List.of(NOVEL),speaker);return raw.correct(Sentence.interpret(raw.sounds(),l));}
    public static void main(String[] args)throws Exception{
        var rows=new ArrayList<Map<String,Object>>();var failures=new ArrayList<String>();
        for(long seed=0;seed<12;seed++)for(String condition:List.of("seeded-contact","supported-first-use","disconnected","frozen","full-contact")){
            var random=new Random(seed);var founding=BiomeDialect.foundingLanguage("minecraft:plains");boolean innovation=condition.equals("supported-first-use");
            var teacher=condition.equals("full-contact")||innovation?founding:founding.withGrammar(founding.grammar().learn(new Construction(List.of(Grammar.Role.SUBJECT,Grammar.Role.OBJECT),Concept.IS,9)));
            boolean initialSeed=teacher.grammar().constructions().stream().anyMatch(c->c.evidence()>=Construction.MIN_EVIDENCE);
            int firstUse=-1,observations=0;
            for(int generation=1;generation<=GENERATIONS;generation++){
                var cohort=new ArrayList<Lexicon>();int adopted=0,understood=0,unresolved=0,alreadyMastered=0,fixedCompact=0,fixedFull=0;
                for(int actor=0;actor<POPULATION;actor++){
                    var learner=child(teacher,random);if(compact(learner))alreadyMastered++;
                    for(int exposure=0;exposure<EXPOSURES;exposure++){
                        boolean proposed=innovation&&generation==1;
                        var message=Sentence.speak(List.of(FACT),teacher,proposed);
                        if(proposed){check(teacher.grammar().constructions().isEmpty()||firstUse>=0,"first supported innovation has no seeded construction");if(firstUse<0)firstUse=observations;
                            teacher=teacher.withGrammar(teacher.grammar().rehearse(message.sounds(),teacher::meaning,FACT));}
                        observations++;
                        if(!condition.equals("disconnected")&&!condition.equals("frozen"))learner=LanguageLearning.hear(learner,message.sounds(),Set.of(Concept.I,Concept.IS,Concept.HURT),FACT,random);
                    }
                    if(compact(learner))adopted++;
                    if(understands(learner,teacher))understood++;else if(!Sentence.interpret(Sentence.speak(List.of(NOVEL),teacher).sounds(),learner).complete())unresolved++;
                    var compactBenchmark=Sentence.speak(List.of(NOVEL),founding,true);var fullBenchmark=Sentence.speak(List.of(NOVEL),founding);
                    if(compactBenchmark.correct(Sentence.interpret(compactBenchmark.sounds(),learner)))fixedCompact++;
                    if(fullBenchmark.correct(Sentence.interpret(fullBenchmark.sounds(),learner)))fixedFull++;
                    cohort.add(learner);
                }
                check(alreadyMastered==0,"every cohort starts without adult productive mastery");
                int expectedAdoption=condition.equals("seeded-contact")||innovation?POPULATION:0;
                if(adopted!=expectedAdoption)failures.add(seed+"/"+condition+"/g"+generation+": adopted "+adopted+", expected "+expectedAdoption);
                if(!condition.equals("disconnected")&&!condition.equals("frozen")&&understood!=POPULATION)failures.add(seed+"/"+condition+"/g"+generation+": held-out meaning not retained");
                if(fixedCompact!=expectedAdoption||fixedFull!=POPULATION)failures.add(seed+"/"+condition+"/g"+generation+": fixed held-out benchmarks "+fixedCompact+" compact / "+fixedFull+" full");
                var row=new LinkedHashMap<String,Object>();row.put("seed",seed);row.put("condition",condition);row.put("generation",generation);row.put("population",POPULATION);row.put("adoptedCompact",adopted);row.put("heldOutMeaningsCorrect",understood);row.put("heldOutUnresolved",unresolved);row.put("fixedCompactMeaningCorrect",fixedCompact);row.put("fixedFullMeaningCorrect",fixedFull);row.put("initialProductiveMastery",alreadyMastered);row.put("founderSeededConstruction",initialSeed);row.put("firstSupportedUse",firstUse);row.put("exampleProduction",Sentence.speak(List.of(NOVEL),cohort.getFirst()));rows.add(row);
                teacher=cohort.getFirst();
            }
        }
        var report=new LinkedHashMap<String,Object>();report.put("status",failures.isEmpty()?"passed":"failed");report.put("conditions",List.of("seeded-contact","supported-first-use","disconnected","frozen","full-contact"));report.put("seeds",12);report.put("generations",GENERATIONS);report.put("populationPerCohort",POPULATION);report.put("exposuresPerLearner",EXPOSURES);report.put("cohorts",rows.size());report.put("learnerEpisodes",rows.size()*POPULATION);report.put("failures",failures);report.put("rows",rows);
        report.put("interpretation","Syntax-only symbolic experiment, no bodies, births, survival or audio fitness. Lexicon.child supplies weak inherited grammar; exact I/IS/HURT/WALK lexical priors are explicitly normalized to isolate syntax. Adaptive contact hears eight public injury examples per learner. Disconnected learners receive none; frozen learners receive no updates. Seeded compact source starts with authored SO/implicit-IS construction strength 9. Supported-first-use starts without constructions and deliberately exercises the engine's already-authored omission option, then rehearses the emitted form. This is actor-first use and diffusion of a supported pattern, not engine invention. Three staged cohorts per seed use the previous cohort's first learner as teacher; no selection for success. The held-out WALK meaning is never a lesson event. All seeds/conditions and failures are retained; no demographic or population-scale claim.");
        Files.writeString(Path.of(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(report));check(failures.isEmpty(),failures.toString());
    }
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
}
