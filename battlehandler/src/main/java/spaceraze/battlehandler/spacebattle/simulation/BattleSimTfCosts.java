package spaceraze.battlehandler.spacebattle.simulation;

/**
 * This class is used to send information about an ongoing simulation to a listener of that simulation
 * @author WMPABOD
 *
 */
public class BattleSimTfCosts {
	private int tf1CostSupply,tf2CostSupply;
	private int tf1CostBuy,tf2CostBuy;
	private String message;
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public BattleSimTfCosts(){
	}

	public int getTf1CostBuy() {
		return tf1CostBuy;
	}

	public void setTf1CostBuy(int tf1CostBuy) {
		this.tf1CostBuy = tf1CostBuy;
	}

	public int getTf1CostSupply() {
		return tf1CostSupply;
	}

	public void setTf1CostSupply(int tf1CostSupply) {
		this.tf1CostSupply = tf1CostSupply;
	}

	public int getTf2CostBuy() {
		return tf2CostBuy;
	}

	public void setTf2CostBuy(int tf2CostBuy) {
		this.tf2CostBuy = tf2CostBuy;
	}

	public int getTf2CostSupply() {
		return tf2CostSupply;
	}

	public void setTf2CostSupply(int tf2CostSupply) {
		this.tf2CostSupply = tf2CostSupply;
	}
	
}
