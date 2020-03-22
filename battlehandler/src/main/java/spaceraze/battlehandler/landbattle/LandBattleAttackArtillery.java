package spaceraze.battlehandler.landbattle;

import java.util.List;

import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.enums.LandBattleAttackType;
import spaceraze.world.report.landbattle.EnemyTroopAttack;
import spaceraze.world.report.landbattle.EnemyTroopTarget;
import spaceraze.world.report.landbattle.OwnTroopAttack;
import spaceraze.world.report.landbattle.OwnTroopTarget;

public class LandBattleAttackArtillery extends LandBattleAttack {
	private TaskForceTroop attacker;
	private boolean defender; // true if troop is on their own planet

	public LandBattleAttackArtillery(TaskForceTroop anAttacker, List<TaskForceTroop> aTargetOpponents, boolean defending, int aResistance){
		super(LandBattleAttackType.ARTILLERY,aTargetOpponents,aResistance);
		attacker = anAttacker;
		this.defender = defending;
	}
	
	@Override
	public void performAttack(LandBattleGroup attBG, LandBattleGroup defBG, int attVipBonus, int defVipBonus) {
		if (attacker.getTroop().isDestroyed()){
			Logger.finer("Attacker already destroyed");
		}else{
			TaskForceTroop targetTroop = getRandomOpponent();
			if (targetTroop == null){
				Logger.finer("All opponents destroyed");
			}else{
				Logger.finer("targetTroop: " + targetTroop.getTroop().getUniqueShortName());
				int multiplier = Functions.getRandomInt(1, 20);
				Logger.finer("artMultiplier: " + multiplier);
				
				int attVIPBonus = attVipBonus;
				if(defender){
					attVIPBonus = defVipBonus;
				}
				
				int actualDamage = attacker.getTroop().getArtilleryActualDamage(multiplier,defender,resistance, attVIPBonus);
				String result = targetTroop.getTroop().hit(actualDamage, true, !defender, resistance);
				Logger.finer(result);
				if (targetTroop.getTroop().isDestroyed()){
					attacker.getTroop().addKill();
					//TODO 2020-01-05 Moved to GalaxyUpdater, the troop will be removed from galaxy after the fight is done. Borde inte den döda truppen ha lagt till i LandBattleReport.postBattleSurvivingOwnTroops eller LandBattleReport.postBattleSurvivingEnemyTroops?
					//g.removeTroop(targetTroop);
					
				}
				
				if(defender) {
					attBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
							new EnemyTroopAttack(attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), true),
							new OwnTroopTarget(targetTroop.getTroop().getUniqueName(), targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
							actualDamage, 0, multiplier, 0));
					
					defBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
							new OwnTroopAttack(attacker.getTroop().getUniqueName(), attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), true),
							new EnemyTroopTarget(targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
							actualDamage, 0, multiplier, 0));
				}else {
					attBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
							new OwnTroopAttack(attacker.getTroop().getUniqueName(), attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), true),
							new EnemyTroopTarget(targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
							actualDamage, 0, multiplier, 0));
					
					defBG.getReport().addReport(new spaceraze.world.report.landbattle.LandBattleAttack(
							new EnemyTroopAttack(attacker.getTroop().getTroopType().getUniqueName(), attacker.getTroop().getMaxDC(), attacker.getTroop().getCurrentDC(), true),
							new OwnTroopTarget(targetTroop.getTroop().getUniqueName(), targetTroop.getTroop().getTroopType().getUniqueName(), targetTroop.getTroop().getMaxDC(), targetTroop.getTroop().getCurrentDC()),
							actualDamage, 0, multiplier, 0));
				}
				
				//attReport.addAttackResultArtillery(attacker.getTroop(),targetTroop.getTroop(),actualDamage,multiplier,!defender);
				//defReport.addAttackResultArtillery(attacker.getTroop(),targetTroop.getTroop(),actualDamage,multiplier,defender);
			}
		}
	}


	@Override
	public String toString(){
		String retStr = "LBAA:";
		retStr = retStr + "att=" + attacker.getTroop().getUniqueShortName() + " artDam=" + attacker.getTroop().getAttackArtillery() + " ";
		retStr = retStr + "def=" + defender + " ";
		retStr = retStr + getAsString();
		return retStr;
	}

}
