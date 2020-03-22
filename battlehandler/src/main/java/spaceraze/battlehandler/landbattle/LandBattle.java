package spaceraze.battlehandler.landbattle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.VIP;
import spaceraze.world.enums.TroopTargetingType;
import spaceraze.world.enums.TypeOfTroop;
import spaceraze.world.report.landbattle.EnemyTroop;
import spaceraze.world.report.landbattle.OwnTroop;
import spaceraze.world.report.landbattle.TroopState;

public class LandBattle {
	private String planetName;
	private int planetResistence;
	private int currentTurn;
	private LandBattleGroup defBG;
	private LandBattleGroup attBG;
	
	public LandBattle(List<TaskForceTroop> defendingTaskForceTroops, List<TaskForceTroop> attackingTaskForceTroops, String planetName, int resistence, int currentTurn){
		// create battle groups
    	this.defBG = new LandBattleGroup(defendingTaskForceTroops);
    	this.attBG = new LandBattleGroup(attackingTaskForceTroops);
		
		this.planetName = planetName;
		this.planetResistence = resistence;
		this.currentTurn = currentTurn;
	}
	
	public void performBattle(){
		Logger.finer("performBattle() called");
		Logger.finer("Current turn: " + currentTurn);
    	
    	// perform battle group lineup
		Logger.finer("Starting lineup");
    	defBG.performLineup();
    	attBG.performLineup();
    	defBG.modifyLineup(attBG);
    	attBG.modifyLineup(defBG);
    	
    	Map<String, OwnTroop> attBGOwnTroops = createOwnTroops(attBG.getTroops());
	    Map<String, EnemyTroop> attBGEnemyTroops = createEnemyTroops(attBG.getTroops());
	    Map<String, OwnTroop> defBGOwnTroops = createOwnTroops(defBG.getTroops());
	    Map<String, EnemyTroop> defBGEnemyTroops = createEnemyTroops(defBG.getTroops());

		Logger.finer("Lineup finished");
    	// set unit opposition
    	OpponentHandler opponentHandler = new OpponentHandler();
    	setOpponents(attBG,defBG,opponentHandler);
    	// create master attack list
		Logger.finer("Creating master attack list");
    	List<LandBattleAttack> attackList = new LinkedList<>();
    	// add all close combat & support troops to master attack list
    	defBG.addToMasterAttackList(attackList,opponentHandler,currentTurn,attBG,true, planetResistence);
    	printMasterAttackList(attackList);
    	attBG.addToMasterAttackList(attackList,opponentHandler,currentTurn,defBG,false, planetResistence);
    	printMasterAttackList(attackList);
		Logger.finer("Master attack list finished");
    	// randomize master attack list
    	Collections.shuffle(attackList);
    	printMasterAttackList(attackList);
		Logger.finer("Master attack list shuffled");
		
		//2020-01-26 skapar rapporten innan själva striden är genomförd, detta för att kunna lägga till attacker i rapporten. Efter striden uppdateras troop listorna med status efter striden via postBattleUpdateTroop();
		if(!attBGOwnTroops.isEmpty()) {
			attBG.setReport(createReport(attBG, defBG, attBGOwnTroops.values(), defBGEnemyTroops.values(), false));
		}
		if(!defBGOwnTroops.isEmpty()) {
			defBG.setReport(createReport(defBG, attBG, defBGOwnTroops.values(), attBGEnemyTroops.values(), true));
		}
    	// traverse master attack list and perform attacks and counter attacks
		performAttacks(attackList);
    	// test, write result...
    	Logger.finer("");
    	Logger.finer("Attackers battle report:");
    	Logger.finer("------------------------");
    	Logger.finer("");
    	//Logger.finer(attackingBattleReport.getAsString(ReportLevel.MEDIUM));
    	Logger.finer("");
    	Logger.finer("Defenders battle report:");
    	Logger.finer("------------------------");
    	Logger.finer("");
    	
    	if(!attBGOwnTroops.isEmpty()) {
    		postBattleUpdateTroop(attBG, attBGOwnTroops);
    		postBattleUpdateTroop(attBG, attBGEnemyTroops);
    	}
    	if(!defBGOwnTroops.isEmpty()) {
    		postBattleUpdateTroop(defBG, defBGOwnTroops);
        	postBattleUpdateTroop(defBG, defBGEnemyTroops);
    	}
	}
	
	
	private spaceraze.world.report.landbattle.LandBattleReport createReport(LandBattleGroup ownLandBattleGroup, LandBattleGroup enemyLandBattleGroup, Collection<OwnTroop> ownTroops, Collection<EnemyTroop> enemyTroops, boolean isDefending) {
    	if(ownLandBattleGroup.getTroops().get(0).getTroop().getOwner() == null) { // no player is the same as Neutral or a simulation.
    		return null;
    	}
    	List<OwnTroop> ownTroops1 = new ArrayList<>(ownTroops);
    	List<EnemyTroop> enemyTroops1 = new ArrayList<>(enemyTroops);
		String enemyName = enemyLandBattleGroup.getTroops().get(0).getTroop().getOwner() == null ? null : enemyLandBattleGroup.getTroops().get(0).getTroop().getOwner().getName();
		String enemyFaction = enemyLandBattleGroup.getTroops().get(0).getTroop().getOwner() == null ? null : enemyLandBattleGroup.getTroops().get(0).getTroop().getOwner().getFaction().getName();
		return new spaceraze.world.report.landbattle.LandBattleReport(ownTroops1, enemyTroops1, enemyName, enemyFaction, isDefending);
		
	}
	
	/**
	 * Used when testing
	 */
	private void printMasterAttackList(List<LandBattleAttack> attackList){
		Logger.finer("Master attack printout:");
		for (LandBattleAttack attack : attackList) {
			Logger.finer("  " + attack.toString());
		}
	}
	
	private void performAttacks(List<LandBattleAttack> attackList){
		for (LandBattleAttack attack : attackList) {
			Logger.finer("***** " + attack.toString() + " *****");
			attack.performAttack(attBG,defBG, getVIPBonus(attBG.getTroops()), getVIPBonus(defBG.getTroops()));
		}
	}
	
	private int getVIPBonus(List<TaskForceTroop> taskForceTroops) {
		//TODO 2020-01-04 Test this out, do the compering work with null value?
		return taskForceTroops.stream().filter(taskForceTroop -> !taskForceTroop.getTroop().isDestroyed())
				.flatMap(taskForceTroop -> taskForceTroop.getVipOnTroop().stream())
				.map(VIP::getLandBattleGroupAttackBonus).mapToInt(v -> v).max().orElse(0);

		// is there any VIPS with group attacks bonus in this Landbattlegroup?
		/*		
		List<VIP> groupBonusVIPs = new LinkedList<VIP>();
				for (Troop aTroop : attackingTroops) {
					groupBonusVIPs.addAll(g.findLandBattleVIPs(aTroop,true));
				}
				for (VIP vip : groupBonusVIPs) {
					if (vip.getLandBattleGroupAttackBonus() > attVipBonus){
						Logger.info("###(Tobbe)##### Adding VIP bonus = "  + attVipBonus);
						attVipBonus = vip.getLandBattleGroupAttackBonus();
					}
				}*/
	}
	
	

    private void setOpponents(LandBattleGroup attBG,LandBattleGroup defBG, OpponentHandler opponentHandler){
    	// add smaller firstline to larger first line randomly
    	if (attBG.getNrFirstLine() > defBG.getNrFirstLine()){
    		Logger.finer("Attckar first line > defender first line");
    		defBG.addFirstLineOpponents(attBG,opponentHandler);
    	}else{
    		Logger.finer("Defender first line > attacker first line");
    		attBG.addFirstLineOpponents(defBG,opponentHandler);
    	}
    	// add unopposed firstline in larger force randomly
    	if (attBG.getNrFirstLine() > defBG.getNrFirstLine()){
    		Logger.finer("Attckar first line > defender first line (add second opponent)");
    		attBG.addFirstLineOpponents2(defBG,opponentHandler);
    	}else{
    		Logger.finer("Defender first line > attacker first line (add second opponent)");
    		defBG.addFirstLineOpponents2(attBG,opponentHandler);
    	}
    	// the force with reserves (only one side may have reserves left) may add these "smart" to opponents with only 1 opponent 
    	if (attBG.getNrReserves() > 0){
    		Logger.finer("Attckar reserve");
    		attBG.addReserveOpponents(defBG,opponentHandler);
    	}else{
    		Logger.finer("Defender reserve)");
    		defBG.addReserveOpponents(attBG,opponentHandler);
    	}
    	// if both size have flankers
    	if (attBG.getNrFlankers() > 0 && defBG.getNrFlankers() > 0){
    		// match them up randomly until one side have no unopposed flankers left
    		Logger.finer("Flankers vs flankers");
    		addFlankersVsFlankers(attBG,defBG,opponentHandler);
    	}
    	// if one side have unopposed flankers
    	if (attBG.getNrUnopposedFlankers(opponentHandler) > 0 && defBG.getNrSupport() > 0){
    		// any flankers left attack random support troop
    		Logger.finer("Attckar flanker aginst support");
    		addFlankersVsSupport(attBG,defBG,opponentHandler);
    	}else
    	if (defBG.getNrUnopposedFlankers(opponentHandler) > 0 && attBG.getNrSupport() > 0){
    		// any flankers left attack random support troop
    		Logger.finer("Defender flanker aginst support");
    		addFlankersVsSupport(defBG,attBG,opponentHandler);
    	}
    	// if one side still have unopposed flankers
       	if (attBG.getNrUnopposedFlankers(opponentHandler) > 0){
    		// any flankers left attack random firstline troop in the back
       		Logger.finer("Attckar flanker aginst first line");
       		addFlankersVsFirstLine(attBG,defBG,opponentHandler);
    	}else
    	if (defBG.getNrUnopposedFlankers(opponentHandler) > 0){
    		// any flankers left attack random firstline troop in the back
    		Logger.finer("Defender flanker aginst first line");
       		addFlankersVsFirstLine(defBG,attBG,opponentHandler);
    	}
    }

    private void addFlankersVsFirstLine(LandBattleGroup flankingBG,LandBattleGroup stabbedBG,OpponentHandler opponentHandler){
    	List<TaskForceTroop> tmpStabbed = stabbedBG.getUnstabbedFirstLine(flankingBG, opponentHandler);
    	List<TaskForceTroop> tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
    	
    	while (!tmpStabbed.isEmpty() && !tmpFlankers.isEmpty()){
			
			Collections.shuffle(tmpStabbed);
			Collections.shuffle(tmpFlankers);
			
			
			TaskForceTroop tempTroop = null;
			TaskForceTroop troop;
			TaskForceTroop offTroop;
			
			offTroop = tmpFlankers.get(0);
			int index = 0;
			while (tempTroop == null && tmpStabbed.size() > index) {
				troop = tmpStabbed.get(index);
				if(offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ALLROUND)){
					tempTroop = troop;
					opponentHandler.addOpponents(offTroop, troop);
				} else if (offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ANTIINFANTRY)){
					if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.INFANTRY)){
						tempTroop = troop;
					}
				}else{// ANTITANK
					if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.ARMORED)){
						tempTroop = troop;
					}
				}
				index++;
			}
			if(tempTroop == null){
				tempTroop = tmpStabbed.get(0);
			}
			
			
			opponentHandler.addOpponents(offTroop,tempTroop);
			
			tmpStabbed = stabbedBG.getUnstabbedFirstLine(flankingBG,opponentHandler);
	    	tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
		}
    	
    	if (flankingBG.getNrUnopposedFlankers(opponentHandler) > 0){
    		// add up to two flankers against each firstline
        	tmpStabbed = stabbedBG.getMaxOneStabberFirstLine(flankingBG, opponentHandler);
        	tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
        	while (!tmpStabbed.isEmpty() && !tmpFlankers.isEmpty()){
    			
    			Collections.shuffle(tmpStabbed);
    			Collections.shuffle(tmpFlankers);
    			
    			
    			TaskForceTroop tempTroop = null;
    			TaskForceTroop troop;
    			TaskForceTroop offTroop;
    			
    			offTroop = tmpFlankers.get(0);
    			int index = 0;
    			while (tempTroop == null && tmpStabbed.size() > index) {
    				troop = tmpStabbed.get(index);
    				if(offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ALLROUND)){
    					tempTroop = troop;
    					opponentHandler.addOpponents(offTroop, troop);
    				} else if (offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ANTIINFANTRY)){
    					if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.INFANTRY)){
    						tempTroop = troop;
    					}
    				}else{// ANTITANK
    					if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.ARMORED)){
    						tempTroop = troop;
    					}
    				}
    				index++;
    			}
    			//TODO 2020-03-19 vad är det här?  tempTroops används inte, är det tänkt att den ska användas i opponentHandler.addOpponents(tmpFlankers.get(0),tmpStabbed.get(0));
    			if(tempTroop == null){
    				tempTroop = tmpStabbed.get(0);
    			}
        		opponentHandler.addOpponents(tmpFlankers.get(0),tmpStabbed.get(0));
            	tmpStabbed = stabbedBG.getMaxOneStabberFirstLine(flankingBG, opponentHandler);
            	tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
        	}
    	}
    }

    private void addFlankersVsSupport(LandBattleGroup flankingBG,LandBattleGroup supportBG,OpponentHandler opponentHandler){
    	List<TaskForceTroop> tmpSupport = supportBG.getUnopposedSupport(opponentHandler);
    	List<TaskForceTroop> tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
    	while (!tmpSupport.isEmpty() && !tmpFlankers.isEmpty()){
        	Collections.shuffle(tmpSupport);
        	Collections.shuffle(tmpFlankers);
        	opponentHandler.addOpponents(tmpFlankers.get(0),tmpSupport.get(0));
        	tmpSupport = supportBG.getUnopposedSupport(opponentHandler);
        	tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
    	}
    	if (flankingBG.getNrUnopposedFlankers(opponentHandler) > 0){
    		// add up to two flankers against each support
        	tmpSupport = supportBG.getSupportWithMaxOneOpponent(opponentHandler);
        	tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
        	while (!tmpSupport.isEmpty() && !tmpFlankers.isEmpty()){
            	Collections.shuffle(tmpSupport);
            	Collections.shuffle(tmpFlankers);
        		opponentHandler.addOpponents(tmpFlankers.get(0),tmpSupport.get(0));
            	tmpSupport = supportBG.getSupportWithMaxOneOpponent(opponentHandler);
            	tmpFlankers = flankingBG.getUnopposedFlankers(opponentHandler);
        	}
    	}
    }

	private void addFlankersVsFlankers(LandBattleGroup attBG,LandBattleGroup defBG, OpponentHandler opponentHandler){
		while (attBG.getNrUnopposedFlankers(opponentHandler) > 0 && defBG.getNrUnopposedFlankers(opponentHandler) > 0){
			List<TaskForceTroop> attFlankers = attBG.getUnopposedFlankers(opponentHandler);
			List<TaskForceTroop> defFlankers = defBG.getUnopposedFlankers(opponentHandler);
			Collections.shuffle(attFlankers);
			Collections.shuffle(defFlankers);
			
			Logger.finer("Number of attacking flankers: " +  attFlankers.size());
			Logger.finer("Number of defending flankers: " +  defFlankers.size());
			
			TaskForceTroop tempTroop = null;
			TaskForceTroop troop;
			TaskForceTroop offTroop;
			int randomInt = Functions.getRandomInt(1, 2);
			if(randomInt == 1){
				offTroop = attFlankers.get(0);
				int index = 0;
				while (tempTroop == null && defFlankers.size() > index) {
					troop = defFlankers.get(index);
					if(offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ALLROUND)){
						tempTroop = troop;
						opponentHandler.addOpponents(offTroop, troop);
					} else if (offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ANTIINFANTRY)){
						if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.INFANTRY)){
							tempTroop = troop;
						}
					}else{// ANTITANK
						if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.ARMORED)){
							tempTroop = troop;
						}
					}
					index++;
				}
				if(tempTroop == null){
					tempTroop = defFlankers.get(0);
				}
			}else{
				offTroop = defFlankers.get(0);
				int index = 0;
				while (tempTroop == null && attFlankers.size() > index) {
					troop = attFlankers.get(index);
					if(offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ALLROUND)){
						tempTroop = troop;
						opponentHandler.addOpponents(offTroop, troop);
					} else if (offTroop.getTroop().getTargetingType().equals(TroopTargetingType.ANTIINFANTRY)){
						if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.INFANTRY)){
							tempTroop = troop;
						}
					}else{// ANTITANK
						if(troop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.ARMORED)){
							tempTroop = troop;
						}
					}
					index++;
				}
				if(tempTroop == null){
					tempTroop = attFlankers.get(0);
				}
			}
			
			opponentHandler.addOpponents(offTroop,tempTroop);
		}
	}
	
	private Map<String, OwnTroop> createOwnTroops(List<TaskForceTroop> taskForceTroops) {
    	Map<String, OwnTroop> ownTroops = new HashMap<>();
    //	if(taskForceTroops.get(0).getTroop().getOwner() != null) { // no player is the same as Neutral or a simulation.
    		taskForceTroops.stream().map(TaskForceTroop::getTroop)
    		.forEach(troop -> ownTroops.put(troop.getUniqueName(), new OwnTroop(troop.getUniqueName(), troop.getTroopType().getUniqueName(), troop.getCurrentStrength(), troop.getPosition())));
    //	}
    	return ownTroops;
	}
    
    private Map<String, EnemyTroop> createEnemyTroops(List<TaskForceTroop> taskForceTroops) {
    	Map<String, EnemyTroop> enemyTroops = new HashMap<>();
    //	if(taskForceTroops.get(0).getTroop().getOwner() != null) { // no player is the same as Neutral or a simulation.
    		taskForceTroops.stream().map(TaskForceTroop::getTroop)
    		.forEach(troop -> enemyTroops.put(troop.getUniqueName(), new EnemyTroop(troop.getTroopType().getUniqueName(), troop.getCurrentStrength(), troop.getPosition())));
    //	}
    	return enemyTroops;
	}
    
    private void postBattleUpdateTroop(LandBattleGroup landBattleGroup, Map<String, ? extends TroopState> troops) {
    	landBattleGroup.getTroops().stream().map(TaskForceTroop::getTroop)
		.forEach(troop -> troops.get(troop.getUniqueName()).setPostBattleHitpoints(troop.getCurrentStrength()));
	}

	public LandBattleGroup getDefBG() {
		return defBG;
	}

	public LandBattleGroup getAttBG() {
		return attBG;
	}

	
}
