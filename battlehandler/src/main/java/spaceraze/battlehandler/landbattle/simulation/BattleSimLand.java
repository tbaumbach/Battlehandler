/*
 * Created on 2005-maj-05
 */
package spaceraze.battlehandler.landbattle.simulation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import spaceraze.battlehandler.landbattle.LandBattle;
import spaceraze.battlehandler.landbattle.TaskForceTroop;
import spaceraze.util.general.Logger;
import spaceraze.world.GameWorld;
import spaceraze.world.Troop;
import spaceraze.world.TroopType;
import spaceraze.world.VIP;

/**
 * @author WMPABOD
 * <p>
 * This class is used to simulate SpaceRaze battles.
 */
public class BattleSimLand extends Thread {
    private String message;
    private int totalCombatNr = 500;
    private int sleep = 0;
    //	private static String gameWorldName;
    private GameWorld gameWorld;
    //private final String TF1HOMEPLANET_NAME = "tf1home";
    //private final String TF2HOMEPLANET_NAME = "tf2home";
    private final String BATTLEPLANET_NAME = "battleplanet";
    private int bg1CostSupply, bg2CostSupply;
    private int bg1CostBuy, bg2CostBuy;
    private String attTroopsString;
    private String defTroopsString;
    //private Map battleSimMap;
    private BattleSimLandListener battleSimLandListener;
    private boolean showTrace;
    private int planetResistance;

    public BattleSimLand(BattleSimLandListener aBattleSimLandListener, GameWorld aGameWorld) {
        this.battleSimLandListener = aBattleSimLandListener;
        this.gameWorld = aGameWorld;
        //battleSimMap = createMap();
    }

    private static String addATroop(String typeName, List<TaskForceTroop> troopsList, GameWorld gameWorld, int techBonus, int kills, int damage, List<String> vipNames) {
        Logger.finer("BattleSim addATroop: " + typeName + " " + techBonus + " " + vipNames.size());
        String message = null;
        TroopType tt = gameWorld.getTroopTypeByName(typeName, false);
        if (tt == null) {
            // try to find by short name
            tt = gameWorld.getTroopTypeByShortName(typeName);
        }
        if (tt != null) {
    	/*	if ((aPosition == BattleGroupPosition.FLANKER) & (!tt.isAttackScreened())){
        		message = "Cannot be set as a flanker: " + typeName;
    		}else{*/
            Troop aTroop =  tt.getTroop(null, techBonus, 0, 0);
            Logger.finer("New troop created: " + aTroop.getUniqueShortName());
            if (kills > 0) {
                aTroop.setKills(kills);
            }
            if (damage > 0) {
                int currentDC = (aTroop.getMaxDC() / 100) * (100 - damage);
                aTroop.setCurrentDC(currentDC);
            }

            List<VIP> vips = new ArrayList<>();
            for (String aVipName : vipNames) {
                Logger.finer("Adding VIP: " + aVipName + " " + aTroop.getUniqueShortName());
                VIP vip = VIP.getNewVIPshortName(aVipName, gameWorld) != null ? VIP.getNewVIPshortName(aVipName, gameWorld) : VIP.getNewVIP(aVipName, gameWorld);
                if (vip != null) {
                    vips.add(vip);
                }
                //tempVIP.setLocation(aTroop);
            }

            troopsList.add(new TaskForceTroop(aTroop, vips));
            //	}
        } else {
            message = "Cannot find trooptype with name: " + typeName;
        }
        return message;
    }

    private static String addTroops(List<TaskForceTroop> troopsList, GameWorld gameWorld, String troopsString, int currentTurn) {
        StringTokenizer st = new StringTokenizer(troopsString, ";");
        String message = null;
        while (st.hasMoreTokens() && message == null) {
            String token = null;
            try {
                String aTroop = st.nextToken();
                Logger.finer("addTroops, token: " + aTroop);
                token = aTroop;
                // set # of ships
                int multipleShipsEnd = aTroop.indexOf("]");
                int nrShips = 1;
                if (multipleShipsEnd > -1) {
                    // multiple instances of ships should be created
                    String nrString = aTroop.substring(1, multipleShipsEnd);
                    aTroop = aTroop.substring(multipleShipsEnd + 1);
                    Logger.finer("nrString: " + nrString);
                    nrShips = Integer.parseInt(nrString);
                }
                // set VIPS/techbonus/screened
                int otherAbilitiesStart = aTroop.indexOf("(");
//   				boolean screened = false;
                int techBonus = 0;
                int kills = 0;
                int damaged = 0;
                int turn = 1;
                List<String> vipNames = new ArrayList<>();
                if (otherAbilitiesStart > -1) {
                    // vips/tech exist
                    String oaTemp = aTroop.substring(otherAbilitiesStart + 1, aTroop.length() - 1);
                    aTroop = aTroop.substring(0, otherAbilitiesStart);
                    Logger.finer("oaTemp:  " + oaTemp);
                    StringTokenizer st2 = new StringTokenizer(oaTemp, ",");
                    while (st2.hasMoreElements()) {
                        String tempStr = st2.nextToken();
                        Logger.finer(tempStr);
                        int colonIndex = tempStr.indexOf(":");
                        try {
                            turn = Integer.parseInt(tempStr);
                        } catch (NumberFormatException nfe) {
                            // not a number...
                            if (colonIndex > -1) {
                                // tech bonus for ship
                                String bonusString = tempStr.substring(colonIndex + 1);
                                String typeOfBonus = tempStr.substring(0, colonIndex);
                                if (typeOfBonus.equals("t")) {
                                    techBonus = Integer.parseInt(bonusString);
                                } else if (typeOfBonus.equals("k")) {
                                    kills = Integer.parseInt(bonusString);
                                } else {
                                    damaged = Integer.parseInt(bonusString);
                                }

                            } else {
                                // is a VIP
//   	 								VIP tempVIP = g.getNewVIPshortName(tempStr);
//   	 								tempVIP.setBoss(tf.getPlayer());
//   	 								vips.add(tempVIP);
                                vipNames.add(tempStr);
                            }
                        }
                    }
                }
                if ((currentTurn == 0) || (turn == currentTurn)) { // current turn 0 is used when computing costs
                    for (int i = 0; i < nrShips; i++) {
                        message = addATroop(aTroop, troopsList, gameWorld, techBonus, kills, damaged, vipNames);
                    }
                }
            } catch (Exception e) {
                message = "Error when parsing token " + token;
            }
        }
        return message;
    }


    public void simulateBattles(String attTroops, String defTroops, int nrIterations, int sleepTime, boolean showTrace, int planetResistance) {
        this.attTroopsString = attTroops;
        this.defTroopsString = defTroops;
        this.totalCombatNr = nrIterations;
        this.sleep = sleepTime;
        this.showTrace = showTrace;
        this.planetResistance = planetResistance;
        start();
    }

    @Override
    public void run() {
        simulateBattles();
        if (battleSimLandListener != null) {
            battleSimLandListener.battleSimFinished();
        }
    }

    public void simulateBattles() {
        Logger.finer("Battlesim started");
        Logger.finer("attTroopsString: " + attTroopsString);
        Logger.finer("defTroopsString: " + defTroopsString);
        Logger.setDoOutput(showTrace);
        int countWinsPlayer1 = 0;
        int countWinsPlayer2 = 0;
        int totalNrRounds = 0;
        for (int i = 1; i <= totalCombatNr; i++) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            if (i % 100 == 0) { // progress count
                Logger.finest("# " + i + "/" + totalCombatNr);
				System.out.println(i);
            }

            List<TaskForceTroop> attTroops = new LinkedList<>();
            List<TaskForceTroop> defTroops = new LinkedList<>();



            boolean continueBattle = true;
            int currentTurn = 1;
            while (continueBattle) {
                Logger.finer("");
                Logger.finer("While loop, continueBattle=true, turn: " + currentTurn);
                Logger.finer("");
                // add spaceships to fleets
				message = addTroops(attTroops, defTroops, currentTurn);
				if (message == null) {
					Logger.finer("Units parsed ok!");
					Logger.finer("totalUnitCount: " + attTroops.size() + defTroops.size());
					// remove destroyed troops from lists
					removeDestroyedTroops(attTroops);
					removeDestroyedTroops(defTroops);
					// check if one or both sides have any troops
					if (attTroops.isEmpty()) {
						continueBattle = false;
						countWinsPlayer2++;
					} else if (defTroops.isEmpty()) {
						continueBattle = false;
						countWinsPlayer1++;
					} else {
						// perform land battle
						LandBattle battle = new LandBattle(defTroops, attTroops, BATTLEPLANET_NAME, planetResistance, currentTurn);
						battle.performBattle();
						currentTurn++;
					}
				} else {
					break;
				}
			}

            totalNrRounds += currentTurn - 1;
            Logger.finer("totalNrRounds: " + totalNrRounds + " currentTurn: " + currentTurn);
            double tf1wins = (countWinsPlayer1 * 1.0) / i;
            double tf2wins = (countWinsPlayer2 * 1.0) / i;
            double tmpAverageNrRounds = (totalNrRounds * 1.0) / i;
            BattleSimLandResult bslr = new BattleSimLandResult(tf1wins, tf2wins, tmpAverageNrRounds);
            bslr.setIterations(i);
            if (battleSimLandListener != null) {
                battleSimLandListener.battleSimPerformed(bslr);
            }
        }
        Logger.finer("");
        Logger.finer("");
        Logger.finer("");
        Logger.finer("countWinsPlayer1 (attacker): " + countWinsPlayer1);
        Logger.finer("countWinsPlayer2 (defender): " + countWinsPlayer2);
        if (message == null) {
            message = "Ok";
        }

        Logger.setDoOutput(true);
        Logger.finer("Battlesim finished");
    }

	private String addTroops(List<TaskForceTroop> attTroops, List<TaskForceTroop> defTroops, int currentTurn) {
    	String troopMessage;
		if ((attTroopsString == null) || (attTroopsString.equals(""))) {
			troopMessage = "No troops in attacking battlegroup";
		} else {
			if ((defTroopsString == null) || (defTroopsString.equals(""))) {
				troopMessage = "No troops in defending battlegroup";
			} else {
				troopMessage = addTroops(attTroops, gameWorld, attTroopsString, currentTurn);
				if (troopMessage == null) {
					troopMessage = addTroops(defTroops, gameWorld, defTroopsString, currentTurn);
				}
			}
		}
		return troopMessage;
	}

	/**
     * Compute costs of both TFs
     */
    public BattleSimLandCosts getCosts(String attTroopsString, String defTroopsString) {
        Logger.finer("getTfCosts called");
        Logger.finer("attTroops: " + attTroopsString);
        Logger.finer("defTroops: " + defTroopsString);
        Logger.setDoOutput(false);
        String costMessage;
        List<TaskForceTroop> attTroops = new LinkedList<>();
        List<TaskForceTroop> defTroops = new LinkedList<>();
        // add spaceships to fleets
        if ((attTroopsString == null) || (attTroopsString.equals(""))) {
            costMessage = "No troops in attacking battlegroup";
        } else {
            if ((defTroopsString == null) || (defTroopsString.equals(""))) {
                costMessage = "No troops in defending battlegroup";
            } else {
                costMessage = addTroops(attTroops, gameWorld, attTroopsString, 0);
                if (costMessage == null) {
                    costMessage = addTroops(defTroops, gameWorld, defTroopsString, 0);

                }
            }
        }
        int totalUnitCount = attTroops.size();
        totalUnitCount = totalUnitCount + defTroops.size();
        Logger.finer("totalShipCount: " + totalUnitCount);
        // compute costs (once)
        BattleSimLandCosts costs = new BattleSimLandCosts();
        if (costMessage == null) {
            costs.setAttTroopsCostBuy(computeCosts(attTroops, false));
            costs.setAttTroopsCostSupply(computeCosts(attTroops, true));
            costs.setDefTroopsCostBuy(computeCosts(defTroops, false));
            costs.setDefTroopsCostSupply(computeCosts(defTroops, true));
        } else {
            costs.setMessage(costMessage);
        }
        Logger.setDoOutput(true);
        Logger.finer("getTfCosts finished");
        Logger.finer("Message: " + costMessage);
        return costs;
    }

    private int computeCosts(List<TaskForceTroop> troops, boolean support) {
        int total = 0;
        for (TaskForceTroop troop : troops) {
            if (support) {
                total += troop.getTroop().getUpkeep();
            } else {
                total += troop.getTroop().getTroopType().getCostBuild(null);
            }
        }
        return total;
    }

    private static void removeDestroyedTroops(List<TaskForceTroop> troops) {
        List<TaskForceTroop> destroyedTroops = new LinkedList<>();
        for (TaskForceTroop aTroop : troops) {
            if (aTroop.getTroop().isDestroyed()) {
                destroyedTroops.add(aTroop);
            }
        }
        for (TaskForceTroop aTroop : destroyedTroops) {
            troops.remove(aTroop);
        }
    }

    public String getMessage() {
        return message;
    }

    public int getBg1CostBuy() {
        return bg1CostBuy;
    }

    public int getBg1CostSupply() {
        return bg1CostSupply;
    }

    public int getBg2CostBuy() {
        return bg2CostBuy;
    }

    public int getBg2CostSupply() {
        return bg2CostSupply;
    }


    public static void main(String[] args) {
/*    	
    	String attShipsString = "L-GA";
    	String attTroopsString = "binf;ht;lart;si";
    	String defTroopsString = "binf;binf;ati(r);hart;AALA;si";
*/
//    	String attTroopsString = "binf;ht;hart;mil(d)";
    	/*
    	String attTroopsString = "inf";
    	String defTroopsString = "inf";

    	int iterations = 1;
    	boolean showTrace = false;
    	GameWorld gw = GameWorldHandler.getGameWorld("titanium");
    	BattleSimLand bsl = new BattleSimLand(null,gw);
    	BattleSimLandCosts bslc = bsl.getCosts(attTroopsString, defTroopsString);
    	if (bslc.getMessage() == null){
    		bsl.simulateBattles(attTroopsString,defTroopsString,iterations,100,0,showTrace,3);
    		try {
    			Thread.sleep(1000);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    		System.out.println();
    		if (!bsl.getMessage().equalsIgnoreCase("ok")){
    			System.out.println("Message bsl: " + bsl.getMessage());
    		}else{
    			System.out.println("Attacker win: " + bsl.getBg1wins() + "%");
    			System.out.println("Defender win: " + bsl.getBg2wins() + "%");
    			System.out.println("Average # rounds: " + bsl.getAverageNrTurns());
    		}
    	}else{
    		System.out.println();
    		System.out.println("Costs reply: " + bslc.getMessage());
    	}*/
    }

    public void setTotalCombatNr(int totalCombatNr) {
        this.totalCombatNr = totalCombatNr;

    }

}
