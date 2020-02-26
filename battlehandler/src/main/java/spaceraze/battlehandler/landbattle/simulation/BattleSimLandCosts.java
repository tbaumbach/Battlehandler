package spaceraze.battlehandler.landbattle.simulation;

/**
 * This class is used to send information about an ongoing simulation to a listener of that simulation
 * @author WMPABOD
 *
 */
public class BattleSimLandCosts {
	private int attTfCostSupply,attTroopsCostSupply,defTroopsCostSupply;
	private int attTfCostBuy,attTroopsCostBuy,defTroopsCostBuy;
	private String message;
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getAttTfCostBuy() {
		return attTfCostBuy;
	}

	public void setAttTfCostBuy(int attTfCostBuy) {
		this.attTfCostBuy = attTfCostBuy;
	}

	public int getAttTfCostSupply() {
		return attTfCostSupply;
	}

	public void setAttTfCostSupply(int attTfCostSupply) {
		this.attTfCostSupply = attTfCostSupply;
	}

	public int getAttTroopsCostBuy() {
		return attTroopsCostBuy;
	}

	public void setAttTroopsCostBuy(int attTroopsCostBuy) {
		this.attTroopsCostBuy = attTroopsCostBuy;
	}

	public int getAttTroopsCostSupply() {
		return attTroopsCostSupply;
	}

	public void setAttTroopsCostSupply(int attTroopsCostSupply) {
		this.attTroopsCostSupply = attTroopsCostSupply;
	}

	public int getDefTroopsCostBuy() {
		return defTroopsCostBuy;
	}

	public void setDefTroopsCostBuy(int defTroopsCostBuy) {
		this.defTroopsCostBuy = defTroopsCostBuy;
	}

	public int getDefTroopsCostSupply() {
		return defTroopsCostSupply;
	}

	public void setDefTroopsCostSupply(int defTroopsCostSupply) {
		this.defTroopsCostSupply = defTroopsCostSupply;
	}

	
}
