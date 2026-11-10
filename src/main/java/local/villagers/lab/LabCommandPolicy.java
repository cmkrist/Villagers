package local.villagers.lab;

/** Narrow command grammar shared in behavior with tools/lab-command.py. No arbitrary execute/data/item commands. */
public final class LabCommandPolicy {
    private LabCommandPolicy() {}
    public static boolean allowed(String command) {
        return command != null && command.length() <= 512 && (command.matches("villagers [A-Za-z0-9_ -]+")
                || command.equals("glossary")
                || command.matches("teach [a-z_]{1,32} [a-z]{2,12}")
                || command.matches("time (set|add) (day|night|noon|midnight|[0-9]{1,8})")
                || command.matches("weather (clear|rain|thunder)( [0-9]{1,6}[stm]?)?")
                || command.matches("gamemode (creative|spectator) @a")
                || command.matches("tick (query|rate (20|40|80))")
                || command.matches("tp @a (-?[0-9]{1,6}(\\.[0-9]{1,4})? ){2,4}-?[0-9]{1,6}(\\.[0-9]{1,4})?"));
    }
}
