package com.net2plan.niw;

import java.util.Optional;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class OpticalAmplifierInfo 
{
	private double positionInFiber_km_minus2IsBooster_minus1Preamplif;
	private double gainDb;
	private double noiseFigureDb;
	private double cdCompensationPsPerNm;
	private double pmdPs;
	private double minAcceptableGainDb;
	private double maxAcceptableGainDb;
	private double minOutputPower_dBm;
	private double maxOutputPower_dBm;
	
	public boolean isBooster () { return positionInFiber_km_minus2IsBooster_minus1Preamplif == -2.0; }
	public boolean isPreamplifier () { return positionInFiber_km_minus2IsBooster_minus1Preamplif == -1.0; }
	public boolean isOla () { return positionInFiber_km_minus2IsBooster_minus1Preamplif >= 0.0; }
	public Optional<Double> getOlaPositionInKm () { return isOla ()? Optional.of(positionInFiber_km_minus2IsBooster_minus1Preamplif) : Optional.empty(); }
	public void setOlaPositionInKm (double positionKm) { if (positionKm < 0) throw new Net2PlanException (); this.positionInFiber_km_minus2IsBooster_minus1Preamplif = positionKm;  }
	
	public double getGainDb() {
		return gainDb;
	}
	public double getNoiseFigureDb() {
		return noiseFigureDb;
	}
	public double getCdCompensationPsPerNm() {
		return cdCompensationPsPerNm;
	}
	public double getPmdPs() {
		return pmdPs;
	}
	public double getMinAcceptableGainDb() {
		return minAcceptableGainDb;
	}
	public double getMaxAcceptableGainDb() {
		return maxAcceptableGainDb;
	}
	public double getMinAcceptableOutputPower_dBm() {
		return minOutputPower_dBm;
	}
	public double getMaxAcceptableOutputPower_dBm() {
		return maxOutputPower_dBm;
	}
	public OpticalAmplifierInfo setGainDb(double gainDb) {
		this.gainDb = gainDb;
		return this;
	}
	public OpticalAmplifierInfo setNoiseFigureDb(double noiseFigureDb) {
		this.noiseFigureDb = noiseFigureDb;
		return this;
	}
	public OpticalAmplifierInfo setCdCompensationPsPerNm(double cdCompensationPsPerNm) {
		this.cdCompensationPsPerNm = cdCompensationPsPerNm;
		return this;
	}
	public OpticalAmplifierInfo setPmdPs(double pmdPs) {
		this.pmdPs = pmdPs;
		return this;
	}
	public OpticalAmplifierInfo setMinAcceptableGainDb(double minAcceptableGainDb) {
		this.minAcceptableGainDb = minAcceptableGainDb;
		return this;
	}
	public OpticalAmplifierInfo setMaxAcceptableGainDb(double maxAcceptableGainDb) {
		this.maxAcceptableGainDb = maxAcceptableGainDb;
		return this;
	}
	public OpticalAmplifierInfo setMinAcceptableOutputPower_dBm(double minInputPower_dBm) {
		this.minOutputPower_dBm = minInputPower_dBm;
		return this;
	}
	public OpticalAmplifierInfo setMaxAcceptableOutputPower_dBm(double maxInputPower_dBm) {
		this.maxOutputPower_dBm = maxInputPower_dBm;
		return this;
	}
	public OpticalAmplifierInfo(double positionInFiber_km_minus2IsBooster_minus1Preamplif, 
			double gainDb, double noiseFigureDb, double cdCompensationPsPerNm, double pmdPs, double minAcceptableGainDb,
			double maxAcceptableGainDb, double minInputPower_dBm, double maxInputPower_dBm) 
	{
		this.positionInFiber_km_minus2IsBooster_minus1Preamplif = positionInFiber_km_minus2IsBooster_minus1Preamplif;
		this.gainDb = gainDb;
		this.noiseFigureDb = noiseFigureDb;
		this.cdCompensationPsPerNm = cdCompensationPsPerNm;
		this.pmdPs = pmdPs;
		this.minAcceptableGainDb = minAcceptableGainDb;
		this.maxAcceptableGainDb = maxAcceptableGainDb;
		this.minOutputPower_dBm = minInputPower_dBm;
		this.maxOutputPower_dBm = maxInputPower_dBm;
	}
	public static OpticalAmplifierInfo getDefaultBooster ()
	{
		return new OpticalAmplifierInfo(-2.0, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_GAIN_DB, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_NF_DB, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_CD_PSPERNM, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_PMD_PS, 
				WNetConstants.WFIBER_DEFAULT_OLAMINGAIN_DB.get(0), 
				WNetConstants.WFIBER_DEFAULT_OLAMAXGAIN_DB.get(0),
				WNetConstants.WFIBER_DEFAULT_OLAMINOUTPUTPOWER_DBM.get(0), 
				WNetConstants.WFIBER_DEFAULT_OLAMAXOUTPUTPOWER_DBM.get(0));
	}
	public static OpticalAmplifierInfo getDefaultPreamplifier ()
	{
		return new OpticalAmplifierInfo(-1.0, 
				WNetConstants.WFIBER_DEFAULT_PREAMPLIFIER_GAIN_DB, 
				WNetConstants.WFIBER_DEFAULT_PREAMPLIFIER_NF_DB, 
				WNetConstants.WFIBER_DEFAULT_PREAMPLIFIER_CD_PSPERNM, 
				WNetConstants.WFIBER_DEFAULT_PREAMPLIFIER_PMD_PS, 
				WNetConstants.WFIBER_DEFAULT_OLAMINGAIN_DB.get(0), 
				WNetConstants.WFIBER_DEFAULT_OLAMAXGAIN_DB.get(0),
				WNetConstants.WFIBER_DEFAULT_OLAMINOUTPUTPOWER_DBM.get(0), 
				WNetConstants.WFIBER_DEFAULT_OLAMAXOUTPUTPOWER_DBM.get(0));
	}
	public static OpticalAmplifierInfo getDefaultOla (double positionInKm)
	{
		return new OpticalAmplifierInfo(positionInKm, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_GAIN_DB, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_NF_DB, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_CD_PSPERNM, 
				WNetConstants.WFIBER_DEFAULT_BOOSTER_PMD_PS, 
				WNetConstants.WFIBER_DEFAULT_OLAMINGAIN_DB.get(0), 
				WNetConstants.WFIBER_DEFAULT_OLAMAXGAIN_DB.get(0),
				WNetConstants.WFIBER_DEFAULT_OLAMINOUTPUTPOWER_DBM.get(0), 
				WNetConstants.WFIBER_DEFAULT_OLAMAXOUTPUTPOWER_DBM.get(0));
	}
	public static Optional<OpticalAmplifierInfo> createFromString (String s)
	{
		try
		{
			final String[] vals = s.split(" ");
			final OpticalAmplifierInfo res = new OpticalAmplifierInfo(
					Double.parseDouble(vals [0]) , 
					Double.parseDouble(vals [1]) ,
					Double.parseDouble(vals [2]) ,
					Double.parseDouble(vals [3]) ,
					Double.parseDouble(vals [4]) ,
					Double.parseDouble(vals [5]) ,
					Double.parseDouble(vals [6]) ,
					Double.parseDouble(vals [7]) ,
					Double.parseDouble(vals [8]));
			return Optional.of(res);
		} catch (Exception e) { return Optional.empty(); } 
	}
	public String writeToString ()
	{
		return positionInFiber_km_minus2IsBooster_minus1Preamplif    
		+ " " + gainDb 
		+ " " + noiseFigureDb
		+ " " + cdCompensationPsPerNm
		+ " " + pmdPs
		+ " " + minAcceptableGainDb
		+ " " + maxAcceptableGainDb
		+ " " + minOutputPower_dBm
		+ " " + maxOutputPower_dBm;
	}
	
	public boolean isOkGainBetweenMargins ()
	{
		return this.getGainDb() >= this.getMinAcceptableGainDb() && this.getGainDb() <= this.getMaxAcceptableGainDb();
	}
}
