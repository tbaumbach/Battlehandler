package spaceraze.battlehandler.landbattle;

import java.util.LinkedList;
import java.util.List;

import spaceraze.util.general.Logger;
import spaceraze.world.enums.BattleGroupPosition;
import spaceraze.world.enums.LandBattleAttackType;

public class OpponentHandler {
	List<LandBattleOpponents> opponents;

	public OpponentHandler(){
		opponents = new LinkedList<>();
	}
	
	public void addOpponents(TaskForceTroop aTroop, TaskForceTroop bTroop){
		opponents.add(new LandBattleOpponents(aTroop,bTroop));
	}
	
	public LandBattleAttackType getFlankerAttackType(List<TaskForceTroop> opponentList, LandBattleGroup otherBattleGroup){
		LandBattleAttackType anAttackType = null;
		TaskForceTroop opposingTroop = opponentList.get(0); // assuming there are only one opponent, should be correct for flankers
		BattleGroupPosition opposingPosition = otherBattleGroup.getPosition(opposingTroop);
		// FLANKER_VS_FLANKER
		if (opposingPosition == BattleGroupPosition.FLANKER){
			anAttackType = LandBattleAttackType.FLANKER_VS_FLANKER;
		}else
		// FLANKER_VS_FIRSTLINE // i.e. backstabbing
		if (opposingPosition == BattleGroupPosition.FIRST_LINE){
			anAttackType = LandBattleAttackType.FLANKER_VS_FIRSTLINE;
		}else
		// FLANKER_VS_SUPPORT
		if (opposingPosition == BattleGroupPosition.SUPPORT){
			anAttackType = LandBattleAttackType.FLANKER_VS_SUPPORT;
		}
		return anAttackType;
	}
	
	public boolean noOpponent(TaskForceTroop aTroop){
		boolean found = false;
		int counter = 0;
		while (!found && counter < opponents.size()){
			LandBattleOpponents anOpponents = opponents.get(counter);
			if (anOpponents.getContain(aTroop)){
				found = true;
			}else{
				counter++;
			}
		}
		return !found;
	}
	
	public boolean maxOneOpponent(TaskForceTroop aTroop){
    	Logger.finer("maxOneOpponent: " + aTroop.getTroop().getUniqueShortName());
		int found = 0;
		int counter = 0;
		while (found < 2 && counter < opponents.size()){
	    	Logger.finer("found=" + found + " counter=" + counter);
			LandBattleOpponents anOpponents = opponents.get(counter);
			if (anOpponents.getContain(aTroop)){
		    	Logger.finer("anOpponents.getContain(aTroop)=true ");
				found++;
			}
			counter++;
		}
		return found < 2;
	}

	public List<TaskForceTroop> getOpponents(TaskForceTroop aTroop){
		List<TaskForceTroop> foundOpponents = new LinkedList<>();
		for (LandBattleOpponents anOpponent : opponents) {
			if (anOpponent.getContain(aTroop)){
				TaskForceTroop otherTroop = anOpponent.getOpponent(aTroop);
				foundOpponents.add(otherTroop);
			}
		}
		return foundOpponents;
	}

}
