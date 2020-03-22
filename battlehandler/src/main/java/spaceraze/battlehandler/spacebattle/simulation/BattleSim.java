/*
 * Created on 2005-maj-05
 */
package spaceraze.battlehandler.spacebattle.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import spaceraze.battlehandler.spacebattle.SpaceBattlePerformer;
import spaceraze.util.general.Logger;
import spaceraze.world.GameWorld;
import spaceraze.world.Spaceship;
import spaceraze.world.SpaceshipType;
import spaceraze.world.VIP;
import spaceraze.world.spacebattle.TaskForce;
import spaceraze.world.spacebattle.TaskForceSpaceShip;

/**
 * @author WMPABOD
 *
 *         This class is used to simulate SpaceRaze battles. This is a
 *         client-side version of the battle sim. It cannot use any resources on
 *         the server.
 * 
 *         This class can be used to: -(static)check if a string containing ship
 *         data is correct -create a BattleSim object (params:
 *         gameworld,simListener) -start the simulations -stop the simulations
 * 
 */
public class BattleSim extends Thread {
	private double tf1wins;
	private double tf2wins;
	private String message;
	private int totalCombatNr = 500;
	private static int maximumNrShips = 1000;
	private static int sleep = 0;
	private BattleSimListener battleSimListener;
	private GameWorld gameWorld;
	private String tf1ships;
	private String tf2ships;

	public BattleSim(BattleSimListener aBattleSimListener, GameWorld aGameWorld) {
		this.battleSimListener = aBattleSimListener;
		this.gameWorld = aGameWorld;
	}
	/*
	 * public BattleSim(double tf1wins, double tf2wins,String message,int
	 * tf1CostBuy,int tf1CostSupply,int tf2CostBuy,int tf2CostSupply) { this.tf1wins
	 * = tf1wins; this.tf2wins = tf2wins; this.message = message; this.tf1CostBuy =
	 * tf1CostBuy; this.tf1CostSupply = tf1CostSupply; this.tf2CostBuy = tf2CostBuy;
	 * this.tf2CostSupply = tf2CostSupply; }
	 */
	
	private static String addAShip(String typeName, TaskForce tf, GameWorld gameWorld, boolean screened,
			int techBonus, int kills, int damaged, List<String> vipNames) {
		Logger.finer("BattleSim addAShip: " + typeName + " " + screened + " " + techBonus + " " + vipNames.size());
		String message = null;
		SpaceshipType sst = gameWorld.getSpaceshipTypeByName(typeName);
		if (sst == null) {
			// try to find by short name
			sst = gameWorld.getSpaceshipTypeByShortName(typeName);
		}
		if (sst != null) {
			Spaceship ss = sst.getShip(null, techBonus, 0);
			/// TODO 2019-12-07 undersök varför detta är avmarkerat? Var det så att vi gjorde
			// om och skeppen inte längre kan ändra screened? Testa i klienten och se om det
			// går att screena ett skepp.
			// ss.setScreened(screened);
			ss.setKills(kills);
			if (damaged > 0) {
				ss.setDamage(damaged);
			}
			tf.addSpaceship(new TaskForceSpaceShip(ss, createSpaceshipVips(gameWorld, vipNames)));
		} else {
			message = "Cannot find shiptype with name: " + typeName;
		}
		return message;
	}
	
	private static List<VIP> createSpaceshipVips(GameWorld gameWorld, List<String> vipNames) {
		return vipNames.stream()
				.map(vipName -> VIP.getNewVIPshortName(vipName, gameWorld) != null
						? VIP.getNewVIPshortName(vipName, gameWorld) : VIP.getNewVIP(vipName, gameWorld))
				.collect(Collectors.toList());
	}
	
	private static String addShips(TaskForce tf, GameWorld gameWorld, String ships) {
		StringTokenizer st = new StringTokenizer(ships, ";");
		String message = null;
		while (st.hasMoreTokens() && message == null) {
			String token = null;
			try {
				String aShip = st.nextToken();
				token = aShip;
				// set # of ships
				int multipleShipsEnd = aShip.indexOf("]");
				int nrShips = 1;
				if (multipleShipsEnd > -1) {
					// multiple instances of ships should be created
					String nrString = aShip.substring(1, multipleShipsEnd);
					aShip = aShip.substring(multipleShipsEnd + 1);
					Logger.finer("nrString: " + nrString);
					nrShips = Integer.parseInt(nrString);
				}
				// set VIPS/techbonus/screened
				int otherAbilitiesStart = aShip.indexOf("(");
				boolean screened = false;
				int techBonus = 0;
				int kills = 0;
				int damaged = 0;
				List<String> vipNames = new ArrayList<>();
				if (otherAbilitiesStart > -1) {
					// vips/tech exist
					String oaTemp = aShip.substring(otherAbilitiesStart + 1, aShip.length() - 1);
					aShip = aShip.substring(0, otherAbilitiesStart);
					Logger.finer("oaTemp:  " + oaTemp);
					StringTokenizer st2 = new StringTokenizer(oaTemp, ",");
					while (st2.hasMoreElements()) {
						String tempStr = st2.nextToken();
						Logger.finer(tempStr);
						if (tempStr.equalsIgnoreCase("s")) {
							// ship is screened
							screened = true;
						} else {
							int colonIndex = tempStr.indexOf(":");
							if (colonIndex > -1) {
								String prefix = tempStr.substring(0, colonIndex);
								String suffix = tempStr.substring(colonIndex + 1);
								if (prefix.equalsIgnoreCase("t")) {
									// tech bonus for ship
									techBonus = Integer.parseInt(suffix);
								} else if (prefix.equalsIgnoreCase("k")) {
									// nr kills for ship
									kills = Integer.parseInt(suffix);
								} else if (prefix.equalsIgnoreCase("d")) {
									// ship damage in percent
									damaged = Integer.parseInt(suffix);
								}
							} else {
								vipNames.add(tempStr);
							}
						}
					}
				}
				for (int i = 0; i < nrShips; i++) {
					message = addAShip(aShip, tf, gameWorld, screened, techBonus, kills, damaged, vipNames);

				}
			} catch (Exception e) {
				message = "Error when parsing token " + token;
			}
		}
		return message;
	}
	
	public void simulateBattles(String ships1, String ships2, int nrIterations, int maxNrShips, int sleepTime) {
		this.tf1ships = ships1;
		this.tf2ships = ships2;
		totalCombatNr = nrIterations;
		maximumNrShips = maxNrShips;
		sleep = sleepTime;
		start();
	}

	@Override
	public void run() {
		simulateBattles();
		battleSimListener.battleSimFinished();
	}

	public void simulateBattles() {
		Logger.finer("Battlesim started");
		Logger.finer("TotalNrBattles: " + totalCombatNr);
		Logger.setDoOutput(false);
		int countWinsPlayer1 = 0;
		int countWinsPlayer2 = 0;
		for (int i = 1; i <= totalCombatNr; i++) {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
			if ((i % 100) == 0) { // progress count
				Logger.finest("# " + i + "/" + totalCombatNr);
			}
			TaskForce tf1 = new TaskForce(null, null);
			TaskForce tf2 = new TaskForce(null, null);
			// add spaceships to task forces
			message = addShips(tf1, gameWorld, tf1ships);
			message = addShips(tf2, gameWorld, tf2ships);
			// perform combat
			(new SpaceBattlePerformer()).performCombat(tf1, tf2, gameWorld.getInitMethod());
			if (tf1.getStatus().equalsIgnoreCase("fighting")) {
				countWinsPlayer1++;
			} else {
				countWinsPlayer2++;
			}
			double tf1wins = (countWinsPlayer1 * 1.0) / i;
			double tf2wins = (countWinsPlayer2 * 1.0) / i;
			BattleSimResult bsr = new BattleSimResult(tf1wins, tf2wins);
			bsr.setIterations(i);
			if (battleSimListener != null) {
				battleSimListener.battleSimPerformed(bsr);
			}
		}
		Logger.finer("Fleet A: " + tf1wins);
		Logger.finer("Fleet B: " + tf2wins);
		Logger.setDoOutput(true);
		Logger.finer("Battlesim finished");
	}

	/**
	 * Compute costs of both TFs
	 * 
	 * @param ships1
	 *            semicolon-separated list of the ships in TF1:s fleet
	 * @param ships2
	 *            semicolon-separated list of the ships in TF2:s fleet
	 */
	public BattleSimTfCosts getTfCosts(String ships1, String ships2) {
		Logger.finer("getTfCosts called");
		Logger.finer("ships1: " + ships1);
		Logger.finer("ships2: " + ships2);
		Logger.setDoOutput(false);
		String costMessage;
		TaskForce tf1 = new TaskForce(null, null);
		TaskForce tf2 = new TaskForce(null, null);
		// add spaceships to fleets
		if ((ships1 == null) || (ships1.equals(""))) {
			costMessage = "No ships in fleet A";
		} else if ((ships2 == null) || (ships2.equals(""))) {
			costMessage = "No ships in fleet B";
		} else {
			costMessage = addShips(tf1, gameWorld, ships1);
			if (costMessage == null) {
				costMessage = addShips(tf2, gameWorld, ships2);
			}
		}
		// count # participating ships
		int totalShipCount = tf1.getTotalNrShips();
		totalShipCount = totalShipCount + tf2.getTotalNrShips();
		Logger.finer("totalShipCount: " + totalShipCount);
		Logger.finer("maximumNrShips: " + maximumNrShips);
		if (totalShipCount > maximumNrShips) {
			costMessage = "Total ship count exceeds maximum (" + totalShipCount + " > " + maximumNrShips + ")";
		}
		// compute costs (once)
		BattleSimTfCosts costs = new BattleSimTfCosts();
		if (costMessage == null) {
			costs.setTf1CostBuy(tf1.getTotalCostBuy());
			costs.setTf2CostBuy(tf2.getTotalCostBuy());
			costs.setTf1CostSupply(tf1.getTotalCostSupply());
			costs.setTf2CostSupply(tf2.getTotalCostSupply());
		} else {
			costs.setMessage(costMessage);
		}
		Logger.setDoOutput(true);
		Logger.finer("getTfCosts finished");
		Logger.fine("Message: " + costMessage);
		return costs;
	}

	public String getTf1wins() {
		return String.valueOf(Math.round(tf1wins * 100));
	}

	public String getTf2wins() {
		return String.valueOf(Math.round(tf2wins * 100));
	}

	public String getMessage() {
		return message;
	}

	public  void setTotalCombatNr(int totalCombatNr) {
		this.totalCombatNr = totalCombatNr;
	}
	
	

}
