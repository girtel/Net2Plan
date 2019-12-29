package com.net2plan.examples.niw.research.technoec;


import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import com.google.common.collect.ImmutableSortedMap;
import com.net2plan.utils.Triple;

public class TecnoEc2_costModel
{
	public static List<L2DCLikeSwitches> l2dcSwitchesAvailable = Arrays.asList(
			new L2DCLikeSwitches("Nexus 93120", 65000.0/2, 1200.0/2, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 1200),
			new L2DCLikeSwitches("Nexus 93180", 56100/2, 1800/2, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 1800),
			new L2DCLikeSwitches("Nexus 93240", 30000, 2400/2, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 2400),
			new L2DCLikeSwitches("Nexus 93360", 32500, 1800, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 3600),
			new L2DCLikeSwitches("Nexus 93364", 51685.01, 3200, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 6400)
			);
	public static List<TelcoLikeSwitches> telcoLikeSwitchesAvailable = Arrays.asList(
			new TelcoLikeSwitches("NCS 5001", 41610, 400, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 800),
			new TelcoLikeSwitches("NCS 5002", 62140, 600, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 1200),
			new TelcoLikeSwitches("NCS 5011", 115325, 1600, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 3200),
			new TelcoLikeSwitches("NCS 5502", 139000, 2400, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 4800),
			new TelcoLikeSwitches("NCS 5504", 146500, 2700, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 5400),
			new TelcoLikeSwitches("NCS 5508", 271000, 14400, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 28800),
			new TelcoLikeSwitches("NCS 5516", 450000, 28800, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 57600)
			);
	public static List<Pluggables> pluggablesAvailable = Arrays.asList(
			new Pluggables("10G SFP+ copper", 79 , 10),
			new Pluggables("40G QSFP copper", 96.12 , 40),
			new Pluggables("100G QSFP copper", 150.45 , 100),
			new Pluggables("10G SFP+ optical", 234 , 10),
			new Pluggables("40G QSFP optical", 620.17 , 40),
			new Pluggables("100G QSFP optical", 1283.16 , 100),
			new Pluggables("100G CFP optical", 2500 , 100)
			);
	public static SortedMap<String , OtherElements> otherElementsAvailable = new TreeMap <> ();
	static
	{
		otherElementsAvailable.put("Fabric Card" , new OtherElements("Fabric Card" , "A99-SFC2" , 60000.00));
		otherElementsAvailable.put("RSP-SE" , new OtherElements("RSP-SE" , "" , 93300.00));
		otherElementsAvailable.put("RP2-SE" , new OtherElements("RP2-SE" , "A99-RP2-SE" , 80000.00));
		otherElementsAvailable.put("Fan 9906" , new OtherElements("Fan 9906", "ASR-9906-FAN", 3800.00));
		otherElementsAvailable.put("Fan 9910" , new OtherElements("Fan 9910" , "ASR-9910-FAN" , 5000.00));
		otherElementsAvailable.put("Power Supply: 2kW DC" , new OtherElements("Power Supply: 2kW DC" , "PWR-2KW-DC-V2", 2800.00));
		otherElementsAvailable.put("Power Supply: 3kW AC" , new OtherElements("Power Supply: 3kW AC" , "PWR-3KW-AC-V2" , 3212.00));
		otherElementsAvailable.put("Power Supply: 4,4kW DC" , new OtherElements("Power Supply: 4,4kW DC" , "PWR-4.4KW-DC-V3", 5600.00));
		otherElementsAvailable.put("Power Supply: 6kW AC" , new OtherElements("Power Supply: 6kW AC" , "PWR-6KW-AC-V3", 5600.00));
	}
	public static List<RouterChassis> routerChassisAvailable = Arrays.asList(
			//new RouterChassis("XX", numLineCards, numRsps, numFanTrays, numFabricCards, priceDollars, capacityGbps)
			new RouterChassis("ASR 9904", 2, 2, 1, 0, 312200, 3200),			
			new RouterChassis("ASR 9906", 4, 2, 2, 5, 544185, 6400),			
			new RouterChassis("ASR 9910", 8, 2, 2, 5, 578174.99, 12800),			
			new RouterChassis("ASR 9912", 10, 2, 7, 7, 677000, 16000),			
			new RouterChassis("ASR 9922", 20, 2, 4, 7, 749600, 32000)
			);
	public static List<LineCards> lineCardsAvailable = Arrays.asList(
			new LineCards("A9K-16X100GE-SE", 16, 100.0, 1820000),			
			new LineCards("A9K-1X100GE-SE", 1, 100.0, 240000),			
			new LineCards("A9K-48X10GE-1G-SE", 48, 10.0, 350000)
			);

	public static List<Server> serversAvailable = Arrays.asList(
			new Server("Dell 360", 25000)
			);
	
	public static class Server
	{
		private final String name;
		private final double priceDollars;
		public Server(String name, double priceDollars) {
			super();
			this.name = name;
			this.priceDollars = priceDollars;
		}
		public String getName() {
			return name;
		}
		public double getPriceDollars() {
			return priceDollars;
		}
	}

	
	public static class LineCards
	{
		private final String name;
		private final int numPorts;
		private final double portRateGbps;
		private final double priceDollars;
		public LineCards(String name, int numPorts, double portRateGbps, double priceDollars) {
			super();
			this.name = name;
			this.numPorts = numPorts;
			this.portRateGbps = portRateGbps;
			this.priceDollars = priceDollars;
		}
		public String getName() {
			return name;
		}
		public int getNumPorts() {
			return numPorts;
		}
		public double getPortRateGbps() {
			return portRateGbps;
		}
		public double getPriceDollars() {
			return priceDollars;
		}
		
	}

	
	
	public static class RouterChassis
	{
		private final String name;
		private final int numLineCards;
		private final int numRsps;
		private final int numFanTrays;
		private final int numFabricCards;
		private final double priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars;
		private final double capacityGbps;
		public RouterChassis(String name, int numLineCards, int numRsps, int numFanTrays, int numFabricCards,
				double priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars
				, double capacityGbps) 
		{
			this.name = name;
			this.numLineCards = numLineCards;
			this.numRsps = numRsps;
			this.numFanTrays = numFanTrays;
			this.numFabricCards = numFabricCards;
			this.priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars = priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars;
			this.capacityGbps = capacityGbps;
		}
		public String getName() {
			return name;
		}
		public int getNumLineCards() {
			return numLineCards;
		}
		public int getNumRsps() {
			return numRsps;
		}
		public int getNumFanTrays() {
			return numFanTrays;
		}
		public int getNumFabricCards() {
			return numFabricCards;
		}
		public double getPriceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars() {
			return priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars;
		}
		public double getCapacityGbps() {
			return capacityGbps;
		}
		
	}
	
	
	public static class Pluggables
	{
		private final String name;
		private final double priceDollars;
		private final double lineRateGbps;
		public Pluggables(String name, double priceDollars, double lineRateGbps) {
			this.name = name;
			this.priceDollars = priceDollars;
			this.lineRateGbps = lineRateGbps;
		}
		public String getName() {
			return name;
		}
		public double getPriceDollars() {
			return priceDollars;
		}
		public double getLineRateGbps() {
			return lineRateGbps;
		}
		
	}
	
	public static class TelcoLikeSwitches
	{
		private final String name;
		private final double priceInDollars;
		private final double capacityGbps;
		private final Function<Triple<Double,Double,Double>,Boolean> validationFunction;
		public TelcoLikeSwitches(String name, double priceInDollars, double capacityGbps,
				Function<Triple<Double, Double, Double>, Boolean> validationFunction) {
			this.name = name;
			this.priceInDollars = priceInDollars;
			this.capacityGbps = capacityGbps;
			this.validationFunction = validationFunction;
		}
		public String getName() {
			return name;
		}
		public double getPriceInDollars() {
			return priceInDollars;
		}
		public double getCapacityGbps() {
			return capacityGbps;
		}
		public Function<Triple<Double, Double, Double>, Boolean> getValidationFunction() {
			return validationFunction;
		}
	}
	
	
	public static class L2DCLikeSwitches
	{
		private final String name;
		private final double priceInDollars;
		private final double capacityGbps;
		private final Function<Triple<Double,Double,Double>,Boolean> validationFunction;
		public L2DCLikeSwitches(String name, double priceInDollars, double capacityGbps,
				Function<Triple<Double, Double, Double>, Boolean> validationFunction) {
			this.name = name;
			this.priceInDollars = priceInDollars;
			this.capacityGbps = capacityGbps;
			this.validationFunction = validationFunction;
		}
		public String getName() {
			return name;
		}
		public double getPriceInDollars() {
			return priceInDollars;
		}
		public double getCapacityGbps() {
			return capacityGbps;
		}
		public Function<Triple<Double, Double, Double>, Boolean> getValidationFunction() {
			return validationFunction;
		}
		
	}

	public static class OtherElements
	{
		private final String type;
		private final String name;
		private final double priceInDollars;
		public OtherElements(String type , String name, double priceInDollars) 
		{
			this.type = type;
			this.name = name;
			this.priceInDollars = priceInDollars;
		}
		public String getType() {
			return type;
		}
		public String getName() {
			return name;
		}
		public double getPriceInDollars() {
			return priceInDollars;
		}
	}
	
	
	
	
}
