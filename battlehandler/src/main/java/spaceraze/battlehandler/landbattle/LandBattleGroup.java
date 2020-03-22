package spaceraze.battlehandler.landbattle;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import spaceraze.util.general.Logger;
import spaceraze.world.enums.BattleGroupPosition;
import spaceraze.world.enums.LandBattleAttackType;
import spaceraze.world.enums.TroopTargetingType;
import spaceraze.world.enums.TypeOfTroop;
import spaceraze.world.report.landbattle.LandBattleReport;

public class LandBattleGroup {
	private List<TaskForceTroop> troops;
	// sub-lists
	private List<TaskForceTroop> firstLine;
	private List<TaskForceTroop> reserve;
	private List<TaskForceTroop> flankers;
	private List<TaskForceTroop> support;
	private LandBattleReport report;
	
	public LandBattleGroup(List<TaskForceTroop> someTroops){
		troops = someTroops;
		firstLine = new LinkedList<>();
		reserve = new LinkedList<>();
		flankers = new LinkedList<>();
		support = new LinkedList<>();
		
	      
	}
	
	public void addToMasterAttackList(List<LandBattleAttack> attackList,OpponentHandler opponentHandler,int currentTurn, LandBattleGroup otherBattleGroup, boolean defending, int aResistance){
		// add all troops
		for (TaskForceTroop aTroop : troops) {
			List<TaskForceTroop> opponents = opponentHandler.getOpponents(aTroop);
			BattleGroupPosition aPosition = getPosition(aTroop);
			Logger.finer("addToMaster: " + aTroop.getTroop().getUniqueShortName() + " " + (aPosition == BattleGroupPosition.SUPPORT) + " " + aTroop.getTroop().getAttackArtillery() + " " + opponents.size());
			if (!opponents.isEmpty()){
				if (aPosition == BattleGroupPosition.FIRST_LINE){
					int nrAttacks = getNrAttacks(aTroop,currentTurn);
					List<TaskForceTroop> eligbleOpponents = getFirstLineOpponentsOnly(opponents, otherBattleGroup);
					LandBattleAttackGround groundAttack = new LandBattleAttackGround(LandBattleAttackType.FIRSTLINE_VS_FIRSTLINE,aTroop,eligbleOpponents,defending,aResistance);
					createAttacks(groundAttack,nrAttacks,attackList);
				}else
				if (aPosition == BattleGroupPosition.FLANKER){
					int nrAttacks = getNrAttacks(aTroop,currentTurn);
					LandBattleAttackType attackType = opponentHandler.getFlankerAttackType(opponents,otherBattleGroup);
					LandBattleAttackGround groundAttack = new LandBattleAttackGround(attackType,aTroop,opponents,defending,aResistance);
					createAttacks(groundAttack,nrAttacks,attackList);
				}else
				if (aPosition == BattleGroupPosition.SUPPORT){ // support attacked by flankers perform ground attacks against their attackers, instead of using their artillery attack
					int nrAttacks = getNrAttacks(aTroop,currentTurn);
					LandBattleAttackGround groundAttack = new LandBattleAttackGround(LandBattleAttackType.SUPPORT_VS_FLANKER,aTroop,opponents,defending,aResistance);
					createAttacks(groundAttack,nrAttacks,attackList);
				}
				// Note: reserve never have an opponent and defence cannot attack, only do counter-fire
			}else
			if (aPosition == BattleGroupPosition.SUPPORT && aTroop.getTroop().getAttackArtillery() > 0){
				Logger.finer("-> addingToMaster");
				// add artillery
				int nrAttacks = getNrAttacks(aTroop,currentTurn);
				List<TaskForceTroop> eligbleOpponents = otherBattleGroup.getTroops();
				LandBattleAttackArtillery artilleryAttack = new LandBattleAttackArtillery(aTroop,eligbleOpponents,defending,aResistance);
				createAttacks(artilleryAttack,nrAttacks,attackList);
			}	
		}		
	}
/*	
	public List<Troop> getAllTroops(){
		return troops;
	}
	*/
	private List<TaskForceTroop> getFirstLineOpponentsOnly(List<TaskForceTroop> opponents, LandBattleGroup otherBattleGroup){
		List<TaskForceTroop> eligbleOpponents = new LinkedList<>();
		for (TaskForceTroop troop : opponents) {
			if (otherBattleGroup.isFirstLine(troop)){
				eligbleOpponents.add(troop);
			}
		}
		return eligbleOpponents;
	}
	
	private void createAttacks(LandBattleAttack anAttack, int nrAttacks, List<LandBattleAttack> attackList){
		for (int i = 0; i < nrAttacks; i++){
			attackList.add(anAttack);
		}
	}
	
	private int getNrAttacks(TaskForceTroop aTroop, int currentTurn){
		Logger.finer("getNrAttacks; " + aTroop.getTroop().getUniqueShortName());
		// get basic nr of attacks
		int attacks = aTroop.getTroop().getTroopType().getNrAttacks();
		// is there any VIPs with attack bonus on this troop?
//		List<VIP> bonusVIPs = getLandBattleVIPs(aTroop, g);
//		int maxBonus = 0;
//		for (VIP vip : bonusVIPs) {
//			Logger.finer("bonusVIPs; " + vip.getTroopAttacksBonus());
//			if (vip.getTroopAttacksBonus() > maxBonus){
//				maxBonus = vip.getTroopAttacksBonus();
//			}
//		}
//		Logger.finer("maxBonus; " + maxBonus);
//		attacks += maxBonus;
		// is there any VIPS with group attacks bonus in this Landbattlegroup?
	/*	List<VIP> groupBonusVIPs = getLandBattleVIPs(g);
		int maxGroupBonus = 0;
		for (VIP vip : groupBonusVIPs) {
			LoggingHandler.finer("groupBonusVIPs; " + vip.getLandBattleGroupAttacksBonus());
			if (vip.getLandBattleGroupAttacksBonus() > maxGroupBonus){
				maxGroupBonus = vip.getLandBattleGroupAttacksBonus();
			}
		}
		LoggingHandler.finer("maxGroupBonus; " + maxGroupBonus);
		attacks += maxGroupBonus;*/
		// drop penalty?
		if (aTroop.getTroop().getLastPlanetMoveTurn() == currentTurn){
			attacks -= aTroop.getTroop().getTroopType().getDropPenalty();
			if (attacks < 0){ // is this one needed?
				attacks = 0; 
			}
		}
		return attacks;
	}
	
	/**
	 * Find all land battle VIPs on aTroop
	 */
//	private List<VIP> getLandBattleVIPs(Troop aTroop, Galaxy g){
//		List<VIP> VIPs = new LinkedList<VIP>();
//		VIPs = g.findLandBattleVIPs(aTroop,false);
//		return VIPs;
//	}

	/**
	 * Find all land battle VIPs on troops in this battle group
	 */
//	private List<VIP> getLandBattleVIPs(Galaxy g){
//		List<VIP> VIPs = new LinkedList<VIP>();
//		for (Troop aTroop : troops) {
//			VIPs.addAll(g.findLandBattleVIPs(aTroop,true));
//		}
//		return VIPs;
//	}

	public BattleGroupPosition getPosition(TaskForceTroop aTroop){
		BattleGroupPosition aPosition = BattleGroupPosition.FIRST_LINE;
		if (findTroop(aTroop, reserve)){
			aPosition = BattleGroupPosition.RESERVE;
		}else
		if (findTroop(aTroop, flankers)){
			aPosition = BattleGroupPosition.FLANKER;
		}else
		if (findTroop(aTroop, support)){
			aPosition = BattleGroupPosition.SUPPORT;
		}
		return aPosition;
	}
	
	public void addFirstLineOpponents(LandBattleGroup largerBG, OpponentHandler opponentHandler){
		Collections.shuffle(firstLine);
		for (TaskForceTroop aTroop : firstLine) {
			largerBG.addAsOpposingFirstLine(aTroop,opponentHandler);
		}
	}
	
	/**
	 * Find opponents for troops in the reserve
	 */
	public void addReserveOpponents(LandBattleGroup smallerBG, OpponentHandler opponentHandler){
		List<TaskForceTroop> possibeOpponents = smallerBG.getPossibleOpponents(opponentHandler);
		while (!reserve.isEmpty() && !possibeOpponents.isEmpty()){
			Collections.shuffle(reserve);
			TaskForceTroop anAttacker = reserve.get(0);
			TaskForceTroop anOpponent = smallerBG.findOpponent(possibeOpponents, anAttacker);
			reserve.remove(0);
			firstLine.add(anAttacker); // ska verkligen truppen flyttas till FL??
			opponentHandler.addOpponents(anAttacker,anOpponent);
			possibeOpponents = smallerBG.getPossibleOpponents(opponentHandler);
		}
	}
	
	/**
	 * Find the most effective to attack against
	 */
	private TaskForceTroop findOpponent(List<TaskForceTroop> possibeOpponents, TaskForceTroop anAttacker){
		TaskForceTroop bestVictim = null;
		Collections.shuffle(possibeOpponents);
		for (TaskForceTroop aTroop : possibeOpponents) {
			if (bestVictim == null){
				bestVictim = aTroop;
			}else{
				if (aTroop.getTroop().getSuitableWeight(anAttacker.getTroop()) > bestVictim.getTroop().getSuitableWeight(anAttacker.getTroop())){
					bestVictim = aTroop;
				}
			}
		}
		return bestVictim;
	}
	
	/**
	 * Find all troops that have only 1 opponent
	 */
	public List<TaskForceTroop> getPossibleOpponents(OpponentHandler opponentHandler){
		List<TaskForceTroop> foundTroops = new LinkedList<>();
		for (TaskForceTroop aTroop : firstLine) {
			if (opponentHandler.maxOneOpponent(aTroop)){
				foundTroops.add(aTroop);
			}
		}
		return foundTroops;
	}

	/**
	 * Used to allocate the larger bg:s firstline troops to opponents
	 */
	public void addFirstLineOpponents2(LandBattleGroup smallerBG, OpponentHandler opponentHandler){
		Logger.finer("addFirstLineOpponents2");
		Collections.shuffle(firstLine);
		for (TaskForceTroop aTroop : firstLine) {
			Logger.finer("aTroop: " + aTroop.getTroop().getUniqueShortName());
			if (opponentHandler.noOpponent(aTroop)){
				Logger.finer("aTroop has no opponent");
				TaskForceTroop newOpponent = smallerBG.findSmallerOpponent(opponentHandler, aTroop.getTroop().getTargetingType());
				if (newOpponent != null){
					Logger.finer("aTroop new opponent: " + newOpponent.getTroop().getUniqueShortName());
					opponentHandler.addOpponents(newOpponent,aTroop);
				}
			}
		}
	}
	
	public TaskForceTroop findSmallerOpponent(OpponentHandler opponentHandler, TroopTargetingType targetType){
    	Logger.finer("findSmallerOpponent");
    	TaskForceTroop found = null;
    	TaskForceTroop firstTroopWithNoOpponentButWrongTypeOfTroop = null;
		Collections.shuffle(firstLine);
		int counter = 0;
		while (found == null && counter < firstLine.size()){
	    	Logger.finer("counter: " + counter);
	    	TaskForceTroop firstLineTroop = firstLine.get(counter);
	    	Logger.finer("firstLineTroop: " + firstLineTroop.getTroop().getUniqueShortName());
			if (opponentHandler.maxOneOpponent(firstLineTroop)){
		    	Logger.finer("maxOneOpponent=true ");
		    	if(targetType.equals(TroopTargetingType.ALLROUND)){
		    		found = firstLineTroop;
				} else if (targetType.equals(TroopTargetingType.ANTIINFANTRY)){
					if(firstLineTroop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.INFANTRY)){
						found = firstLineTroop;
					}else{
						if(firstTroopWithNoOpponentButWrongTypeOfTroop == null){
							firstTroopWithNoOpponentButWrongTypeOfTroop = firstLineTroop;
						}
					}
				}else{// ANTITANK
					if(firstLineTroop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.ARMORED)){
						found = firstLineTroop;
					}else{
						if(firstTroopWithNoOpponentButWrongTypeOfTroop == null){
							firstTroopWithNoOpponentButWrongTypeOfTroop = firstLineTroop;
						}
					}
				}
		    	
			}
			counter++;
			
		}
		
		if(found == null && firstTroopWithNoOpponentButWrongTypeOfTroop != null){
			found = firstTroopWithNoOpponentButWrongTypeOfTroop;
		}
		
		return found;
	}

	private boolean findTroop(TaskForceTroop aTroop, List<TaskForceTroop> aList){
		boolean found = false;
		int counter = 0;
		while (!found && counter < aList.size()){
			TaskForceTroop listTroop = aList.get(counter);
			if (listTroop == aTroop){
				found = true;
			}else{
				counter++;
			}
		}
		return found;
	}

	public void addAsOpposingFirstLine(TaskForceTroop aTroop, OpponentHandler opponentHandler){
		Collections.shuffle(firstLine);
		boolean found = false;
		int counter = 0;
		TaskForceTroop firstTroopWithNoOpponentButWrongTypeOfTroop = null;
		while (!found && counter < firstLine.size()){
			TaskForceTroop firstLineTroop = firstLine.get(counter);
			if (opponentHandler.noOpponent(firstLineTroop)){
				if(aTroop.getTroop().getTargetingType().equals(TroopTargetingType.ALLROUND)){
					opponentHandler.addOpponents(aTroop, firstLineTroop);
					found = true;
				} else if (aTroop.getTroop().getTargetingType().equals(TroopTargetingType.ANTIINFANTRY)){
					if(firstLineTroop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.INFANTRY)){
						opponentHandler.addOpponents(aTroop, firstLineTroop);
						found = true;
					}else{
						if(firstTroopWithNoOpponentButWrongTypeOfTroop == null){
							firstTroopWithNoOpponentButWrongTypeOfTroop = firstLineTroop;
						}
					}
				}else{// ANTITANK
					if(firstLineTroop.getTroop().getTroopType().getTypeOfTroop().equals(TypeOfTroop.ARMORED)){
						opponentHandler.addOpponents(aTroop, firstLineTroop);
						found = true;
					}else{
						if(firstTroopWithNoOpponentButWrongTypeOfTroop == null){
							firstTroopWithNoOpponentButWrongTypeOfTroop = firstLineTroop;
						}
					}
				}
			}
			counter++;
		}
		if(!found && firstTroopWithNoOpponentButWrongTypeOfTroop != null){
			opponentHandler.addOpponents(aTroop, firstTroopWithNoOpponentButWrongTypeOfTroop);
		}
	}
	
	public List<TaskForceTroop> getSupportWithMaxOneOpponent(OpponentHandler opponentHandler){
		return getTroopsWithMaxOneOpponent(support, opponentHandler);
	}

	public List<TaskForceTroop> getTroopsWithMaxOneOpponent(List<TaskForceTroop> troopList, OpponentHandler opponentHandler){
		List<TaskForceTroop> maxOneList = new LinkedList<>();
		for (TaskForceTroop aTroop : troopList) {
			if (opponentHandler.maxOneOpponent(aTroop)){
				maxOneList.add(aTroop);
			}
		}
		return maxOneList;
	}

	/**		
	 *     Först ställs trupperna upp:
    		Explicita order till enheter:
    		-first line
    		-reserve (kan tilldelas slumpvis om styrkan är större än motståndaren, överblivna firstline enheter kan inte slåss mot support eller flankers om de inte är i dubbla antal.)
    		-flanker (anfaller motståndans fria flankers o annars försöker de anfalla motståndarens support) (kan ha default sann)
    		-support/artillery/AA (skyddas av first line + flankers mot andra ) (kan ha default sann)
    		Saknas order så har trupperna ev. defaultorder.

	 */
	public void performLineup(){
		for (TaskForceTroop troop : troops) {
			if (troop.getTroop().getPosition() == BattleGroupPosition.FIRST_LINE){
				Logger.finer(troop.getTroop().getUniqueShortName() + ": BattleGroupPosition.FIRST_LINE");
				firstLine.add(troop);
			}else
			if (troop.getTroop().getPosition() == BattleGroupPosition.FLANKER){
				flankers.add(troop);
			}else
			if (troop.getTroop().getPosition() == BattleGroupPosition.RESERVE){
				reserve.add(troop);
			}else{
				support.add(troop);
			}
		}
	}

	/**		
    		Om en styrka är mindre än hälften i first line än motståndaren så används ev. flankers f�r att fylla på first line.
    		Om ena sidan efter detta fortfarande har mindre än hälften så många first line så används support troops för att fylla på first line.

	 */
	public void modifyLineup(LandBattleGroup otherBattleGroup){
		int nrToMove;
		if (otherBattleGroup.getNrFirstLine() > getNrFirstLine()){
			nrToMove = otherBattleGroup.getNrFirstLine() - getNrFirstLine();
			// use reserves to strengthen first line
			moveToFirstLine(reserve,nrToMove);
		}
		if (otherBattleGroup.getNrFirstLine() == getNrFirstLine() && otherBattleGroup.getNrReserves() > 0 && getNrReserves() > 0){
			// move an equal nr of reserves to first line until (at least) one BG have no reserves left
			int moveNr = Math.min(otherBattleGroup.getNrReserves(),getNrReserves());
			otherBattleGroup.moveReservesToFirstLine(moveNr);
			moveReservesToFirstLine(moveNr);
		}else
		if (otherBattleGroup.getNrFirstLine() > (getNrFirstLine()*2)){
			nrToMove = getNrToMove(otherBattleGroup.getNrFirstLine(),getNrFirstLine());
			// use flankers to strengthen first line
			moveToFirstLine(flankers,nrToMove);
			if (otherBattleGroup.getNrFirstLine() > (getNrFirstLine()*2)){
				nrToMove = getNrToMove(otherBattleGroup.getNrFirstLine(),getNrFirstLine());
				// use support to strengthen first line
				moveToFirstLine(support,nrToMove);
				
			}
		}
	}
	
	public List<TaskForceTroop> getUnopposedFlankers(OpponentHandler opponentHandler){
		return getUnopposedTroops(flankers, opponentHandler);
	}

	public List<TaskForceTroop> getUnopposedSupport(OpponentHandler opponentHandler){
		return getUnopposedTroops(support, opponentHandler);
	}

	private List<TaskForceTroop> getUnopposedTroops(List<TaskForceTroop> troopList, OpponentHandler opponentHandler){
		List<TaskForceTroop> unopposedFlankers = new LinkedList<>();
		for (TaskForceTroop aTroop : troopList) {
			if (opponentHandler.noOpponent(aTroop)){
				unopposedFlankers.add(aTroop);
			}
		}
		return unopposedFlankers;
	}

	public List<TaskForceTroop> getUnstabbedFirstLine(LandBattleGroup flankingBG, OpponentHandler opponentHandler){
		List<TaskForceTroop> unstabbedFirstLine = new LinkedList<>();
		for (TaskForceTroop aTroop : firstLine) {
			if (getNrStabbers(aTroop, flankingBG, opponentHandler) == 0){
				unstabbedFirstLine.add(aTroop);
			}
		}
		return unstabbedFirstLine;
	}

	public List<TaskForceTroop> getMaxOneStabberFirstLine(LandBattleGroup flankingBG, OpponentHandler opponentHandler){
		List<TaskForceTroop> maxOneStabberFirstLine = new LinkedList<>();
		for (TaskForceTroop aTroop : firstLine) {
			if (getNrStabbers(aTroop, flankingBG, opponentHandler) <= 1){
				maxOneStabberFirstLine.add(aTroop);
			}
		}
		return maxOneStabberFirstLine;
	}

	private int getNrStabbers(TaskForceTroop aTroop, LandBattleGroup flankingBG, OpponentHandler opponentHandler){
		int found = 0;
		List<TaskForceTroop> opponents = opponentHandler.getOpponents(aTroop);
		for (TaskForceTroop otherTroop : opponents) {
			if (flankingBG.isFlanker(otherTroop)){
				found++;
			}
		}
		return found;
	}
	
	private boolean isFlanker(TaskForceTroop aTroop){
		return flankers.contains(aTroop);
	}

	private boolean isFirstLine(TaskForceTroop aTroop){
		return firstLine.contains(aTroop);
	}

	/**
	 * If smaller is less than half than larger, return larger-(smaller*2)/2 rounded up.
	 */
	private int getNrToMove(int larger, int smaller){
		int nrToMove = 0;
		int temp = larger - (smaller*2);
		if (temp > 0){
			if (temp%2 > 0){
				nrToMove = temp/2 + 1;
			}else{
				nrToMove = temp/2;
			}
		}
		return nrToMove;
	}

	public void moveReservesToFirstLine(int nrToMove){
		moveToFirstLine(reserve, nrToMove);
	}
	
	private void moveToFirstLine(List<TaskForceTroop> someTroops, int nrToMove){
		int counter = nrToMove;
		while (!someTroops.isEmpty() && counter > 0){
			Collections.shuffle(someTroops);
			TaskForceTroop troopToMove = someTroops.get(0);
			firstLine.add(troopToMove);
			someTroops.remove(troopToMove);
			counter--;
		}
	}

	public int getNrFirstLine(){
		return firstLine.size();
	}

	public int getNrFlankers(){
		return flankers.size();
	}

	public int getNrSupport(){
		return support.size();
	}

	public int getTotalNrTroops(){
		return troops.size();
	}

	public int getNrUnopposedFlankers(OpponentHandler opponentHandler){
		return getUnopposedFlankers(opponentHandler).size();
	}

	public int getNrReserves(){
		return reserve.size();
	}

	public List<TaskForceTroop> getFirstLine() {
		return firstLine;
	}

	public List<TaskForceTroop> getFlankers() {
		return flankers;
	}

	public List<TaskForceTroop> getReserve() {
		return reserve;
	}

	public List<TaskForceTroop> getSupport() {
		return support;
	}

	public List<TaskForceTroop> getTroops() {
		return troops;
	}

	public LandBattleReport getReport() {
		return report;
	}

	public void setReport(LandBattleReport report) {
		this.report = report;
	}
	
	
}
