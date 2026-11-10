package local.villagers;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Comparator;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Explicit adoption outside the authored scenario, plus native campaign inspection. */
final class CreatureCommands {
    private CreatureCommands() {}
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("creatures")
                .then(Commands.literal("adopt").executes(c->apply(c.getSource(),"adopt")))
                .then(Commands.literal("release").executes(c->apply(c.getSource(),"release")))
                .then(Commands.literal("status").executes(c->apply(c.getSource(),"status"))))
            .then(Commands.literal("war").then(Commands.literal("status").executes(c->{
                var source=c.getSource();
                if(!WarCampaign.ready(source.getLevel())){source.sendFailure(Component.literal("No active war campaign in this world."));return 0;}
                String report=WarCampaign.snapshot(source.getLevel()).toString();source.sendSuccess(()->Component.literal(report),false);return 1;
            }))));
    }
    private static int apply(CommandSourceStack source,String action) {
        var pos=source.getPosition();
        Mob mob=source.getLevel().getEntitiesOfClass(Mob.class,new AABB(pos,pos).inflate(24),
            candidate->candidate.isAlive() && CreatureAgents.supported(candidate)).stream()
            .min(Comparator.comparingDouble(candidate->candidate.distanceToSqr(pos))).orElse(null);
        if(mob==null){source.sendFailure(Component.literal("No illager, Enderman or wandering trader within24blocks."));return 0;}
        if(action.equals("adopt"))CreatureAgents.adopt(source.getLevel(),mob);
        else if(!CreatureAgents.managed(mob)){source.sendFailure(Component.literal("That creature has not been adopted."));return 0;}
        else if(action.equals("release"))CreatureAgents.release(mob);
        String report=action.equals("release")?"Released creature to native AI.":CreatureAgents.status(mob).toString();
        source.sendSuccess(()->Component.literal(report),false);return 1;
    }
}
