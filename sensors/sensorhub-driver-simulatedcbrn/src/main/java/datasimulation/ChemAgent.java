package datasimulation;


import java.util.Random;

/**
 * Created by Ian Patterson on 5/9/2017.
 */

//TODO: move all threat level detection into the CBRN simulated data class
public class ChemAgent
{
	private Random rand = new Random();
	private String agentID;
	private String agentClass;


	public void setAgentID(String agentID) {
		this.agentID = agentID;
	}


	public void setAgentClass(String agentClass) {
		this.agentClass = agentClass;
	}


	public ChemAgent(String type)
	{
		agentID = type;

		if(type.contains("g") || type.contains("G"))
		{
			agentClass = "G_Agent";
		}
		else if (type.contains("h") || type.contains("H"))
		{
			agentClass = "H_Agent";
		}
		else if (type.contains("v") || type.contains("V"))
		{
			agentClass = "V_Agent";
		}
		else if (type.contains("AC"))
		{
			agentClass = "BloodTIC";
		}
		else
		{
			agentClass = "undefined";
		}

	}


	public String getAgentID()
	{
		return agentID;
	}


	public String getAgentClass()
	{
		return agentClass;
	}
}
