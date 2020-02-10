package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;


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
			new L2DCLikeSwitches("Nexus 93120", 65000.0/2, 0.0 , 1200.0/2, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 1200),
			new L2DCLikeSwitches("Nexus 93180", 56100/2, 0.0 , 1800/2, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 1800),
			new L2DCLikeSwitches("Nexus 93240", 30000, 0.0 , 2400/2, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 2400),
			new L2DCLikeSwitches("Nexus 93360", 32500, 0.0 , 1800, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 3600),
			new L2DCLikeSwitches("Nexus 93364", 51685.01, 0.0 , 3200, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 6400)
			);
	public static List<TelcoLikeSwitches> telcoLikeSwitchesAvailable = Arrays.asList(
			new TelcoLikeSwitches("NCS 5001", 41610, 0.0 , 400, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 800),
			new TelcoLikeSwitches("NCS 5002", 62140, 0.0 , 600, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 1200),
			new TelcoLikeSwitches("NCS 5011", 115325, 0.0 , 1600, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 3200),
			new TelcoLikeSwitches("NCS 5502", 139000, 0.0 , 2400, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 4800),
			new TelcoLikeSwitches("NCS 5504", 146500, 0.0 , 2700, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 5400),
			new TelcoLikeSwitches("NCS 5508", 271000, 0.0 , 14400, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 28800),
			new TelcoLikeSwitches("NCS 5516", 450000, 0.0 , 28800, t->t.getFirst()*10 + t.getSecond()*40 + t.getThird()*100 <= 57600)
			);
	public static List<Pluggables> pluggablesAvailable = Arrays.asList(
			new Pluggables("10G SFP+ copper", 79 , 0.0 , 10),
			new Pluggables("40G QSFP copper", 96.12 , 0.0 , 40),
			new Pluggables("100G QSFP copper", 150.45 , 0.0 , 100),
			new Pluggables("10G SFP+ optical", 234 , 0.0 , 10),
			new Pluggables("40G QSFP optical", 620.17 , 0.0 , 40),
			new Pluggables("100G QSFP optical", 1283.16 , 0.0 , 100),
			new Pluggables("100G CFP optical", 2500 , 0.0 , 100)
			);
	public static SortedMap<String , OtherElements> otherElementsAvailable = new TreeMap <> ();
	static
	{
		otherElementsAvailable.put("Fabric Card" , new OtherElements("Fabric Card" , "A99-SFC2" , 60000.00 , 0.0));
		otherElementsAvailable.put("RSP-SE" , new OtherElements("RSP-SE" , "" , 93300.00 , 0.0));
		otherElementsAvailable.put("RP2-SE" , new OtherElements("RP2-SE" , "A99-RP2-SE" , 80000.00 , 0.0));
		otherElementsAvailable.put("Fan 9906" , new OtherElements("Fan 9906", "ASR-9906-FAN", 3800.00 , 0.0));
		otherElementsAvailable.put("Fan 9910" , new OtherElements("Fan 9910" , "ASR-9910-FAN" , 5000.00 , 0.0));
		otherElementsAvailable.put("Power Supply: 2kW DC" , new OtherElements("Power Supply: 2kW DC" , "PWR-2KW-DC-V2", 2800.00 , 0.0));
		otherElementsAvailable.put("Power Supply: 3kW AC" , new OtherElements("Power Supply: 3kW AC" , "PWR-3KW-AC-V2" , 3212.00 , 0.0));
		otherElementsAvailable.put("Power Supply: 4,4kW DC" , new OtherElements("Power Supply: 4,4kW DC" , "PWR-4.4KW-DC-V3", 5600.00 , 0.0));
		otherElementsAvailable.put("Power Supply: 6kW AC" , new OtherElements("Power Supply: 6kW AC" , "PWR-6KW-AC-V3", 5600.00 , 0.0));
	}
	public static List<RouterChassis> routerChassisAvailable = Arrays.asList(
			//new RouterChassis("XX", numLineCards, numRsps, numFanTrays, numFabricCards, priceDollars, capacityGbps)
			new RouterChassis("ASR 9904", 0.0 , 2, 2, 1, 0, 312200, 3200),			
			new RouterChassis("ASR 9906", 0.0 , 4, 2, 2, 5, 544185, 6400),			
			new RouterChassis("ASR 9910", 0.0 , 8, 2, 2, 5, 578174.99, 12800),			
			new RouterChassis("ASR 9912", 0.0 , 10, 2, 7, 7, 677000, 16000),			
			new RouterChassis("ASR 9922", 0.0 , 20, 2, 4, 7, 749600, 32000)
			);
	
	public static List<LineCards> lineCardsAvailable = Arrays.asList(
			new LineCards("A9K-16X100GE-SE", 1820000.0 , 0.0 , 16, 100.0),			
			new LineCards("A9K-1X100GE-SE", 240000.0 , 0.0 , 1, 100.0),			
			new LineCards("A9K-48X10GE-1G-SE", 350000.0 , 0.0 , 48, 10.0),
			new LineCards("Invented40G", 350000.0 , 0.0 , 12, 40.0)
			);

	public static List<Server> serversAvailable = Arrays.asList(
			new Server("Dell 360", 25000 , 0.0)
			);
	
	public static class BomMember
	{
		private final String name;
		private final double priceDollars;
		private final double consumptionKw;
		public BomMember(String name, double priceDollars, double consumptionKw) {
			super();
			this.name = name;
			this.priceDollars = priceDollars;
			this.consumptionKw = consumptionKw;
		}
		public String getName() {
			return name;
		}
		public double getPriceDollars() {
			return priceDollars;
		}
		public double getConsumptionKw() {
			return consumptionKw;
		}
		
	}
	
	
	public static class Server extends BomMember
	{
		public Server(String name, double priceDollars, double consumptionKw) {
			super(name, priceDollars, consumptionKw);
		}
	}

	
	public static class LineCards extends BomMember
	{
		private final int numPorts;
		private final double portRateGbps;
//		public LineCards(int numPorts, double portRateGbps) {
//			super();
//			this.numPorts = numPorts;
//			this.portRateGbps = portRateGbps;
//		}
		public LineCards(String name, double priceDollars, double consumptionKw, int numPorts, double portRateGbps) {
			super(name, priceDollars, consumptionKw);
			this.numPorts = numPorts;
			this.portRateGbps = portRateGbps;
		}
		public int getNumPorts() {
			return numPorts;
		}
		public double getPortRateGbps() {
			return portRateGbps;
		}
	}

	
	
	public static class RouterChassis extends BomMember
	{
		private final int numLineCards;
		private final int numRsps;
		private final int numFanTrays;
		private final int numFabricCards;
		private final double priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars;
		private final double capacityGbps;
		public RouterChassis(String name, double consumptionKw , int numLineCards, int numRsps, int numFanTrays, int numFabricCards,
				double priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars
				, double capacityGbps) 
		{
//		
//		public RouterChassis(String name, double priceDollars, double consumptionKw, int numLineCards, int numRsps,
//				int numFanTrays, int numFabricCards, double priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars,
//				double capacityGbps) 
//		{
			super(name, priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars, consumptionKw);
			this.numLineCards = numLineCards;
			this.numRsps = numRsps;
			this.numFanTrays = numFanTrays;
			this.numFabricCards = numFabricCards;
			this.priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars = priceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars;
			this.capacityGbps = capacityGbps;
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
	
	public static class Pluggables extends BomMember
	{
		private final double lineRateGbps;
		public Pluggables(String name, double priceDollars, double consumptionKw, double lineRateGbps) {
			super(name, priceDollars, consumptionKw);
			this.lineRateGbps = lineRateGbps;
		}
		public double getLineRateGbps() {
			return lineRateGbps;
		}
		
	}
	
	public static class TelcoLikeSwitches extends BomMember
	{
		private final double capacityGbps;
		private final Function<Triple<Double,Double,Double>,Boolean> validationFunction;
		public TelcoLikeSwitches(String name, double priceDollars, double consumptionKw, double capacityGbps,
				Function<Triple<Double, Double, Double>, Boolean> validationFunction) {
			super(name, priceDollars, consumptionKw);
			this.capacityGbps = capacityGbps;
			this.validationFunction = validationFunction;
		}
		public double getCapacityGbps() {
			return capacityGbps;
		}
		public Function<Triple<Double, Double, Double>, Boolean> getValidationFunction() {
			return validationFunction;
		}
	}
	
	
	public static class L2DCLikeSwitches extends BomMember
	{
		private final double capacityGbps;
		private final Function<Triple<Double,Double,Double>,Boolean> validationFunction;
		public L2DCLikeSwitches(String name, double priceDollars, double consumptionKw, double capacityGbps,
				Function<Triple<Double, Double, Double>, Boolean> validationFunction) {
			super(name, priceDollars, consumptionKw);
			this.capacityGbps = capacityGbps;
			this.validationFunction = validationFunction;
		}
		public double getCapacityGbps() {
			return capacityGbps;
		}
		public Function<Triple<Double, Double, Double>, Boolean> getValidationFunction() {
			return validationFunction;
		}
		
	}

	public static class OtherElements extends BomMember
	{
		private final String type;
		public OtherElements(String name, String type , double priceDollars, double consumptionKw) {
			super(name, priceDollars, consumptionKw);
			this.type = type;
		}
		public String getType() {
			return type;
		}
	}
	
	
	
	
}
