package spaceraze.battlehandler.landbattle;

import java.util.List;

import spaceraze.servlethelper.game.troop.TroopMutator;
import spaceraze.servlethelper.game.troop.TroopPureFunctions;
import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.GameWorld;
import spaceraze.world.Troop;
import spaceraze.world.enums.LandBattleAttackType;

public class LandBattleAttackArtillery extends LandBattleAttack {
	private TaskForceTroop attacker;
	private boolean defender; // true if troop is on their own planet

	public LandBattleAttackArtillery(TaskForceTroop anAttacker, List<TaskForceTroop> aTargetOpponents, boolean defending, int aResistance){
		super(LandBattleAttackType.ARTILLERY,aTargetOpponents,aResistance);
		attacker = anAttacker;
		this.defender = defending;
	}
	
	@Override
	public void performAttack(LandBattleGroup attBG, LandBattleGroup defBG, int attVipBonus, int defVipBonus, GameWorld gameWorld) {
		if (TroopPureFunctions.isDestroyed(attacker.getTroop())){
			Logger.finer("Attacker already destroyed");
		}else{
			TaskForceTroop targetTroop = getRandomOpponent();
			if (targetTroop == null){
				Logger.finer("All opponents destroyed");
			}else{
				Logger.finer("targetTroop: " + targetTroop.getTroop().getShortName());
				int multiplier = Functions.getRandomInt(1, 20);
				Logger.finer("artMultiplier: " + multiplier);
				
				int attVIPBonus = attVipBonus;
				if(defender){
					attVIPBonus = defVipBonus;
				}
				
				int actualDamage = getArtilleryActualDamage(attacker.getTroop(), multiplier, defender, resistance, attVIPBonus);
				String result = TroopMutator.hit(targetTroop.getTroop(), actualDamage, true, !defender, resistance);
				Logger.finer(result);
				if (TroopPureFunctions.isDestroyed(targetTroop.getTroop())){
					TroopMutator.addKill(attacker.getTroop());
					//TODO 2020-01-05 Moved to GalaxyUpdater, the troop will be removed from galaxy after the fight is done. Borde inte den döda truppen ha lagt till i LandBattleReport.postBattleSurvivingOwnTroops eller LandBattleReport.postBattleSurvivingEnemyTroops?
					//g.removeTroop(targetTroop);
					
				}
				
				if(defender) {
					attBG.getReport().getLandBattleAttacks().add(LandBattleAttackGround.createLandBattleAttackForDefendingTroop(attacker, targetTroop, multiplier, 0, actualDamage, 0, true, gameWorld));
					
					defBG.getReport().getLandBattleAttacks().add(LandBattleAttackGround.getLandBattleAttackForAttackTroop(attacker, targetTroop, multiplier, 0, actualDamage, 0 , true, gameWorld));
				}else {
					attBG.getReport().getLandBattleAttacks().add(LandBattleAttackGround.getLandBattleAttackForAttackTroop(attacker, targetTroop, multiplier, 0, actualDamage, 0 , true, gameWorld));
					
					defBG.getReport().getLandBattleAttacks().add(LandBattleAttackGround.createLandBattleAttackForDefendingTroop(attacker, targetTroop, multiplier, 0, actualDamage, 0, true, gameWorld));
				}
				
				//attReport.addAttackResultArtillery(attacker.getTroop(),targetTroop.getTroop(),actualDamage,multiplier,!defender);
				//defReport.addAttackResultArtillery(attacker.getTroop(),targetTroop.getTroop(),actualDamage,multiplier,defender);
			}
		}
	}

	public int getArtilleryActualDamage(Troop troop, int multiplier, boolean defender, int resistance, int vipBonus){
		int baseDamage = TroopPureFunctions.getAttackArtillery(troop);
		Logger.finer("getAttackArtillery(): " + baseDamage);
		return TroopPureFunctions.getModifiedActualDamage(troop, baseDamage, multiplier, defender, resistance, vipBonus);
	}


	@Override
	public String toString(){
		String retStr = "LBAA:";
		retStr = retStr + "att=" + attacker.getTroop().getShortName() + " artDam=" + TroopPureFunctions.getAttackArtillery(attacker.getTroop()) + " ";
		retStr = retStr + "def=" + defender + " ";
		retStr = retStr + getAsString();
		return retStr;
	}

}
