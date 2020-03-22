package spaceraze.battlehandler.landbattle;

import java.util.List;

import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.enums.LandBattleAttackType;
import spaceraze.world.report.landbattle.EnemyTroopAttack;
import spaceraze.world.report.landbattle.EnemyTroopTarget;
import spaceraze.world.report.landbattle.OwnTroopAttack;
import spaceraze.world.report.landbattle.OwnTroopTarget;

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
						attBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
								new EnemyTroopAttack(attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), false),
								new OwnTroopTarget(targetTroop.getTroop().getUniqueName(), targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
								attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier));
					}
					if(defBG.getReport() != null) {
						defBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
								new OwnTroopAttack(attacker.getTroop().getUniqueName(), attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), false),
								new EnemyTroopTarget(targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
								attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier));
					}
				}else {
					if(attBG.getReport() != null) {
						attBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
								new OwnTroopAttack(attacker.getTroop().getUniqueName(), attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), false),
								new EnemyTroopTarget(targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
								attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier));
					}
					if(defBG.getReport() != null) {
						defBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
								new EnemyTroopAttack(attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), false),
								new OwnTroopTarget(targetTroop.getTroop().getUniqueName(), targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
								attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier));
					}
				}
				
				
				//attReport.addAttackResultGround(attacker.getTroop(), targetTroop.getTroop(), attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier, !defender);
				//defReport.addAttackResultGround(attacker.getTroop(), targetTroop.getTroop(), attackerActualDamage, defenderActualDamage, attMultiplier, defMultiplier, defender);
			}
		}
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
