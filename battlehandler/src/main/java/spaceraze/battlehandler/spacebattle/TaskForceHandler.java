package spaceraze.battlehandler.spacebattle;

import spaceraze.servlethelper.game.VipPureFunctions;
import spaceraze.servlethelper.game.planet.PlanetPureFunctions;
import spaceraze.servlethelper.game.spaceship.SpaceshipPureFunctions;
import spaceraze.util.move.FindPlanetCriterion;
import spaceraze.world.Galaxy;
import spaceraze.world.Planet;
import spaceraze.world.Player;
import spaceraze.world.enums.SpaceshipRange;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TaskForceHandler {

    private TaskForceHandler(){}

    public static List<TaskForce> getTaskForces(Planet aPlanet, boolean includeCivilians, Galaxy galaxy) {
        List<TaskForce> taskforces = new LinkedList<TaskForce>();
        // get all neutral ships at aPlanet
        TaskForce neutraltf = getTaskForce(null, aPlanet, includeCivilians, galaxy);
        if (neutraltf != null) {
            taskforces.add(neutraltf);
        }
        // get all player taskforces at this planet
        for (Player player : galaxy.getPlayers()) {
            // LoggingHandler.fine(this,this,"getTaskforces","Player loop: " +
            // tempplayer.getName());
            TaskForce temptf = getTaskForce(player, aPlanet, includeCivilians, galaxy);
            if (temptf != null) {
                // LoggingHandler.finer(this,this,"getTaskforces","TaskForce added: " +
                // temptf.getTotalNrShips(true));
                taskforces.add(temptf);
            }
        }
        return taskforces;
    }

    public static TaskForce getTaskForce(Player aPlayer, Planet aPlanet, boolean includeCivilians, Galaxy galaxy) {
        // TODO 2019-12-07 Säkra att VIPar på troops eller planet inte kan påverka
        // striden. VIPar som inte har egeneskaper som påverkar troops borde inte kunna
        // vara på en troop.
        List<TaskForceSpaceShip> taskForceSpaceShips = new ArrayList<>();
        SpaceshipPureFunctions.getPlayersSpaceshipsOnPlanet(aPlayer, aPlanet, galaxy.getSpaceships()).stream()
                .filter(spaceship -> (!spaceship.isCivilian() || includeCivilians))
                .forEach(spaceship -> taskForceSpaceShips
                        .add(new TaskForceSpaceShip(spaceship, VipPureFunctions.findAllVIPsOnShip(spaceship, galaxy.getAllVIPs()))));

        TaskForce tf = new TaskForce(aPlayer != null ? aPlayer.getGovernorName() : null, aPlayer != null ? aPlayer.getFaction().getName() : null, taskForceSpaceShips);

        if (tf.getTotalNrShips() == 0) { // om inga skepp returnera null = finns ingen taskforce
            return null;
        }

        // 2019-12-25 add possible planets to retreat to. Changed is made to remove the Galaxy object from battleHandler.
        if(aPlayer != null) { //Only players ship can retreat
            tf.addClosestFriendlyPlanets(SpaceshipRange.LONG, PlanetPureFunctions.findClosestPlanets(aPlanet, aPlayer, SpaceshipRange.LONG,
                    FindPlanetCriterion.OWN_PLANET_NOT_BESIEGED, null, galaxy));

            tf.addClosestFriendlyPlanets(SpaceshipRange.SHORT, PlanetPureFunctions.findClosestPlanets(aPlanet, aPlayer, SpaceshipRange.SHORT,
                    FindPlanetCriterion.OWN_PLANET_NOT_BESIEGED, null, galaxy));
        }

        return tf;
    }
}
