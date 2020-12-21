package spaceraze.battlehandler.spacebattle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


import spaceraze.servlethelper.game.spaceship.SpaceshipMutator;
import spaceraze.servlethelper.game.spaceship.SpaceshipPureFunctions;
import spaceraze.servlethelper.game.vip.VipMutator;
import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.Building;
import spaceraze.world.Galaxy;
import spaceraze.world.GameWorld;
import spaceraze.world.Planet;
import spaceraze.world.Spaceship;
import spaceraze.world.VIP;
import spaceraze.world.enums.SpaceShipSize;
import spaceraze.world.enums.SpaceshipRange;
import spaceraze.world.enums.SpaceshipTargetingType;
import spaceraze.world.report.spacebattle.*;

/**
 *
 * TaskForce är en tillfällig samling rymdskepp vid en planet och skapas för att
 * hantera konflikter
 */
public class TaskForce implements Serializable, Cloneable { // serialiseras denna någonsin??
	static final long serialVersionUID = 1L;
	private static final String FIGHTING = "fighting";
	private List<TaskForceSpaceShip> allShips = null, destroyedShips, retreatedShips;
	private String playerName;
	private String factionName;
	private boolean runningAway = false, isDestroyed = false;
	//private Galaxy galaxy;
	private Map<SpaceshipRange, List<Planet>> closestFriendlyPlanets = new HashMap<>();
	private SpaceBattleReport spaceBattleReport;

	public TaskForce(String playerName, String factionName, List<TaskForceSpaceShip> taskForceSpaceShips) {
		allShips = taskForceSpaceShips;
		destroyedShips = new ArrayList<>();
		retreatedShips = new ArrayList<>();
		//player = p;
		this.playerName = playerName;
		this.factionName = factionName;
		//this.galaxy = galaxy;
	}
	
	public TaskForce(String playerName, String factionName) {
		this(playerName, factionName, new ArrayList<>());
	}

	/*
	public boolean isDefender(Player aPlayer) {
		boolean defender = false;
		if (player != null) {
			if (aPlayer != null) {
				defender = aPlayer.getName().equals(player.getName());
			}
		} else {
			if (aPlayer == null) {
				defender = true;
			}
		}
		return defender;
	}
	*/
	
	/**
	 * Makes a shallow clone of this object (TaskForce), but also makes shallow
	 * clones of all ships in this taskforce.
	 */
	
	/*
	private void setAllSpaceships(List<Spaceship> newShipList) {
		allShips = newShipList;
	}

	private void setDestroyedSpaceships(List<Spaceship> newShipList) {
		destroyedShips = newShipList;
	}

	private void setRetreatingSpaceships(List<Spaceship> newShipList) {
		retreatedShips = newShipList;
	}*/

	public void reloadSquadrons() {
		for (Iterator<TaskForceSpaceShip> iter = allShips.iterator(); iter.hasNext();) {
			Spaceship aShip = iter.next().getSpaceship();
			if (aShip.isSquadron()) {
				if (aShip.getCarrierLocation() != null) {
					SpaceshipMutator.supplyWeapons(aShip, SpaceShipSize.HUGE);
				}
			}
		}
	}

	public int getTotalNrShips() {
		return allShips.size();
	}

	public int getTotalNrNonDestroyedShips() {
		int count = 0;
		for (TaskForceSpaceShip aShip : allShips) {
			if (!aShip.getSpaceship().isDestroyed()) {
				count++;
			}
		}
		return count;
	}

	public int getTotalNrShips(boolean screened) {
		int totalCount = 0;
		totalCount = totalCount + getNrCapitalShips(screened);
		totalCount = totalCount + getNrFighters(screened);
		return totalCount;
	}

	public int getNrCapitalShips(boolean screened) {
		int returnValue = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i).getSpaceship();
			if (ss.isScreened() == screened) {
				if (!ss.isSquadron()) {
					returnValue++;
				}
			}
		}
		return returnValue;
	}

	public int getNrFighters(boolean screened) {
		int returnValue = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i).getSpaceship();
			if (ss.isScreened() == screened) {
				if (ss.isSquadron()) {
					returnValue++;
				}
			}
		}
		return returnValue;
	}

	public int getTotalNrFirstLineShips() {
		return getNrFirstLineCapitalShips() + getNrFirstLineFighters();
	}

	public int getNrFirstLineCapitalShips() {
		int firstLineNr = 0;
		if (onlyFirstLine()) {
			firstLineNr = getNrCapitalShips(false);
		} else {
			firstLineNr = getNrCapitalShips(true) + getNrCapitalShips(false); // add both, there can only be ships in
																				// one of them
		}
		if (getPlayerName() != null) {
			Logger.finer(getPlayerName() + " ffgetNrFirstLineCapitalShips() returns: " + firstLineNr);
		} else {
			Logger.finer("neutral ffgetNrFirstLineCapitalShips() returns: " + firstLineNr);
		}
		return firstLineNr;
	}

	public int getNrFirstLineFighters() {
		int firstLineNr = 0;
		if (onlyFirstLine()) {
			firstLineNr = getNrFighters(false);
		} else {
			firstLineNr = getNrFighters(false) + getNrFighters(true); // add both, there can only be ships in one of
																		// them
		}
		Logger.finer("getNrFirstLineFighters() returns: " + firstLineNr);
		return firstLineNr;
	}

	/**
	 * #ships = relative size 1 = 1 2 = 1.8 3 = 2.5 4 = 3 6 = 3.4 9 = 4 16 = 5 25 =
	 * 6
	 * 
	 * @return relative size value based on nr of firstline ships
	 */
	public double getRelativeSize() {
		double totalRelativeSize = 0;
		totalRelativeSize = totalRelativeSize + getRelativeSize(true);
		totalRelativeSize = totalRelativeSize + getRelativeSize(false);
		if (getPlayerName() != null) {
			Logger.finer(this.getPlayerName() + " getRelativeSize(): " + totalRelativeSize + " squadrons: "
					+ getRelativeSize(false) + " capitals: " + getRelativeSize(true));
		} else {
			Logger.finer("neutral getRelativeSize(): " + totalRelativeSize + " squadrons: " + getRelativeSize(false)
					+ " capitals: " + getRelativeSize(true));
		}
		return totalRelativeSize;
	}

	/**
	 * 
	 * @param capitalShips
	 *            if false, only count fighters
	 * @return
	 */
	private double getRelativeSize(boolean capitalShips) {
		int firstLineNr = 0;
		if (capitalShips) {
			firstLineNr = getNrFirstLineCapitalShips();
		} else {
			firstLineNr = getNrFirstLineFighters();
		}
		double relativeSize = 0;
		if (firstLineNr == 0) {
			relativeSize = 0;
		} else if (firstLineNr == 1) {
			relativeSize = 1;
		} else if (firstLineNr == 2) {
			relativeSize = 1.8;
		} else if (firstLineNr == 3) {
			relativeSize = 2.5;
		} else {
			relativeSize = Math.pow(firstLineNr, 0.5) + 1;
		}
		return relativeSize;
	}

	public String getStatus() {
		String status = FIGHTING;
		if (isDestroyed) {
			status = "destroyed";
		} else if (allShips.size() == 0) {
			status = "ran away";
		} else if (runningAway) {
			status = "running away";
		}
		return status;
	}

	public void removeSquadronsFromCarrier(Spaceship aCarrier) {
		for (Iterator<TaskForceSpaceShip> iter = allShips.iterator(); iter.hasNext();) {
			Spaceship aShip = iter.next().getSpaceship();
			if (aShip.isSquadron()) {
				if (aShip.getCarrierLocation() == aCarrier) {
					aShip.setCarrierLocation(null);
				}
			}
		}
	}

	public void runAway(TaskForce opponentTF, GameWorld gameWorld) {
		if (getPlayerName() != null) { // only player taskforces can run away, neutrals never run
			// kolla om fienden är mer än 4ggr så stort tonnage, försöka fly i så fall
			if ((4 * getStrength(gameWorld)) < (opponentTF.getStrength(gameWorld))) {
				runningAway = true;
			}
		}
	}

	public boolean isRunningAway() {
		return runningAway;
	}

	public TaskForceSpaceShip getShipAt(boolean screened, int position) {
		Logger.finer("getShipAt: " + screened + " " + position);
		TaskForceSpaceShip returnss = null;
		int index = 0;
		int foundnr = 0;
		while (returnss == null) {
			Logger.finer("index: " + index);
			TaskForceSpaceShip tempss = allShips.get(index);
			Logger.finer("tempss: " + tempss.getSpaceship().getName());
			if (tempss.getSpaceship().isScreened() == screened) {
				if (foundnr == position) {
					returnss = tempss;
				} else {
					foundnr++;
				}
			}
			index++;
		}
		return returnss;
	}

	public String shipHit(TaskForce tfshooting, TaskForceSpaceShip firingShip, Random r, SpaceBattleAttack activeAttackReport, SpaceBattleAttack targetAttackReport, GameWorld gameWorld) {
		Logger.finest("called, firingShip: " + firingShip.getSpaceship().getName());

		// returnera "destroyed" om inga skepp finns kvar i tf:n
		// returnera annars "fighting"
		String statusString = FIGHTING;
		TaskForceSpaceShip targetShip = null;
		
		
		int nr = 0;
		
		
		boolean canAttackScreened = canAttackScreened(firingShip);
		// screenOnly == if true only ships in first lines are counted as possible targets. Perhaps the name should change to onlyFirstline?
		boolean screenOnly = (!canAttackScreened) && onlyFirstLine();
				
		int aimedShotChance = tfshooting.getAimBonus(gameWorld) + 40;
		// int aimedShotChance = tfshooting.getAimBonus();
		boolean aimedShot = Functions.getD100(aimedShotChance);
		if (aimedShot) { // the shot will be aimed at the most damaged enemy ship
			// if none is damaged, the shot will be performed as normal
			if (noShipDamaged(gameWorld)) {
				aimedShot = false;
			} else {
				// there are at least one damaged ship in the hit TF
				targetShip = getMostDamagedShip(screenOnly, gameWorld);

			}
		}
		// if shot isn't aimed, perform shot as normal, i.e. use target weight etc
		if (!aimedShot) {
			int targetingWeight = getTotalTargetingWeight(SpaceshipPureFunctions.getSpaceshipTypeByKey(firingShip.getSpaceship().getTypeKey(), gameWorld).getTargetingType(), screenOnly);
			int targetIndex = Math.abs(r.nextInt()) % targetingWeight;
			targetShip = getTargetedShip(SpaceshipPureFunctions.getSpaceshipTypeByKey(firingShip.getSpaceship().getTypeKey(), gameWorld).getTargetingType(), targetIndex, screenOnly);
		}
		if (targetShip.getSpaceship().isScreened() && onlyFirstLine()) {
			Logger.severe("Screened ship hit!!! " + targetShip.getSpaceship().getUniqueName() + " aimedShot: " + aimedShot);
		}
		nr = allShips.indexOf(targetShip);
		// perform shot
		int multiplier = getMultiplier(0);
		//attackReport.setAttMultiplier(multiplier);
		Logger.finest("multiplier: " + multiplier);
		int damageNoArmor = SpaceshipPureFunctions.getDamageNoArmor(firingShip.getSpaceship(), gameWorld, targetShip.getSpaceship(), multiplier);
		activeAttackReport.setDamageNoArmor(damageNoArmor);
		targetAttackReport.setDamageNoArmor(damageNoArmor);
		//attackReport.setDamageNoArmor(damageNoArmor);
		int damageLeftAfterShields = targetShip.getSpaceship().shipShieldsHit(damageNoArmor);
		activeAttackReport.setDamageLeftAfterShields(damageLeftAfterShields);
		targetAttackReport.setDamageLeftAfterShields(damageLeftAfterShields);
		//attackReport.setDamageLeftAfterShields(damageLeftAfterShields);
		double afterShieldsDamageRatio = (damageLeftAfterShields * 1.0d) / damageNoArmor;
		Logger.finer("afterShieldsDamageRatio: " + afterShieldsDamageRatio);
		int actualDamage = SpaceshipMutator.getActualDamage(firingShip.getSpaceship(), gameWorld, targetShip.getSpaceship(), multiplier, afterShieldsDamageRatio);
		activeAttackReport.setActualDamage(actualDamage);
		targetAttackReport.setActualDamage(actualDamage);
		String damagedStatus = shipHit(targetShip.getSpaceship(), actualDamage, damageLeftAfterShields, damageNoArmor, targetShip.getSpaceship().getOwner() != null ? targetShip.getSpaceship().getOwner().getGalaxy() : null, gameWorld);
		Logger.finest("multiplier=" + multiplier + " damageNoArmor=" + damageNoArmor + " damageLeftAfterShields="
				+ damageLeftAfterShields + " afterShieldsDamageRatio=" + afterShieldsDamageRatio + " actualDamage="
				+ actualDamage + " damagedStatus=" + damagedStatus);
		
		if (targetShip.getSpaceship().isDestroyed()) {
			firingShip.getSpaceship().addKill();
			allShips.remove(targetShip);
			if (allShips.size() == 0) {
				isDestroyed = true;
			}
			destroyedShips.add(targetShip);
		}
		
		activeAttackReport.setSpaceshipTarget(createSpaceShipTarget(targetShip.getSpaceship(), false, gameWorld));
		targetAttackReport.setSpaceshipTarget(createSpaceShipTarget(targetShip.getSpaceship(), true, gameWorld));
		
		if (allShips.size() == 0) {
			statusString = "destroyed";
		}
		return statusString;
	}

	private SpaceshipTarget createSpaceShipTarget(Spaceship spaceship, boolean isOwn, GameWorld gameWorld){
		return SpaceshipTarget.builder()
				.name(isOwn? spaceship.getName() : null)
				.typeName(SpaceshipPureFunctions.getSpaceshipTypeByKey(spaceship.getTypeKey(), gameWorld).getName())
				.currentShield(spaceship.getCurrentShields())
				.shield(SpaceshipPureFunctions.getShields(spaceship, gameWorld))
				.currentDamageCapacity(spaceship.getCurrentDc())
				.damageCapacity(spaceship.getDamageCapacity())
				.own(isOwn)
				.build();
	}

	private boolean noShipDamaged(GameWorld gameWorld) {
		boolean noShipDamaged = true;
		int counter = 0;
		while ((counter < allShips.size()) & noShipDamaged) {
			Spaceship tmpss = allShips.get(counter).getSpaceship();
			if (SpaceshipPureFunctions.isDamaged(tmpss, gameWorld)) {
				noShipDamaged = false;
			} else {
				counter++;
			}
		}
		return noShipDamaged;
	}

	private TaskForceSpaceShip getMostDamagedShip(boolean screenOnly, GameWorld gameWorld) {
		TaskForceSpaceShip mostDamagedShip = null;
		List<TaskForceSpaceShip> allSsClone = allShips.stream().collect(Collectors.toList());
		Collections.shuffle(allSsClone);
		for (TaskForceSpaceShip aSpaceship : allSsClone) {
			if (!screenOnly | !aSpaceship.getSpaceship().isScreened()) { // if screen only, only get ships in screen
				if (mostDamagedShip == null) {
					mostDamagedShip = aSpaceship;
				} else {
					if (SpaceshipPureFunctions.getDamageLevel(aSpaceship.getSpaceship(), gameWorld) < SpaceshipPureFunctions.getDamageLevel(mostDamagedShip.getSpaceship(), gameWorld)) {
						mostDamagedShip = aSpaceship;
					}
				}
			}
		}
		return mostDamagedShip;
	}

	public int getAimBonus(GameWorld gameWorld) {
		int totalAimBonus = 0;
		// get aimBonus from ship and vip
		totalAimBonus += getSpaceshipAimBonus(gameWorld);
		VIP aimBonusVip = getAimBonusVIP();
		if (aimBonusVip != null) {
			totalAimBonus += aimBonusVip.getAimBonus();
		}
		return totalAimBonus;
	}

	private int getTotalTargetingWeight(SpaceshipTargetingType targetingType, boolean screenOnly) {
		Logger.finer("called, targetingType: " + targetingType + " screenOnly: " + screenOnly);
		int totalWeight = 0;
		for (Iterator<TaskForceSpaceShip> iter = allShips.iterator(); iter.hasNext();) {
			Spaceship aShip = iter.next().getSpaceship();
			Logger.finest("in for-loop, ship: " + aShip.getName() + " totalWeight: " + totalWeight);
			if (screenOnly) {
				Logger.finer("Screen only!");
				if (!aShip.isScreened()) {
					totalWeight = totalWeight + targetingType.getTargetingWeight(aShip);
					Logger.finer("Ship not in screen - adding weight: " + targetingType.getTargetingWeight(aShip));
				}
			} else {
				totalWeight = totalWeight + targetingType.getTargetingWeight(aShip);
				Logger.finer("No screen exists - adding weight: " + targetingType.getTargetingWeight(aShip));
			}
		}
		Logger.finer("return totalWeight: " + totalWeight);
		return totalWeight;
	}

	private TaskForceSpaceShip getTargetedShip(SpaceshipTargetingType targetingType, int targetIndex, boolean screenOnly) {
		Logger.finer("called, targetingType: " + targetingType + " targetIndex: " + targetIndex + " screenOnly: "
				+ screenOnly);
		/*
		 * Spaceship targetShip = null; int indexCounter = 0; Spaceship currentSpaceship
		 * = allss.get(indexCounter); int weightCounter =
		 * targetingType.getTargetingWeight(currentSpaceship); indexCounter++;
		 * LoggingHandler.finest(this,g,
		 * "getTargetedShip","before while-loop, currentSpaceship: " +
		 * currentSpaceship.getName() + " weightCounter: " + weightCounter +
		 * " indexCounter: " + indexCounter); while ((indexCounter < allss.size()) &&
		 * (weightCounter < targetIndex)){ currentSpaceship = allss.get(indexCounter);
		 * weightCounter = weightCounter +
		 * targetingType.getTargetingWeight(currentSpaceship); indexCounter++;
		 * LoggingHandler.finest(this,g,
		 * "getTargetedShip","end of while-loop, currentSpaceship: " +
		 * currentSpaceship.getName() + " weightCounter: " + weightCounter +
		 * " indexCounter: " + indexCounter); } targetShip = currentSpaceship;
		 * LoggingHandler.finest(this,g,"getTargetedShip","return targetship: " +
		 * targetShip.getName());
		 */
		TaskForceSpaceShip targetShip = null;
		int indexCounter = 0;
		int weightCounter = 0;
		while (targetShip == null) {
			TaskForceSpaceShip currentSpaceship = allShips.get(indexCounter);
			Logger.finer("currentSpaceship (" + indexCounter + "): " + currentSpaceship.getSpaceship().getUniqueName() + " ("
					+ indexCounter + ")");
			if ((!screenOnly) | (!currentSpaceship.getSpaceship().isScreened())) { // if all ships, or if the ship is not screened
				Logger.finer("Ship can be hei");
				weightCounter = weightCounter + targetingType.getTargetingWeight(currentSpaceship.getSpaceship());
				Logger.finer("weightCounter: " + weightCounter + " targetIndex: " + targetIndex);
				if (weightCounter > targetIndex) {
					Logger.finer("currentSpaceship targeted: " + currentSpaceship.getSpaceship().getUniqueName());
					targetShip = currentSpaceship;
				}
			}
			indexCounter++;
			// LoggingHandler.finest(this,g,"getTargetedShip","end of while-loop,
			// currentSpaceship: " + currentSpaceship.getName() + " weightCounter: " +
			// weightCounter + " indexCounter: " + indexCounter);
		}
		// LoggingHandler.finest(this,g,"getTargetedShip","return targetship: " +
		// targetShip.getName());
		return targetShip;
	}

	private int getMultiplier(int base) {
		int tempRandom = Functions.getRandomInt(1, 20);
		if (tempRandom > 18) {
			tempRandom = getMultiplier(base + tempRandom);
		} else {
			tempRandom = tempRandom + base;
		}
		Logger.finest("base: " + base + " returns: " + tempRandom);
		return tempRandom;
	}

	public void addSpaceship(TaskForceSpaceShip ship) {
		allShips.add(ship);
	}

	/**
	 * används då det gäller att avgöra om en tf ska fly eller inte
	 * 
	 * @return
	 */
	public int getStrength(GameWorld gameWorld) {
		int total = 0;
		for (TaskForceSpaceShip taskForceSpaceShip : allShips) {
			Spaceship tmpss = taskForceSpaceShip.getSpaceship();
			total = total + (tmpss.getCurrentShields());
			total = total + (tmpss.getCurrentDc() / 2);
			total = total + (SpaceshipPureFunctions.getActualDamage(tmpss, gameWorld));
		}
		return total;
	}

	public void restoreShieldsAndCleanDestroyedAndRetreatedLists(GameWorld gameWorld) {
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i).getSpaceship();
			SpaceshipMutator.restoreShields(ss, gameWorld);
		}
		destroyedShips.clear();
		retreatedShips.clear();
	}

	public int getBombardment(GameWorld gameWorld) {
		int totalBombardment = 0;
		if (gameWorld.isCumulativeBombardment()) {
			totalBombardment = getCumulativeBombardment();
		} else {
			totalBombardment = getMaxBombardment();
		}
		if (totalBombardment > 0) {
			// check if bombardment VIP exist in fleet
			VIP bombVIP = allShips.stream().flatMap(ship -> ship.getVipOnShip().stream())
					.reduce((vip1, vip2) -> vip1.getBombardmentBonus() > vip2.getBombardmentBonus() ? vip1 : vip2).orElse(null);
					//TODO 2019-12-26 Kolla att detta fungerar.
					//galaxy.findHighestVIPbombardmentBonus(allShips);
			if (bombVIP != null) {
				totalBombardment += bombVIP.getBombardmentBonus();
			}
		}
		return totalBombardment;
	}

	private int getCumulativeBombardment() {
		int totalBombardment = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i).getSpaceship();
			totalBombardment = totalBombardment + ss.getBombardment();
		}
		return totalBombardment;
	}

	private int getMaxBombardment() {
		int maxBombardment = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i).getSpaceship();
			if (ss.getBombardment() > maxBombardment) {
				maxBombardment = ss.getBombardment();
			}
		}
		return maxBombardment;
	}

	public int getMaxPsychWarfare(GameWorld gameWorld) {
		int maxPsychWarfare = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i).getSpaceship();
			if (ss.getPsychWarfare() > maxPsychWarfare) {
				maxPsychWarfare = ss.getPsychWarfare();
			}
		}
		return maxPsychWarfare;
	}

	public List<TaskForceSpaceShip> getRetreatedShips() {
		return retreatedShips;
	}

	public List<TaskForceSpaceShip> getDestroyedShips() {
		return destroyedShips;
	}

	public boolean stopsRetreats() {
		boolean hasInterdictor = false;
		int i = 0;
		while ((i < allShips.size()) & (!hasInterdictor)) {
			Spaceship ss = allShips.get(i).getSpaceship();
			if (ss.isNoRetreat()) {
				hasInterdictor = true;
			} else {
				i++;
			}
		}
		return hasInterdictor;
	}

	public int getTotalInitBonus() {
		if (getPlayerName() != null) {
			Logger.finer(
					"getTotalInitBonus " + getPlayerName() + ": " + getInitBonus() + " " + getVIPInitiativeBonus());
		} else {
			Logger.finer("getTotalInitBonus neutral: " + getInitBonus() + " " + getVIPInitiativeBonus());
		}
		return getInitBonus() + getVIPInitiativeBonus();
	}

	public int getTotalInitDefence() {
		if (getPlayerName() != null) {
			Logger.finer(
					"getTotalInitDefence " + getPlayerName() + ": " + getInitBonus() + " " + getVIPInitiativeBonus());
		} else {
			Logger.finer("getTotalInitDefence neutral: " + getInitBonus() + " " + getVIPInitiativeBonus());
		}
		return getInitDefence() + getVIPInitDefence();
	}

	public int getInitBonus() {
		
		int initBonus = allShips.stream().map(TaskForceSpaceShip::getSpaceship)
				.filter(Spaceship::isInitSupport)
				.map(ship -> SpaceshipPureFunctions.getIncreaseInitiative(ship))
				.reduce(Integer::max).orElse(0);
		int initSupportBonus = allShips.stream().map(TaskForceSpaceShip::getSpaceship)
				.filter(ship -> !ship.isInitSupport())
				.map(ship -> SpaceshipPureFunctions.getIncreaseInitiative(ship))
				.reduce(Integer::max).orElse(0);
		/* TODO testa av.
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			if (ss.getInitSupport()) {
				if (ss.getIncreaseInitiative() > initSupportBonus) {
					initSupportBonus = ss.getIncreaseInitiative();
				}
			} else {
				if (ss.getIncreaseInitiative() > initBonus) {
					initBonus = ss.getIncreaseInitiative();
				}
			}
		}*/
		
		return initBonus + initSupportBonus;
	}

	public int getInitDefence() {
		return allShips.stream().map(ship -> ship.getSpaceship().getInitDefence()).reduce(Integer::max).get();
		/*TODO testa av för att sedan ta bort.
		int initDefence = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			if (ss.getInitDefence() > initDefence) {
				initDefence = ss.getInitDefence();
			}
		}
		return initDefence;
		*/
	}

	// increases the chance to fire against the most damage ship.
	public int getSpaceshipAimBonus(GameWorld gameWorld) {
		return allShips.stream().map(ship -> SpaceshipPureFunctions.getSpaceshipTypeByKey(ship.getSpaceship().getTypeKey(), gameWorld).getAimBonus()).reduce(Integer::max).get();
		/*TODO testa av för att sedan ta bort.
		int tmpAimBonus = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			if (ss.getAimBonus() > tmpAimBonus) {
				tmpAimBonus = ss.getAimBonus();
			}
		}
		return tmpAimBonus;
		*/
	}

	public int getVIPInitiativeBonus() {
		
		VIP InitBonusVip = allShips.stream().filter(ship -> !ship.getSpaceship().isSquadron()).flatMap(ship -> ship.getVipOnShip().stream())
		.reduce((vip1, vip2) -> vip1.getInitBonus() > vip2.getInitBonus() ? vip1 : vip2).orElse(null);
		
		int initBonusCapitalShip = InitBonusVip == null ? 0 : InitBonusVip.getInitBonus();
		
		/*
		int initBonusCapitalShip = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			if (!ss.isSquadron()) {
				int tmpInitBonusCapitalShip = galaxy.findVIPhighestInitBonusCapitalShip(ss, player);
				if (tmpInitBonusCapitalShip > initBonusCapitalShip) {
					initBonusCapitalShip = tmpInitBonusCapitalShip;
				}
			}
		}*/
		
		VIP initBonusSquadronVip = allShips.stream().filter(ship -> ship.getSpaceship().isSquadron())
				.filter(ship -> !isScreened(ship.getSpaceship())).flatMap(ship -> ship.getVipOnShip().stream())
				.reduce((vip1, vip2) -> vip1.getInitSquadronBonus() > vip2.getInitSquadronBonus() ? vip1 : vip2).orElse(null);
		
		int initBonusSquadron = initBonusSquadronVip == null ? 0 : initBonusSquadronVip.getInitSquadronBonus();
		
		/*
		int initBonusSquadron = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			// LoggingHandler.finer(ss.getName(),g);
			if (ss.isSquadron()) {
				// LoggingHandler.finer("is squadron",g);
				if ((getTotalNrShips(true) > 0) & (getTotalNrShips(false) > 0)) {
					// LoggingHandler.finer("screened ships exist!",g);
					if (!ss.getScreened()) { // screened starfighters don't give bonuses for vips
						int tmpInitBonusSquadron = galaxy.findVIPhighestInitBonusSquadron(ss, player);
						if (tmpInitBonusSquadron > initBonusSquadron) {
							initBonusSquadron = tmpInitBonusSquadron;
						}
					}
				} else {
					// LoggingHandler.finer("no screened ships",g);
					// no screened ships, all ships may have a valid vip
					int tmpInitBonusSquadron = galaxy.findVIPhighestInitBonusSquadron(ss, player);
					// LoggingHandler.finer(String.valueOf(tmpInitBonusSquadron),g);
					if (tmpInitBonusSquadron > initBonusSquadron) {
						initBonusSquadron = tmpInitBonusSquadron;
					}
				}
			}
		}*/
		Logger.finer(
				"Taskforce.getVIPInitiativeBonus() returning: " + initBonusCapitalShip + " + " + initBonusSquadron);
		return initBonusCapitalShip + initBonusSquadron;
	}
	
	private boolean isScreened(Spaceship ship) {
		return !((onlyFirstLine() && !ship.isScreened()) || !(onlyFirstLine()));
	}

	public int getVIPInitDefence() {
		VIP vip = allShips.stream().flatMap(ship -> ship.getVipOnShip().stream())
				.reduce((vip1, vip2) -> vip1.getInitDefence() > vip2.getInitDefence() ? vip1 : vip2).orElse(null);
		return vip == null ?  0 : vip.getInitDefence();
		//TODO 2019-12-26 Verifiera att detta fungerar som det är tänkt.
		/*
		int initDefence = 0;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			int tmpInitDefence = galaxy.findVIPhighestInitDefence(ss, player);
			if (tmpInitDefence > initDefence) {
				initDefence = tmpInitDefence;
			}
		}
		return initDefence;*/
	}

	/*
	 * public VIP getSiegeBonusVIP(){ VIP highestSiegeVIP = null; for (int i = 0;i <
	 * allss.size();i++){ Spaceship ss =allss.get(i); VIP aVIP =
	 * g.findHighestVIPSiegeBonus(ss,player); if (aVIP != null){ if (highestSiegeVIP
	 * == null){ highestSiegeVIP = aVIP; }else if (aVIP.getSiegeBonus() >
	 * highestSiegeVIP.getSiegeBonus()){ highestSiegeVIP = aVIP; } } } return
	 * highestSiegeVIP; }
	 */
	
	// increases the chance to fire against the most damage ship.
	public VIP getAimBonusVIP() {
		return allShips.stream().flatMap(ship -> ship.getVipOnShip().stream())
				.reduce((vip1, vip2) -> vip1.getAimBonus() > vip2.getAimBonus() ? vip1 : vip2).orElse(null);
		//TODO 2019-12-26 Verifiera att detta fungerar som det är tänkt.
		/*
		VIP highestAimVIP = null;
		for (int i = 0; i < allShips.size(); i++) {
			Spaceship ss = allShips.get(i);
			VIP aVIP = galaxy.findHighestVIPAimBonus(ss, player);
			if (aVIP != null) {
				if (highestAimVIP == null) {
					highestAimVIP = aVIP;
				} else if (aVIP.getAimBonus() > highestAimVIP.getAimBonus()) {
					highestAimVIP = aVIP;
				}
			}
		}
		return highestAimVIP;*/
	}

	public int getTotalCostSupply() {
		//TODO 2019-12-26 Verifiera att detta fungerar som det är tänkt.
		return allShips.stream().map(ship -> ship.getSpaceship().getUpkeep()).reduce((upKeep1, upKeep2) -> upKeep1 + upKeep2).orElse(0);
		/*
		int tmpSupply = 0;
		for (Iterator<Spaceship> iter = allShips.iterator(); iter.hasNext();) {
			Spaceship aShip = (Spaceship) iter.next();
			tmpSupply = tmpSupply + aShip.getUpkeep();
		}
		return tmpSupply;
		*/
	}

	public int getTotalCostBuy() {
		//TODO 2019-12-26 Verifiera att detta fungerar som det är tänkt.
		return allShips.stream().map(ship -> ship.getSpaceship().getBuildCost()).reduce((cost1, cost2) -> cost1 + cost2).orElse(0);
		/*
		int tmpBuy = 0;
		for (Iterator<Spaceship> iter = allShips.iterator(); iter.hasNext();) {
			Spaceship aShip = (Spaceship) iter.next();
			tmpBuy = tmpBuy + aShip.getSpaceshipType().getBuildCost(null);
		}
		return tmpBuy;
		*/
	}

	/**
	 * Returns true if at least one ship in this fleet can besiege
	 * 
	 * @return true if at least one ship in this fleet can besiege
	 */
	public boolean canBesiege() {
		// allShips should not contains any destroyed ships.
		return allShips.stream().anyMatch(ship -> ship.getSpaceship().isCanBlockPlanet() && !ship.getSpaceship().isDestroyed());
	}

	public void removeDestroyedShips() {
		if (allShips != null) {
			List<TaskForceSpaceShip> removeShips = new LinkedList<>();
			for (Iterator<TaskForceSpaceShip> iter = allShips.iterator(); iter.hasNext();) {
				TaskForceSpaceShip aShip =  iter.next();
				if (aShip.getSpaceship().isDestroyed()) {
					destroyedShips.add(aShip);
					removeShips.add(aShip);
				}
			}
			allShips.removeAll(removeShips);
		}
	}

	//TODO 2019-12-26 Ska den här metoden ligga i TaskForce? behöver vi en taskForce för detta endamål?
	public void incomingCannonFire(Planet aPlanet, Building aBuilding, Galaxy galaxy) {
		if (allShips != null) {
			List<TaskForceSpaceShip> shipsPossibleToHit = allShips.stream().filter(ship -> SpaceshipPureFunctions.getSpaceshipTypeByKey(ship.getSpaceship().getTypeKey(), galaxy.getGameWorld()).isCapitalShip()).collect(Collectors.toList());
			
			Logger.finer("shipsPossibleToHit.size(): " + shipsPossibleToHit.size());
			int randomIndex = Functions.getRandomInt(0, shipsPossibleToHit.size() - 1);
			TaskForceSpaceShip shipToBeHit = shipsPossibleToHit.get(randomIndex);

			// perform shot
			int multiplier = getMultiplier(0);
			Logger.finest("multiplier: " + multiplier);

			// randomize damage
			int damageNoArmor = (int) Math.round(aBuilding.getBuildingType().getCannonDamage() * (multiplier / 10.0));
			if (damageNoArmor < 1) {
				damageNoArmor = 1;
			}
			// Use totalDamage to show the damage after armor.
			int totalDamage = shipToBeHit.getSpaceship().getCurrentShields();
			int damageLeftAfterShields = shipToBeHit.getSpaceship().shipShieldsHit(damageNoArmor);
			double afterShieldsDamageRatio = (damageLeftAfterShields * 1.0d) / damageNoArmor;
			Logger.finer("afterShieldsDamageRatio: " + afterShieldsDamageRatio);
			// gör en sådan funktion och plocka ut skadan. är bara small skada som kanonen
			// gör.
			int actualDamage = getActualDamage(shipToBeHit.getSpaceship(), multiplier, afterShieldsDamageRatio, aBuilding);
			// Anväda denna funktion. o skicka i skadan. damageLeftAfterShields är skadan
			// kvar efter att skölden har tagit första smällen. är alltså 0 om skölde
			// klarade av hela skadan. om damageLeftAfterShields är = 0 ss skall
			// damageNoArmor dras av skölden. annars stts skölden till 0 och actualDamage
			// dras av hullet.
			String damagedStatus = shipHit(shipToBeHit.getSpaceship(), actualDamage, damageLeftAfterShields, damageNoArmor, galaxy, galaxy.getGameWorld());
			totalDamage += actualDamage;
			Logger.finer("multiplier=" + multiplier + " damageNoArmor=" + damageNoArmor + " damageLeftAfterShields="
					+ damageLeftAfterShields + " afterShieldsDamageRatio=" + afterShieldsDamageRatio + " actualDamage="
					+ actualDamage + " damagedStatus=" + damagedStatus);
			if (shipToBeHit.getSpaceship().isDestroyed()) {
				galaxy.getPlayerByGovenorName(getPlayerName()).addToGeneral(
						"Your ship " + shipToBeHit.getSpaceship().getName() + " on " + aPlanet.getName() + " was destroyed when hit ("
								+ damageNoArmor + ") by an enemy " + aBuilding.getBuildingType().getName() + ".");
				if (aPlanet.getPlayerInControl() != null) {
					aPlanet.getPlayerInControl()
							.addToGeneral("Your " + aBuilding.getBuildingType().getName() + " at " + aPlanet.getName()
									+ "hit (" + damageNoArmor + ") and destroyed an enemy "
									+ SpaceshipPureFunctions.getSpaceshipTypeByKey(shipToBeHit.getSpaceship().getTypeKey(), galaxy.getGameWorld()).getName() + ".");
					// är detta rätt? ser skumt ut
				}
				// check for destroyed squadrons in the carrier hit
				List<TaskForceSpaceShip> squadronsDestroyed = new LinkedList<>();
				if(SpaceshipPureFunctions.isCarrier(shipToBeHit.getSpaceship(), galaxy.getGameWorld())) {
					for (TaskForceSpaceShip aShip : allShips) {
						Logger.finer("sqd loc: " + aShip.getSpaceship().getCarrierLocation());
						if (aShip.getSpaceship().isSquadron() & (aShip.getSpaceship().getCarrierLocation() == shipToBeHit.getSpaceship())) { // squadron in a destroyed
																								// carrier
							squadronsDestroyed.add(aShip);
						}
					}
				}
				// remove ship
				SpaceshipMutator.removeShip(shipToBeHit.getSpaceship(), galaxy);
				allShips.remove(shipToBeHit);
				destroyedShips.add(shipToBeHit);
				for (TaskForceSpaceShip aSquadron : squadronsDestroyed) {
					Logger.finer("sqd destroyed!");
					SpaceshipMutator.removeShip(aSquadron.getSpaceship(), galaxy);
					allShips.remove(aSquadron);
					destroyedShips.add(aSquadron);
					galaxy.getPlayerByGovenorName(getPlayerName()).addToGeneral(
							"Your squadron " + aSquadron.getSpaceship().getName() + " carried inside " + shipToBeHit.getSpaceship().getName()
									+ " was also destroyed when " + shipToBeHit.getSpaceship().getName() + " was lost.");
					//TODO 2019-12-26 Se till att detta visas korrekt i klienten.
					//addToLatestLostInSpace(aSquadron);
					// squadrons destroyed are not added to planets owners lostInSpace or
					// addToGeneral, he don't know if a carrier carried any squadrons
				}
				Logger.finer("allss.size(): " + allShips.size());
				// is all ships in the tf destroyed?
				if (allShips.size() == 0) {
					isDestroyed = true;
				}
			} else {
				galaxy.getPlayerByGovenorName(getPlayerName()).addToGeneral("Your ship " + shipToBeHit.getSpaceship().getName() + " on " + aPlanet.getName()
						+ " was hit by an enemy " + aBuilding.getBuildingType().getName() + " and the damage ("
						+ damageNoArmor + ") " + damagedStatus + ".");
				if (aPlanet.getPlayerInControl() != null) {
					aPlanet.getPlayerInControl()
							.addToGeneral("Your " + aBuilding.getBuildingType().getName() + " at " + aPlanet.getName()
									+ " hit an enemy " + SpaceshipPureFunctions.getSpaceshipTypeByKey(shipToBeHit.getSpaceship().getTypeKey(), galaxy.getGameWorld()).getName() + " and the damage ("
									+ damageNoArmor + ") " + damagedStatus + ".");
				}
			}
		}
	}

	public int getActualDamage(Spaceship targetShip, int multiplier, double shieldsMultiplier, Building aBuilding) {
		double tmpDamage = 0;

		tmpDamage = aBuilding.getBuildingType().getCannonDamage() * (1.0 - targetShip.getArmorSmall());

		Logger.finer("Damage before shieldsmodifier: " + tmpDamage);
		tmpDamage = tmpDamage * shieldsMultiplier;
		Logger.finer("Damage after shieldsmodifier: " + tmpDamage);
		// double baseDamage = tmpDamage * ((targetShip.getCurrentDc() * 1.0) /
		// targetShip.getDamageCapacity());
		double baseDamage = tmpDamage; // Paul: tar bort raden ovan, g�r att skepp tar mindre skada om de �r skadade
		Logger.finer("Damage after hull damage effect: " + baseDamage);
		// randomize damage
		int actualDamage = (int) Math.round(baseDamage * (multiplier / 10.0));
		Logger.finest("Damage after multiplier: " + actualDamage + " ship hit: " + targetShip.getName()
				+ " firing Building (cannon): " + aBuilding.getBuildingType().getName());
		if (actualDamage < 1) {
			actualDamage = 1;
		}
		return actualDamage;
	}

	public int getTroopCapacity() {
		int capacity = 0;
		for (TaskForceSpaceShip ship : allShips) {
			capacity += ship.getSpaceship().getTroopCapacity();
		}
		return capacity;
	}

	public List<TaskForceSpaceShip> getAllSpaceShips() {
		return allShips;
	}

	public void setDestroyed() {
		isDestroyed = true;
	}
	
	public void addClosestFriendlyPlanets(SpaceshipRange spaceshipRange, List<Planet> planets) {
		closestFriendlyPlanets.put(spaceshipRange, planets);
	}
	
	public List<Planet> getClosestFriendlyPlanets(SpaceshipRange spaceshipRange){
		return closestFriendlyPlanets.getOrDefault(spaceshipRange, new ArrayList<Planet>());
	}
	
	public static Planet getRandomClosestPlanet(TaskForce taskForce, SpaceshipRange aSpaceshipRange) {
    	return taskForce.getClosestFriendlyPlanets(aSpaceshipRange).isEmpty() ? null : 
    		taskForce.getClosestFriendlyPlanets(aSpaceshipRange).get(new Random().nextInt(taskForce.getClosestFriendlyPlanets(aSpaceshipRange).size()));
    }
	
	private boolean canAttackScreened(TaskForceSpaceShip taskForceSpaceShip) {
		return taskForceSpaceShip.getSpaceship().isCanAttackScreenedShips() 
				|| taskForceSpaceShip.getVipOnShip().stream().anyMatch(vip -> vipAllowShipToAttackScreened(vip, taskForceSpaceShip.getSpaceship()));
	}
	
	private boolean vipAllowShipToAttackScreened(VIP vip, Spaceship ship) {
		return (ship.isSquadron() && vip.isAttackScreenedSquadron()) || (!ship.isSquadron() && vip.isAttackScreenedCapital());
	}
	
	/**
	 * Checks if taskaForce have a "active" first line.
	 * "active" means the taskForce have at least one ship that is not screened and at least one screened ship 
	 * 
	 */
	public boolean onlyFirstLine() {
		return getTotalNrShips(false) > 0 && getTotalNrShips(true) > 0;
	}

	public SpaceBattleReport getSpaceBattleReport() {
		return spaceBattleReport;
	}

	public void setSpaceBattleReport(SpaceBattleReport spaceBattleReport) {
		this.spaceBattleReport = spaceBattleReport;
	}

	public String getPlayerName() {
		return playerName;
	}

	public String getFactionName() {
		return factionName;
	}


	// returnera sträng om skottets effekt, förstört, skadat, togs upp av
	// sköldarna
	public String shipHit(Spaceship spaceship, int damage, int damageAfterShields, int damageNoArmor, Galaxy galaxy, GameWorld gameWorld) {
		String returnString = "";
		Logger.finer( "shipHit currentShields "
				+ spaceship.getCurrentShields() + " damage " + damage + " currentdc "
				+ spaceship.getCurrentDc() + " damageAfterShields: " + damageAfterShields
				+ " damageNoArmor: " + damageNoArmor);
		// if (currentshields > damage){
		if (damageAfterShields == 0) { // all damage was absorbed by the shields
			spaceship.setCurrentShields(spaceship.getCurrentShields() - damageNoArmor);
			int shieldStrength = (int) Math.round((100.0 * spaceship.getCurrentShields()) / SpaceshipPureFunctions.getShields(spaceship, gameWorld));
			returnString = "was absorbed by the shields (shield strength: " + String.valueOf(shieldStrength) + "%).";
		} else {
			// damage = damage - currentshields;
			spaceship.setCurrentShields(0);
			if (spaceship.getCurrentDc() > damage) {
				spaceship.setCurrentDc(spaceship.getCurrentDc() - damage);
				int hullStrength = (int) Math.round((100.0 * spaceship.getCurrentDc())	/ spaceship.getDamageCapacity());
				returnString = "damaged the ship (hull strength:" + String.valueOf(hullStrength) + "%).";
			} else {
				spaceship.setCurrentDc(0);
				if (spaceship.getOwner() != null) {
					VipMutator.checkVIPsInDestroyedShips(spaceship, spaceship.getOwner(), galaxy);
					galaxy.checkTroopsInDestroyedShips(spaceship, spaceship.getOwner());
				}
			}
		}
		return returnString;
	}
	
}
