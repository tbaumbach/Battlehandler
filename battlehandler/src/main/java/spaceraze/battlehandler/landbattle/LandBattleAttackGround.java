package spaceraze.battlehandler.landbattle;

import java.util.List;

import spaceraze.servlethelper.game.troop.TroopMutator;
import spaceraze.servlethelper.game.troop.TroopPureFunctions;
import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.GameWorld;
import spaceraze.world.Troop;
import spaceraze.world.enums.LandBattleAttackType;
import spaceraze.world.report.landbattle.*;

public class LandBattleAttackGround extends LandBattleAttack {
	private TaskForceTroop attacker;
	private boolean defender; // true if troop is on their own planet

	public LandBattleAttackGround(LandBattleAttackType attackType, TaskForceTroop anAttacker, List<TaskForceTroop> aTargetOpponents, boolean defending, int aResistance){
		super(attackType,aTargetOpponents,aResistance);
		attacker = anAttacker;
		this.defender = defending;
	}
 
	@Override
	public void performAttack(LandBattleGroup attBG, LandBattleGroup defBG, int attVipBonus, int defVipBonus, GameWorld gameWorld) {
		Logger.finer("performAttack(ground)");
		if (TroopPureFunctions.isDestroyed(attacker.getTroop())){
			Logger.finer("Attacker already destroyed");
		}else{
			TaskForceTroop targetTroop = getRandomOpponent();
			if (targetTroop == null){
				Logger.finer("All opponents destroyed");
			}else{
				Logger.finer("targetTroop: " + targetTroop.getTroop().getName());
				int attMultiplier = Functions.getRandomInt(1, 20);
				Logger.finer("attMultiplier: " + attMultiplier);
				int defMultiplier = 20-attMultiplier-TroopPureFunctions.getTroopTypeByKey(targetTroop.getTroop().getTypeKey(), gameWorld).getFiringBackPenalty();
				if (defMultiplier < 0){
					defMultiplier = 0;
				}
				Logger.finer("defMultiplier: " + defMultiplier);
				
				
				int attVIPBonus = attVipBonus;
				int defVIPBonus = defVipBonus;
				if(defender){
					attVIPBonus = defVipBonus;
					defVIPBonus = attVipBonus;
				}
				
				int attackerActualDamage = getActualDamage(attacker.getTroop(), TroopPureFunctions.getTroopTypeByKey(targetTroop.getTroop().getTypeKey(), gameWorld).isArmor(), attMultiplier, defender, resistance, attVIPBonus);
				Logger.finer("attackerActualDamage: " + attackerActualDamage);
				int defenderActualDamage = getActualDamage(targetTroop.getTroop(), TroopPureFunctions.getTroopTypeByKey(attacker.getTroop().getTypeKey(), gameWorld).isArmor(), defMultiplier, !defender, resistance, defVIPBonus);
				Logger.finer("defenderActualDamage: " + defenderActualDamage);
				String result1 = TroopMutator.hit(targetTroop.getTroop(), attackerActualDamage, false, !defender, resistance);
				Logger.finer(targetTroop.getTroop().getName() + ": " + result1);
				if (TroopPureFunctions.isDestroyed(targetTroop.getTroop())){
					TroopMutator.addKill(attacker.getTroop());
					//TODO 2020-01-05 Moved to GalaxyUpdater, the troop will be removed from galaxy after the fight is done. Borde inte den döda truppen ha lagt till i LandBattleReport.postBattleSurvivingOwnTroops eller LandBattleReport.postBattleSurvivingEnemyTroops?
					//g.removeTroop(targetTroop);
					//TODO 2020-01-26 Detta måste lösas, borde gå att läsa ut döda från rapporterna i stället.
					//attacker.getTroop().addToLatestTroopsLostInSpace(targetTroop.getTroop());
					//targetTroop.getTroop().addToLatestTroopsLostInSpace(targetTroop.getTroop());
				}
				String result2 = TroopMutator.hit(attacker.getTroop(), defenderActualDamage, false, defender, resistance);
				Logger.finer(attacker.getTroop().getName() + ": " + result2);
				if (TroopPureFunctions.isDestroyed(attacker.getTroop())){
					TroopMutator.addKill(targetTroop.getTroop());
					//TODO 2020-01-05 Moved to GalaxyUpdater, the troop will be removed from galaxy after the fight is done. Borde inte den döda truppen ha lagt till i LandBattleReport.postBattleSurvivingOwnTroops eller LandBattleReport.postBattleSurvivingEnemyTroops?
					//g.removeTroop(attacker);
					
					//TODO 2020-01-26 Detta måste lösas, borde gå att läsa ut döda från rapporterna i stället.
					//attacker.getTroop().addToLatestTroopsLostInSpace(attacker.getTroop());
					//targetTroop.getTroop().addToLatestTroopsLostInSpace(attacker.getTroop());
				}
				
				if(defender) {
					if(attBG.getReport() != null) {
						attBG.getReport().getLandBattleAttacks().add(createLandBattleAttackForDefendingTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false, gameWorld));
					}
					if(defBG.getReport() != null) {
						defBG.getReport().getLandBattleAttacks().add(getLandBattleAttackForAttackTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false, gameWorld));
					}
				}else {
					if(attBG.getReport() != null) {
						attBG.getReport().getLandBattleAttacks().add(getLandBattleAttackForAttackTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false, gameWorld));
					}
					if(defBG.getReport() != null) {
						defBG.getReport().getLandBattleAttacks().add(createLandBattleAttackForDefendingTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false, gameWorld));
					}
				}
				
				
				//attReport.addAttackResultGround(attacker.getTroop(), targetTroop.getTroop(), attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier, !defender);
				//defReport.addAttackResultGround(attacker.getTroop(), targetTroop.getTroop(), attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier, defender);
			}
		}
	}

	private static int getActualDamage(Troop troop, boolean armoredTarget, int multiplier, boolean defender, int resistance, int vipBonus){
		int baseDamage = 0;
		if (armoredTarget){
			baseDamage = TroopPureFunctions.getAttackArmored(troop);
		}else{
			baseDamage = TroopPureFunctions.getAttackInfantry(troop);
		}
		Logger.finer("baseDamage: " + baseDamage + " Mult=" + multiplier);
		return TroopPureFunctions.getModifiedActualDamage(troop, baseDamage, multiplier, defender, resistance, vipBonus);
	}

	public static spaceraze.world.report.landbattle.LandBattleAttack getLandBattleAttackForAttackTroop(TaskForceTroop attacker, TaskForceTroop targetTroop, int attMultiplier, int defMultiplier, int attackerActualDamage, int defenderActualDamage, boolean isArtillery, GameWorld gameWorld) {
		return spaceraze.world.report.landbattle.LandBattleAttack.builder()
				.troopAttack(TroopAttack.builder()
						.name(attacker.getTroop().getName())
						.typeName(TroopPureFunctions.getTroopTypeByKey(attacker.getTroop().getTypeKey(), gameWorld).getName())
						.damageCapacity(attacker.getTroop().getDamageCapacity())
						.currentDamageCapacity(attacker.getTroop().getCurrentDamageCapacity())
						.artillery(isArtillery)
						.own(true)
						.build())
				.troopTarget(TroopTarget.builder()
						.typeName(TroopPureFunctions.getTroopTypeByKey(targetTroop.getTroop().getTypeKey(), gameWorld).getName())
						.damageCapacity(targetTroop.getTroop().getDamageCapacity())
						.currentDamageCapacity(targetTroop.getTroop().getCurrentDamageCapacity())
						.own(false).build())
				.damage(attackerActualDamage)
				.counterDamage(defenderActualDamage)
				.attMultiplier(attMultiplier)
				.counterMultiplier(defMultiplier)
				.build();
	}

	public static spaceraze.world.report.landbattle.LandBattleAttack createLandBattleAttackForDefendingTroop(TaskForceTroop attacker, TaskForceTroop targetTroop, int attMultiplier, int defMultiplier, int attackerActualDamage, int defenderActualDamage, boolean isArtillery, GameWorld gameWorld) {
		return spaceraze.world.report.landbattle.LandBattleAttack.builder()
				.troopAttack(TroopAttack.builder()
						.typeName(TroopPureFunctions.getTroopTypeByKey(attacker.getTroop().getTypeKey(), gameWorld).getName())
						.damageCapacity(attacker.getTroop().getDamageCapacity())
						.currentDamageCapacity(attacker.getTroop().getCurrentDamageCapacity())
						.artillery(isArtillery)
						.own(false)
						.build())
				.troopTarget(TroopTarget.builder()
						.name(targetTroop.getTroop().getName())
						.typeName(TroopPureFunctions.getTroopTypeByKey(targetTroop.getTroop().getTypeKey(), gameWorld).getName())
						.damageCapacity(targetTroop.getTroop().getDamageCapacity())
						.currentDamageCapacity(targetTroop.getTroop().getCurrentDamageCapacity())
						.own(true).build())
				.damage(attackerActualDamage)
				.counterDamage(defenderActualDamage)
				.attMultiplier(attMultiplier)
				.counterMultiplier(defMultiplier)
				.build();
	}

	@Override
	public String toString(){
		String retStr = "LBAG:";
		retStr = retStr + "att=" + attacker.getTroop().getShortName() + " ";
		retStr = retStr + "def=" + defender + " ";
		retStr = retStr + getAsString();
		return retStr;
	}

}
