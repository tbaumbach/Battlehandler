package spaceraze.battlehandler.landbattle;

import java.util.List;

import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
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
	public void performAttack(LandBattleGroup attBG, LandBattleGroup defBG, int attVipBonus, int defVipBonus) {
		Logger.finer("performAttack(ground)");
		if (attacker.getTroop().isDestroyed()){
			Logger.finer("Attacker already destroyed");
		}else{
			TaskForceTroop targetTroop = getRandomOpponent();
			if (targetTroop == null){
				Logger.finer("All opponents destroyed");
			}else{
				Logger.finer("targetTroop: " + targetTroop.getTroop().getUniqueName());
				int attMultiplier = Functions.getRandomInt(1, 20);
				Logger.finer("attMultiplier: " + attMultiplier);
				int defMultiplier = 20-attMultiplier-targetTroop.getTroop().getFiringBackPenalty();
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
				
				int attackerActualDamage = attacker.getTroop().getActualDamage(targetTroop.getTroop().isArmor(),attMultiplier,defender,resistance, attVIPBonus);
				Logger.finer("attackerActualDamage: " + attackerActualDamage);
				int defenderActualDamage = targetTroop.getTroop().getActualDamage(attacker.getTroop().isArmor(),defMultiplier,!defender,resistance, defVIPBonus);
				Logger.finer("defenderActualDamage: " + defenderActualDamage);
				String result1 = targetTroop.getTroop().hit(attackerActualDamage, false, !defender, resistance);
				Logger.finer(targetTroop.getTroop().getUniqueName() + ": " + result1);
				if (targetTroop.getTroop().isDestroyed()){
					attacker.getTroop().addKill();
					//TODO 2020-01-05 Moved to GalaxyUpdater, the troop will be removed from galaxy after the fight is done. Borde inte den döda truppen ha lagt till i LandBattleReport.postBattleSurvivingOwnTroops eller LandBattleReport.postBattleSurvivingEnemyTroops?
					//g.removeTroop(targetTroop);
					//TODO 2020-01-26 Detta måste lösas, borde gå att läsa ut döda från rapporterna i stället.
					//attacker.getTroop().addToLatestTroopsLostInSpace(targetTroop.getTroop());
					//targetTroop.getTroop().addToLatestTroopsLostInSpace(targetTroop.getTroop());
				}
				String result2 = attacker.getTroop().hit(defenderActualDamage, false, defender, resistance);
				Logger.finer(attacker.getTroop().getUniqueName() + ": " + result2);
				if (attacker.getTroop().isDestroyed()){
					targetTroop.getTroop().addKill();
					//TODO 2020-01-05 Moved to GalaxyUpdater, the troop will be removed from galaxy after the fight is done. Borde inte den döda truppen ha lagt till i LandBattleReport.postBattleSurvivingOwnTroops eller LandBattleReport.postBattleSurvivingEnemyTroops?
					//g.removeTroop(attacker);
					
					//TODO 2020-01-26 Detta måste lösas, borde gå att läsa ut döda från rapporterna i stället.
					//attacker.getTroop().addToLatestTroopsLostInSpace(attacker.getTroop());
					//targetTroop.getTroop().addToLatestTroopsLostInSpace(attacker.getTroop());
				}
				
				if(defender) {
					if(attBG.getReport() != null) {
						attBG.getReport().getLandBattleAttacks().add(createLandBattleAttackForDefendingTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false));
					}
					if(defBG.getReport() != null) {
						defBG.getReport().getLandBattleAttacks().add(getLandBattleAttackForAttackTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false));
					}
				}else {
					if(attBG.getReport() != null) {
						attBG.getReport().getLandBattleAttacks().add(getLandBattleAttackForAttackTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false));
					}
					if(defBG.getReport() != null) {
						defBG.getReport().getLandBattleAttacks().add(createLandBattleAttackForDefendingTroop(attacker, targetTroop, attMultiplier, defMultiplier, attackerActualDamage, defenderActualDamage, false));
					}
				}
				
				
				//attReport.addAttackResultGround(attacker.getTroop(), targetTroop.getTroop(), attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier, !defender);
				//defReport.addAttackResultGround(attacker.getTroop(), targetTroop.getTroop(), attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier, defender);
			}
		}
	}

	public static spaceraze.world.report.landbattle.LandBattleAttack getLandBattleAttackForAttackTroop(TaskForceTroop attacker, TaskForceTroop targetTroop, int attMultiplier, int defMultiplier, int attackerActualDamage, int defenderActualDamage, boolean isArtillery) {
		return spaceraze.world.report.landbattle.LandBattleAttack.builder()
				.troopAttack(TroopAttack.builder()
						.name(attacker.getTroop().getUniqueName())
						.typeName(attacker.getTroop().getTroopType().getUniqueName())
						.damageCapacity(attacker.getTroop().getMaxDC())
						.currentDamageCapacity(attacker.getTroop().getCurrentDC())
						.artillery(isArtillery)
						.own(true)
						.build())
				.troopTarget(TroopTarget.builder()
						.typeName(targetTroop.getTroop().getTroopType().getUniqueName())
						.damageCapacity(targetTroop.getTroop().getMaxDC())
						.currentDamageCapacity(targetTroop.getTroop().getCurrentDC())
						.own(false).build())
				.damage(attackerActualDamage)
				.counterDamage(defenderActualDamage)
				.attMultiplier(attMultiplier)
				.counterMultiplier(defMultiplier)
				.build();
	}

	public static spaceraze.world.report.landbattle.LandBattleAttack createLandBattleAttackForDefendingTroop(TaskForceTroop attacker, TaskForceTroop targetTroop, int attMultiplier, int defMultiplier, int attackerActualDamage, int defenderActualDamage, boolean isArtillery) {
		return spaceraze.world.report.landbattle.LandBattleAttack.builder()
				.troopAttack(TroopAttack.builder()
						.typeName(attacker.getTroop().getTroopType().getUniqueName())
						.damageCapacity(attacker.getTroop().getMaxDC())
						.currentDamageCapacity(attacker.getTroop().getCurrentDC())
						.artillery(isArtillery)
						.own(false)
						.build())
				.troopTarget(TroopTarget.builder()
						.name(targetTroop.getTroop().getUniqueName())
						.typeName(targetTroop.getTroop().getTroopType().getUniqueName())
						.damageCapacity(targetTroop.getTroop().getMaxDC())
						.currentDamageCapacity(targetTroop.getTroop().getCurrentDC())
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
		retStr = retStr + "att=" + attacker.getTroop().getUniqueShortName() + " ";
		retStr = retStr + "def=" + defender + " ";
		retStr = retStr + getAsString();
		return retStr;
	}

}
