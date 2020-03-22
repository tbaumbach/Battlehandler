package spaceraze.battlehandler.spacebattle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.enums.InitiativeMethod;
import spaceraze.world.report.EventReport;
import spaceraze.world.report.spacebattle.EnemySpaceship;
import spaceraze.world.report.spacebattle.EnemySpaceshipAttack;
import spaceraze.world.report.spacebattle.OwnSpaceship;
import spaceraze.world.report.spacebattle.OwnSpaceshipAttack;
import spaceraze.world.report.spacebattle.SpaceBattleAttack;
import spaceraze.world.report.spacebattle.SpaceshipState;
import spaceraze.world.spacebattle.TaskForce;
import spaceraze.world.spacebattle.TaskForceSpaceShip;

public class SpaceBattlePerformer {
	
	private InitiativeMethod initMethod = InitiativeMethod.WEIGHTED_1;
	private static final String FIGHTING = "fighting";
//	private boolean gameUpdate = false;
	
	
	/*
	private boolean isGameUpdate() {
		return gameUpdate;
	}
	*/
	
	// Called in simulationMode
	public void performCombat(TaskForce tf1, TaskForce tf2, InitiativeMethod initMethod){
		performCombat(tf1, tf2, initMethod, null);
		
	}
	
	public void performCombat(TaskForce tf1, TaskForce tf2, InitiativeMethod initMethod, String planetName){
		
	      Logger.fine(planetName);
	      //this.galaxy = galaxy;
	      this.initMethod = initMethod;
	      
	      Map<String, OwnSpaceship> tf1OwnSpaceships = createOwnSpaceships(tf1);
	      Map<String, EnemySpaceship> tf1EnemySpaceships = createEnemySpaceship(tf1);
	      Map<String, OwnSpaceship> tf2OwnSpaceships = createOwnSpaceships(tf2);
	      Map<String, EnemySpaceship> tf2EnemySpaceships = createEnemySpaceship(tf2);
	      List<EventReport> attackReports1 = new ArrayList<>();
	      List<EventReport> attackReports2 = new ArrayList<>();
	       
	      
	      Logger.finer("Fighting starts");
	      String tf1status = FIGHTING;
	      String tf2status = FIGHTING;
	      //TODO 2019-12-22, Vad är poängen med Random r = Functions.getRandom() ? nu skickas "r" runt, borde vara samma sak som att använda Functions.getRandom() när den ska användas då den static final.
	      Random r = Functions.getRandom();
	      TaskForceSpaceShip firingShip;
	      // loopa tills ena sidan är "gone"
	      while (stillFighting(tf1status, tf2status)){
	      	Logger.finest("In battle loop: " + tf1status + " " + tf2status);
	      	
	      	SpaceBattleAttack attackReport1 = new SpaceBattleAttack();
	      	attackReports1.add(attackReport1);
	    	SpaceBattleAttack attackReport2 = new SpaceBattleAttack();
	    	attackReports2.add(attackReport2);

	        if (getShootingSide(tf1,tf2,r) == 1){
	          firingShip = getFiringShip(tf1, tf2, r, attackReport1, attackReport2); // returnerar null om ett skepp flyr istället för att skjuta
	          if (firingShip != null){
	        	  Logger.finest("firingShip: " + firingShip.getSpaceship().getName() + " ");
	          }else{
	        	  Logger.finest("firingShip == null ");
	          }
	          tf1status = tf1.getStatus();
	          if (firingShip != null){ // tf2 är beskjutet
	            tf2status = tf2.shipHit(tf1, firingShip, r, attackReport1, attackReport2);
	          }else{   // om inget skepp returneras betyder det att tf1 håller på att retirera
	            tf2status = FIGHTING;
	          }
	        }else{
	          firingShip = getFiringShip(tf2, tf1, r, attackReport2, attackReport1);
	          tf2status = tf2.getStatus();
	          if (firingShip != null){ // tf1 är beskjutet
	            tf1status = tf1.shipHit(tf2, firingShip, r, attackReport2, attackReport1);
	          }else{   // om inget skepp returneras betyder det att tf2 håller på att retirera
	            tf1status = FIGHTING;
	          }
	          // räkna hur många skepp som kommer undan?
	        }
	      }
	      // vem vann?
	      TaskForce tfWinner = tf2;
	      TaskForce tfLoser = tf1;
	      Logger.finer("tf2.getNrShips(): " + tf2.getTotalNrShips());
	      Logger.finer("tf1.getNrShips(): " + tf1.getTotalNrShips());
	      if (tf1.getTotalNrShips() > 0){
	      	Logger.finer("tf1.getNrShips(): " + tf1.getTotalNrShips());
	        tfWinner = tf1;
	        tfLoser = tf2;
	      }
	      
	      postBattleUpdateSpaceship(tf1, tf1OwnSpaceships);
	      postBattleUpdateSpaceship(tf1, tf1EnemySpaceships);
	      if(tf1.getPlayerName() != null) {
	    	  tf1.setSpaceBattleReport(createReport(tf1, tf2, tf1OwnSpaceships.values(), tf2EnemySpaceships.values()));
		      tf1.getSpaceBattleReport().addReports(attackReports1);
	      }
	      
	      postBattleUpdateSpaceship(tf2, tf2OwnSpaceships);
	      postBattleUpdateSpaceship(tf2, tf2EnemySpaceships);
	      if(tf2.getPlayerName() != null) {
	    	  tf2.setSpaceBattleReport(createReport(tf2, tf1, tf2OwnSpaceships.values(), tf1EnemySpaceships.values()));
		      tf2.getSpaceBattleReport().addReports(attackReports2);
	      }
	      
	      Logger.finer("tfLoser.getStatus(): " + tfLoser.getStatus());

	      /* See method checkGovOnNeutralPlanet description for the removed reason.  
	      // check if any governor from same planet as attacker is on planet
	      if (neutral){
	      	checkGovOnNeutralPlanet(aPlanet,notNeutralTaskForce); 
	      }*/
	      Logger.finer("performCombat finished");
	    }

	private boolean stillFighting(String tf1status, String tf2status) {
		return !tf1status.equalsIgnoreCase("destroyed")
				&& !tf2status.equalsIgnoreCase("destroyed")
				&& !tf1status.equalsIgnoreCase("ran away")
				&& !tf2status.equalsIgnoreCase("ran away");
	}
	
	// chansen för att den ena tf:en får skjuta baseras på antalet skepp i resp. flotta samt initiativbonus
    // Generaler, Jedis etc kanske kan öka chansen att få skjuta?
    protected int getShootingSide(TaskForce tf1, TaskForce tf2, Random r){
      int returnValue = 0;
//      double tf1ratio = (tf1.getNrFirstLineShips()*1.0) / ((tf1.getNrFirstLineShips() + tf2.getNrFirstLineShips()*1.0));
      double tf1ratio = getInitRatio(tf1,tf2);
//      double tf1ratio = (tf1.getRelativeSize()*1.0) / ((tf1.getRelativeSize() + tf2.getRelativeSize()*1.0));
//      int tf1initBonus = tf1.getTotalInitBonus() - tf2.getTotalInitBonus();
      int tf1initBonus = getInitBonusTotal(tf1,tf2);
      Logger.finer("tf1initbonus: " + tf1initBonus + " ratio: " + tf1ratio);
      double randomDouble = r.nextDouble();
      if (tf1initBonus > 0){ // increase chance of initiative
        tf1ratio = tf1ratio + ((1.0-tf1ratio)*(tf1initBonus/100.0));
      }else
      if (tf1initBonus < 0){ // decrease chance of initiative
        tf1ratio = tf1ratio*(1.0+(tf1initBonus/100.0));
      }
      Logger.finer("tf1ratio (inc bonuses) --> " + tf1ratio + " randomDouble: " + randomDouble);
      if (tf1ratio > randomDouble){
        returnValue = 1;
      }
      //return Math.abs(r.nextInt()%2) + 1;
      return returnValue;
    }
    
    //TODO 2019-12-26 removed the use of this method. This method will kill also kill the attackers governor, to hard to lose a game on a mistake (the client should prevent this). The method lacks logic around game type (could be all against all = same faction is not the same team).
    /*
    protected void checkGovOnNeutralPlanet(Planet aPlanet, TaskForce tf){
    	Logger.finer("called");
    	boolean neutralPlanet = (aPlanet.getPlayerInControl() == null);
    	if (neutralPlanet){
        	Logger.finer("Planet is neutral");
        	Logger.finer("tf: " + tf);
        	Logger.finer("tf.getPlayer(): " + tf.getPlayer());
    		Faction aFaction = tf.getPlayer().getFaction();
    		Player aPlayer = tf.getPlayer();
    		Galaxy g = aPlayer.getGalaxy();
    		List<VIP> govs = g.getAllGovsFromFactionOnPlanet(aPlanet,aFaction);
        	Logger.finer("Govs found: " + govs.size());
    		for (VIP aVIP : govs) {
				Player vipPlayer = aVIP.getBoss();
    	    	Logger.finer("In loop, removing VIP from governor: " + vipPlayer.getGovenorName());
				g.removeVIP(aVIP);
				vipPlayer.addToHighlights(aPlanet.getName(),HighlightType.TYPE_GOVENOR_ON_HOSTILE_NEUTRAL);
				vipPlayer.addToGeneral("While visiting the planet " + aPlanet.getName() + " on a diplomatic mission, the planet has been attacked by forces belonging to the same faction as you.");
				vipPlayer.addToGeneral("The population, enraged by this betrayal, immediately attacks your Governor.");
//				vipPlayer.defeated(true,g.getTurn()); detta sätt senade i GalaxyUpdater.defeatedPlayers()
			}
    	}
    }*/
    
    protected int getInitBonusTotal(TaskForce tf1, TaskForce tf2){
        int tf1initBonus = tf1.getTotalInitBonus() - tf2.getTotalInitDefence();
        if (tf1initBonus < 0){
        	tf1initBonus = 0;
        }
        int tf2initBonus = tf2.getTotalInitBonus() - tf1.getTotalInitDefence();
        if (tf2initBonus < 0){
        	tf2initBonus = 0;
        }
        return tf1initBonus - tf2initBonus;
      }
    
    protected double getInitRatio(TaskForce tf1, TaskForce tf2){
    	double tf1ratio = 0.0d;
    	if (initMethod == InitiativeMethod.WEIGHTED){
    		Logger.finer("tf1.getRelativeSize(): " + tf1.getRelativeSize());
    		Logger.finer("tf2.getRelativeSize(): " + tf2.getRelativeSize());
    		tf1ratio = getWeightedRatio(0,tf1.getRelativeSize(),tf2.getRelativeSize());
    	}else
       	if (initMethod == InitiativeMethod.WEIGHTED_1){
       		tf1ratio = getWeightedRatio(1,tf1.getRelativeSize(),tf2.getRelativeSize());
       	}else
       	if (initMethod == InitiativeMethod.WEIGHTED_2){
       		tf1ratio = getWeightedRatio(2,tf1.getRelativeSize(),tf2.getRelativeSize());
       	}else
       	if (initMethod == InitiativeMethod.WEIGHTED_3){
       		tf1ratio = getWeightedRatio(3,tf1.getRelativeSize(),tf2.getRelativeSize());
       	}else
    	if (initMethod == InitiativeMethod.LINEAR){
    		tf1ratio = (tf1.getTotalNrFirstLineShips()*1.0) / (tf1.getTotalNrFirstLineShips() + tf2.getTotalNrFirstLineShips()*1.0);
    	}else
       	if (initMethod == InitiativeMethod.FIFTY_FIFTY){
       		tf1ratio = 0.5d;
       	}
    	return tf1ratio;
    }
    
    protected double getWeightedRatio(double base, double tf1RelSize, double tf2RelSize){
    	double tf1RelSizeMod = tf1RelSize + base;
    	double tf2RelSizeMod = tf2RelSize + base;
		return tf1RelSizeMod / (tf1RelSizeMod + tf2RelSizeMod);
    }
    
    public TaskForceSpaceShip getFiringShip(TaskForce attackerTF, TaskForce opponentTF, Random r, SpaceBattleAttack activeAttackReport, SpaceBattleAttack targetAttackReport){
    	
    	TaskForceSpaceShip firingShip ;
        if (!attackerTF.isRunningAway()){
        	attackerTF.runAway(opponentTF);
        }
        if (attackerTF.onlyFirstLine() && !attackerTF.isRunningAway()){
          int nrFirstLineShips = attackerTF.getTotalNrShips(false);
          Logger.finer("nrFirstLineShips: " + nrFirstLineShips);
			int screennr = Math.abs(r.nextInt())%nrFirstLineShips;
			firingShip = attackerTF.getShipAt(false,screennr);
        }else{
			firingShip = attackerTF.getAllSpaceShips().get(Math.abs(r.nextInt())%attackerTF.getAllSpaceShips().size());
        }
        
        if (attackerTF.isRunningAway() && firingShip.getSpaceship().getRange().canMove()){ // tempss försöker fly
      	activeAttackReport.setWantsToRetreat(true);
      	targetAttackReport.setWantsToRetreat(true);
          if (!opponentTF.stopsRetreats()){ // tempss flyr
			  boolean gotAway = firingShip.getSpaceship().retreat(TaskForce.getRandomClosestPlanet(attackerTF, firingShip.getSpaceship().getRange()));
        	attackerTF.getAllSpaceShips().remove(firingShip);
            if (firingShip.getSpaceship().isCarrier()){
            	attackerTF.removeSquadronsFromCarrier(firingShip.getSpaceship());
            }else
            if (firingShip.getSpaceship().isSquadron()){
				firingShip.getSpaceship().setCarrierLocation(null);
            }
            if (!gotAway){ // skeppet färstördes då det inte fanns någonstans att fly till
          	// check if this is a carrier and if there are any squadrons located at it
          	// If thats the case, null their carrierLocation
            	
            	attackerTF.getDestroyedShips().add(firingShip);
              if (attackerTF.getAllSpaceShips().isEmpty()){
            	  attackerTF.setDestroyed();
              }
            }else{ // ship has run away
            	attackerTF.getRetreatedShips().add(firingShip);
            }
          }
        }
        
        activeAttackReport.setSpaceshipAttack(new OwnSpaceshipAttack(firingShip.getSpaceship().getName(), 
        		firingShip.getSpaceship().getTypeName(), 
        		attackerTF.getDestroyedShips().contains(firingShip), 
        		firingShip.getSpaceship().getRetreatingTo() == null ? null : firingShip.getSpaceship().getRetreatingTo().getName()));
        
        targetAttackReport.setSpaceshipAttack(new EnemySpaceshipAttack(firingShip.getSpaceship().getTypeName(), attackerTF.getDestroyedShips().contains(firingShip)));
        
        //Return null  if the ship is destroyed or retreating.
        return attackerTF.getDestroyedShips().contains(firingShip) || firingShip.getSpaceship().getRetreatingTo() != null ? null : firingShip;
      }

    
    private spaceraze.world.report.spacebattle.SpaceBattleReport createReport(TaskForce ownTaskForce, TaskForce enemyTaskForce, Collection<OwnSpaceship> ownSpaceships, Collection<EnemySpaceship> enemySpaceships) {
    	if(ownTaskForce.getPlayerName() == null) { // no player is the same as Neutral or a simulation.
    		return null;
    	}
    	List<OwnSpaceship> ownSpaceships1 = new ArrayList<>(ownSpaceships);
    	List<EnemySpaceship> enemySpaceships1 = new ArrayList<>(enemySpaceships);
		String enemyName = enemyTaskForce.getPlayerName();
		String enemyFaction = enemyTaskForce.getPlayerName() == null ? null : enemyTaskForce.getFactionName();
		return new spaceraze.world.report.spacebattle.SpaceBattleReport(ownSpaceships1, enemySpaceships1, enemyName, enemyFaction);
		
	}
    
    private Map<String, OwnSpaceship> createOwnSpaceships(TaskForce taskForce) {
    	Map<String, OwnSpaceship> ownSpaceships = new HashMap<>();
    //	if(taskForce.getPlayerName() != null) { // no player is the same as Neutral or a simulation.
    		taskForce.getAllSpaceShips().stream().map(TaskForceSpaceShip::getSpaceship)
    			.forEach(ship -> ownSpaceships.put(ship.getUniqueName(), new OwnSpaceship(ship.getName(), ship.getType().getName(), ship.getScreened(), ship.getHullStrength())));
    //	}
    	return ownSpaceships;
	}
    
    private Map<String, EnemySpaceship> createEnemySpaceship(TaskForce taskForce) {
    	Map<String, EnemySpaceship> enemySpaceships = new HashMap<>();
    	taskForce.getAllSpaceShips().stream().map(TaskForceSpaceShip::getSpaceship)
    		.forEach(ship -> enemySpaceships.put(ship.getUniqueName(), new EnemySpaceship(ship.getType().getName(), ship.getScreened(), ship.getHullStrength())));
    	return enemySpaceships;
	}
    
    private void postBattleUpdateSpaceship(TaskForce taskForce, Map<String, ? extends SpaceshipState> spaceships) {
    	if(spaceships != null && !spaceships.isEmpty()) { //Only players have spaceships and gets reports.
    	Stream.concat(taskForce.getAllSpaceShips().stream(), Stream.concat(taskForce.getRetreatedShips().stream(), taskForce.getDestroyedShips().stream()))
		.map(TaskForceSpaceShip::getSpaceship)
		.forEach(taskForceSpaceShip -> {
			spaceships.get(taskForceSpaceShip.getUniqueName()).setPostBattleHullState(taskForceSpaceShip.isDestroyed() ? 0 : taskForceSpaceShip.getHullStrength());
			spaceships.get(taskForceSpaceShip.getUniqueName()).setRetret(taskForceSpaceShip.isRetreating());
			});
    	}
	}
    
    /*
    private void postBattleUpdateEnemySpaceship(TaskForce taskForce, Map<String, EnemySpaceship> enemySpaceships) {
    	Stream.concat(taskForce.getAllSpaceShips().stream(), Stream.concat(taskForce.getRetreatedShips().stream(), taskForce.getDestroyedShips().stream()))
		.map(TaskForceSpaceShip::getSpaceship)
		.forEach(ship -> enemySpaceships.get(ship.getUniqueName()).setPostBattleHullState((ship.getCurrentDc() / ship.getHullStrength()) * 100));
	}
    
    private void postBattleUpdateOwnSpaceshipsWith(TaskForce taskForce, Map<String, OwnSpaceship> ownSpaceships) {
    	Stream.concat(taskForce.getAllSpaceShips().stream(), Stream.concat(taskForce.getRetreatedShips().stream(), taskForce.getDestroyedShips().stream()))
    	.map(TaskForceSpaceShip::getSpaceship)
		.forEach(ship -> ownSpaceships.get(ship.getUniqueName()).setPostBattleHullState((ship.getCurrentDc() / ship.getHullStrength()) * 100));
	}*/

}
