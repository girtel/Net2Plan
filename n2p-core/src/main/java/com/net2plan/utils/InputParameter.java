/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/

package com.net2plan.utils;


import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.Constants.RunnableCodeType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/**
 * This class helps to define input parameters for algorithms and reports. By using this class it is possible to:
 * <ul>
 * <li>Define each parameter as an {@code InputParameter}, specifying its default value, description, and potentially valid values (e.g non-negativity).
 *  Also, the parameter type will be defined ({@code int}, {@code double}, {@code String}, {@code boolean}, ...) </li>
 * <li>By calling the method {@link #initializeAllInputParameterFieldsOfObject initializeAllInputParameterFieldsOfObject}, all the fields in the current class of the type 
 * {@code InputParameter} are initialized from the values from a {@code Map<String,String>} map. The checks on the input parameter ranges are checked. Java reflection is used for this. </li>
 * <li>The algorithm can use {@code return InputParameter.getInformationAllInputParameterFieldsOfObject(this);} inside its method {@code getParameters}, to return 
 * all the information of all the fields in the class of the type {@code InputParameter}. Again, this class uses Java reflection internally. </li>
 * </ul>
 * To recuperate the value assigned to an input parameter: use the methods {@code getXXX} 
 * @author Pablo Pavon-Marino
 */
public class InputParameter
{
	private boolean wasInitialized;
	private final String description , memberName;
	private final boolean isDouble,isInt,isLong,isString,isBoolean,isRunnableCode;

	private double valDouble;
	private double defaultDouble;
	private double double_lowerLimitAcceptableValues;
	private boolean double_lowerLimitIncluded;
	private double double_upperLimitAcceptableValues;
	private boolean double_upperLimitIncluded;

	private int valInt;
	private int defaultInt;
	private int int_lowerLimitAcceptableValues;
	private int int_upperLimitAcceptableValues;

	private long valLong;
	private long defaultLong;
	private long long_lowerLimitAcceptableValues;
	private long long_upperLimitAcceptableValues;

	private boolean valBoolean;
	private boolean defaultBoolean;
	
	private String valString;
	private String defaultString;

	private String valRunnable_file , valRunnable_classname , valRunnable_parameters; 
	private String defaultRunnable;

	public InputParameter (String memberName , double defaultValue , String description , double lowerLimitAcceptableValues , boolean lowerLimitIncluded , double upperLimitAcceptableValues , boolean upperLimitIncluded)
	{
		this.wasInitialized = false;
		this.memberName = memberName;
		this.description = description;
		isDouble = true; isInt = false; isLong = false; isString = false; isBoolean = false; isRunnableCode = false;
		defaultDouble = defaultValue;
		double_lowerLimitAcceptableValues = lowerLimitAcceptableValues;
		double_lowerLimitIncluded = lowerLimitIncluded;
		double_upperLimitAcceptableValues = upperLimitAcceptableValues;
		double_upperLimitIncluded = upperLimitIncluded;
	}
	public InputParameter (String memberName , double defaultValue , String par_description)
	{
		this (memberName , defaultValue , par_description , -Double.MAX_VALUE , true , Double.MAX_VALUE , true);
	}
	public InputParameter (String memberName , int defaultValue , String description , int lowerLimitAcceptableValues , int upperLimitAcceptableValues)
	{
		this.wasInitialized = false;
		this.memberName = memberName;
		this.description = description;
		isDouble = false; isInt = true; isLong = false; isString = false; isBoolean = false; isRunnableCode = false;
		defaultInt = defaultValue;
		int_lowerLimitAcceptableValues = lowerLimitAcceptableValues;
		int_upperLimitAcceptableValues = upperLimitAcceptableValues;
	}
	public InputParameter (String memberName , long defaultValue , String description , long lowerLimitAcceptableValues , long upperLimitAcceptableValues)
	{
		this.wasInitialized = false;
		this.memberName = memberName;
		this.description = description;
		isDouble = false; isInt = false; isLong = true; isString = false; isBoolean = false; isRunnableCode = false;
		defaultLong = defaultValue;
		long_lowerLimitAcceptableValues = lowerLimitAcceptableValues;
		long_upperLimitAcceptableValues = upperLimitAcceptableValues;
	}
	public InputParameter (String memberName , int defaultValue , String description)
	{
		this (memberName , defaultValue , description , Integer.MIN_VALUE , Integer.MAX_VALUE);
	}
	public InputParameter (String memberName , long defaultValue , String description)
	{
		this (memberName , defaultValue , description , Long.MIN_VALUE , Long.MAX_VALUE);
	}
	public InputParameter (String memberName , boolean defaultValue , String description)
	{
		this.wasInitialized = false;
		this.memberName = memberName;
		this.description = description;
		isDouble = false; isInt = false; isLong = false; isString = false; isBoolean = true; isRunnableCode = false;
		defaultBoolean = defaultValue;
	}
	public InputParameter (String memberName , String defaultValue , String description)
	{
		this.wasInitialized = false;
		this.memberName = memberName;
		this.description = description;
		if (RunnableCodeType.find(defaultValue) == null)
		{
			/* string field (may be select, but it is not RunnableCode) */
			isDouble = false; isInt = false; isLong = false; isString = true; isBoolean = false; isRunnableCode = false;
			defaultString = defaultValue;
		}
		else
		{
			/* runnable code */
			isDouble = false; isInt = false; isLong = false; isString = false; isBoolean = false; isRunnableCode = true;
			defaultRunnable = defaultValue;
		}
	}

	public double getDouble ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isDouble) throw new Net2PlanException ("Parameter " + this + " is not a double");
		return valDouble;
	}
	public int getInt ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isInt) throw new Net2PlanException ("Parameter " + this + " is not a integer");
		return valInt;
	}
	public Long getLong ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isLong) throw new Net2PlanException ("Parameter " + this + " is not a long");
		return valLong;
	}
	public String getString ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isString) throw new Net2PlanException ("Parameter " + this + " is not a String");
		return valString;
	}
	public boolean getBoolean ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isBoolean) throw new Net2PlanException ("Parameter " + this + " is not a boolean");
		return valBoolean;
	}
	public String getRunnable_file ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isRunnableCode) throw new Net2PlanException ("Parameter " + this + " is not a RunnableCode parameter");
		return valRunnable_file;
	}
	public String getRunnable_classname ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isRunnableCode) throw new Net2PlanException ("Parameter " + this + " is not a RunnableCode parameter");
		return valRunnable_classname;
	}
	public String getRunnable_parameters ()
	{
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + this + " was not initialized");
		if (!isRunnableCode) throw new Net2PlanException ("Parameter " + this + " is not a RunnableCode parameter");
		return valRunnable_parameters;
	}
	

	public void initialize (double val)
	{
		if (!isDouble) throw new Net2PlanException ("Parameter " + this + " is not a double");
		this.valDouble = val;
		wasInitialized = true;
		if (!isWithinAcceptableRange()) throw new Net2PlanException ("Parameter " + this + " is not within the acceptable range");
	}
	public void initialize (int val)
	{
		if (!isInt) throw new Net2PlanException ("Parameter " + this + " is not an integer");
		this.valInt = val;
		wasInitialized = true;
		if (!isWithinAcceptableRange()) throw new Net2PlanException ("Parameter " + this + " is not within the acceptable range");
	}
	public void initialize (long val)
	{
		if (!isLong) throw new Net2PlanException ("Parameter " + this + " is not a long");
		this.valLong = val;
		wasInitialized = true;
		if (!isWithinAcceptableRange()) throw new Net2PlanException ("Parameter " + this + " is not within the acceptable range");
	}
	public void initialize (String val)
	{
		if (!isString) throw new Net2PlanException ("Parameter " + this + " is not a String");
		this.valString = val;
		wasInitialized = true;
		if (!isWithinAcceptableRange()) throw new Net2PlanException ("Parameter " + this + " is not within the acceptable range");
	}
	public void initialize (boolean val)
	{
		if (!isBoolean) throw new Net2PlanException ("Parameter " + this + " is not a boolean");
		this.valBoolean = val;
		wasInitialized = true;
		if (!isWithinAcceptableRange()) throw new Net2PlanException ("Parameter " + this + " is not within the acceptable range");
	}
	public void initialize (String file , String classname , String parameters)
	{
		if (!isRunnableCode) throw new Net2PlanException ("Parameter " + this + " is not a runnable code");
		this.valRunnable_file = file;
		this.valRunnable_classname = classname;
		this.valRunnable_parameters = parameters;
		wasInitialized = true;
		if (!isWithinAcceptableRange()) throw new Net2PlanException ("Parameter " + this + " is not within the acceptable range");
	}

	public static Map<String,String> createMapFromInputParameters (InputParameter [] params)
	{
		Map<String,String> res = new HashMap<String,String> ();
		for (InputParameter p : params)
		{
			if (p.isBoolean) res.put (p.getInfo().getFirst () , "" + p.getBoolean());
			else if (p.isDouble) res.put (p.getInfo().getFirst () , "" + p.getDouble());
			else if (p.isInt) res.put (p.getInfo().getFirst () , "" + p.getInt());
			else if (p.isLong) res.put (p.getInfo().getFirst () , "" + p.getLong());
			else if (p.isString) res.put (p.getInfo().getFirst () , p.getString());
			else if (p.isRunnableCode) 
			{
				res.put (p.getInfo ().getFirst () + "_file" , p.getRunnable_file());
				res.put (p.getInfo ().getFirst () + "_classname" , p.getRunnable_classname());
				res.put (p.getInfo ().getFirst () + "_parameters" , p.getRunnable_parameters());
			}
			else throw new RuntimeException ("Bad");
		}
		return res;
	}

	public static void initializeAllInputParameterFieldsOfObject (Object o , Map<String,String> params)
	{
//		System.out.println ("o.getClass() " +  o.getClass());
//		System.out.println ("o.getClass().getDeclaredFields() " +  o.getClass().getDeclaredFields());
//
		for (Field f : o.getClass().getDeclaredFields())
		{
			Object possibleParam;
			try 
			{ 
				f.setAccessible(true);
				possibleParam = f.get (o); 
			} catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("Could not get member " + f + " from object " + o); } 
			if (!(possibleParam instanceof InputParameter)) continue;
			InputParameter par = (InputParameter) possibleParam;
			par.initializeFromMap(params);
		}
	}

	public static void initializeAllInputParameterFieldsOfObject (Object o , String className , Map<String,String> params)
	{
		Class<?> c = o.getClass ();
//		System.out.println ("Init c.getName (): " + c.getName ());
		while (!c.getName().equals(className)) 
		{ c = c.getSuperclass(); /*System.out.println ("c.getName (): " + c.getName ()); */ if (c == new Object ().getClass ()) throw new RuntimeException ("Bad"); }
		
//		System.out.println ("End c.getName (): " + c.getName ());
		for (Field f : c.getDeclaredFields())
		{
			Object possibleParam;
			try 
			{ 
				f.setAccessible(true);
				possibleParam = f.get (o); 
			} catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("Could not get member " + f + " from object " + o); } 
			if (!(possibleParam instanceof InputParameter)) continue;
			InputParameter par = (InputParameter) possibleParam;
			par.initializeFromMap(params);
		}
	}

	public static List<Triple<String, String, String>> getInformationAllInputParameterFieldsOfObject (Object o)
	{
		List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
		/* Return the information of the input parameters defined with InputParameter classes. 
		 * This uses java reflection to access the names of the fields in this class (Field objects) in running time */
		for (Field f : o.getClass().getDeclaredFields ())
		{
			Object possibleParam;
			try 
			{ 
				f.setAccessible(true);
				possibleParam = f.get (o); 
			} catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("Could not get member " + f + " from object " + o); } 
			if (!(possibleParam instanceof InputParameter)) continue;
			InputParameter par = (InputParameter) possibleParam;
			algorithmParameters.add(par.getInfo());
		}
		return algorithmParameters;
	}

	public static List<Triple<String, String, String>> getInformationAllInputParameterFieldsOfObject (Object o , String className)
	{
		Class<?> c = o.getClass ();
//		System.out.println ("Init c.getName (): " + c.getName ());
		while (!c.getName().equals(className)) 
		{ 
			c = c.getSuperclass(); /* System.out.println ("c.getName (): " + c.getName ()); */ 
			if (c == new Object ().getClass ()) throw new RuntimeException ("Bad"); 
		}

		List<Triple<String, String, String>> algorithmParameters = new LinkedList<Triple<String, String, String>>();
		/* Return the information of the input parameters defined with InputParameter classes. 
		 * This uses java reflection to access the names of the fields in this class (Field objects) in running time */
		for (Field f : c.getDeclaredFields ())
		{
			Object possibleParam;
			try 
			{ 
				f.setAccessible(true);
				possibleParam = f.get (o); 
			} catch (Exception e) { e.printStackTrace(); throw new Net2PlanException ("Could not get member " + f + " from object " + o); } 
			if (!(possibleParam instanceof InputParameter)) continue;
			InputParameter par = (InputParameter) possibleParam;
			algorithmParameters.add(par.getInfo());
		}
		return algorithmParameters;
	}

	@Override
	public String toString () 
	{
		if (isDouble) return "(Double) " + memberName + ": " + (wasInitialized? valDouble : "NOT READ YET");
		if (isInt) return "(Integer) " + memberName + ": " + (wasInitialized? valInt : "NOT READ YET");
		if (isLong) return "(Long) " + memberName + ": " + (wasInitialized? valLong : "NOT READ YET");
		if (isString) return "(String) " + memberName + ": " + (wasInitialized? valString : "NOT READ YET");
		if (isBoolean) return "(Boolean) " + memberName + ": " + (wasInitialized? valBoolean : "NOT READ YET");
		if (isRunnableCode) return "(RunnableCode) " + memberName + (wasInitialized? " file: " + valRunnable_file + ", classname: " + valRunnable_classname + ", parameters: " + valRunnable_parameters  : "NOT READ YET");
		throw new RuntimeException ("Unexpected error");
	}
	
	public void initializeFromMap (Map<String,String> map)
	{
		try 
		{ 
			if (isDouble) initialize(Double.parseDouble (map.get (memberName)));
			else if (isInt) initialize (Integer.parseInt (map.get (memberName)));
			else if (isLong) initialize (Long.parseLong (map.get (memberName)));
			else if (isString) initialize (map.get (memberName));
			else if (isBoolean) initialize (Boolean.parseBoolean(map.get (memberName)));
			else if (isRunnableCode) initialize (map.get (memberName + "_file") , map.get (memberName + "_classname") , map.get (memberName + "_parameters")); 
			else throw new RuntimeException ("Bad");
		} catch (Exception e) { e.printStackTrace(); System.out.println (map); for (Entry<String,String> ent : map.entrySet()) System.out.println("**" + ent.getKey() + "** = **" + ent.getValue() + "**"); throw new Net2PlanException ("Failing to convert the parameter **" + memberName + "** = " + map.get (memberName)); }
	}

	public boolean isWithinAcceptableRange () 
	{ 
		if (!wasInitialized) throw new Net2PlanException ("Parameter " + memberName + " was not read yet");
		if (isDouble)
		{
			if (double_lowerLimitIncluded && (valDouble < double_lowerLimitAcceptableValues)) return false; 
			else if (!double_lowerLimitIncluded && (valDouble <= double_lowerLimitAcceptableValues)) return false;
			if (double_upperLimitIncluded && (valDouble > double_upperLimitAcceptableValues)) return false; 
			else if (!double_upperLimitIncluded && (valDouble >= double_upperLimitAcceptableValues)) return false;
			return true;
		}
		if (isInt)
		{
			if (valInt < int_lowerLimitAcceptableValues) return false; 
			if (valInt > int_upperLimitAcceptableValues) return false;
			return true;
		}
		if (isLong)
		{
			if (valLong < long_lowerLimitAcceptableValues) return false; 
			if (valLong > long_upperLimitAcceptableValues) return false;
			return true;
		}
		if (isString)
		{
			final int selectOccurrence = this.defaultString.indexOf("#select#");
			if (selectOccurrence != -1)
			{
				String [] options = StringUtils.split(defaultString.substring(selectOccurrence + "#select#".length()) , " ");
				//System.out.println ("InputParameter :" + this.memberName + ", default: " + this.defaultString + ", options: " + Arrays.toString (options));
				boolean validOption = false; for (String option : options) if (valString.equals (option)) { validOption = true; break; }
				if (!validOption) return false; //throw new Net2PlanException ("Input parameter " + this.memberName + " is initialized with vaule '" + val + "' which is not a valid option :" + Arrays.toString(options));
			}
			return true;
		}
		if (isRunnableCode) return true;
		
		return true;
	}

	public Triple<String,String,String> getInfo ()
	{
		if (isDouble) return Triple.of(memberName, "" + defaultDouble, description);
		if (isInt) return Triple.of(memberName, "" + defaultInt, description);
		if (isLong) return Triple.of(memberName, "" + defaultLong, description);
		if (isString) return Triple.of(memberName, defaultString, description);
		if (isBoolean) return Triple.of(memberName, "#boolean# " + defaultBoolean, description);
		if (isRunnableCode) return Triple.of (memberName, defaultRunnable, description);
		throw new RuntimeException ("Unexpected error");
	}

	/** Receives a map which assigns for each parameter name, a set of possible values, and returns a list with the cartesian 
	 * product of all the maps combining the different parameter values
	 * @param paramKeyValues the input param-key values 
	 * @return see above
	 */
	public static List<Map<String,String>> getCartesianProductOfParameters (Map<String,List<String>> paramKeyValues)
	{
		List<Map<String,String>> res = new LinkedList<> ();
		
		Map<String,String> firstParamSetting = paramKeyValues.entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue().get(0)));
		res.add(firstParamSetting);
		for (String key : paramKeyValues.keySet())
		{
			List<String> valuesThisKey = paramKeyValues.get(key);
			List<Map<String,String>> copyCurrentResWithoutThisParam = res.stream().map(e->new HashMap<> (e)).collect(Collectors.toList()); 
			res.clear();
			for (String newValueParam : valuesThisKey)
			{
				for (Map<String,String> map : copyCurrentResWithoutThisParam)
				{
					final Map<String,String> mapCopy = new HashMap<> (map);
					mapCopy.put (key , newValueParam); res.add(mapCopy);
				}
			}
		}
		return res;
	}

	/** Returns the map with the default parameters of the parameter description given. For those default descriptions 
	 *  starting with character '#' (e.g. #select# 1 2 3), we return the first element after the second '#', trimmed without 
	 *  spaces (e.g. '2' in the previous example) 
	 * @param paramDescriptions param name, param default, param description
	 * @return see above
	 */
	public static Map<String,String> getDefaultParameters (List<Triple<String, String, String>> paramDescriptions)
	{
		Map<String,String> res = new HashMap<> ();
		for (Triple<String,String,String> triple : paramDescriptions)
		{
			final String key = triple.getFirst();
			String defaultDescription = triple.getSecond();
			if (defaultDescription.startsWith("#"))
			{
				final int secondIndex = defaultDescription.indexOf("#" , 1);
				if (secondIndex != -1) 	
				{
					defaultDescription = defaultDescription.substring(secondIndex + 1).trim();
					final int indexFirstSpace = defaultDescription.indexOf(" ");
					if (indexFirstSpace != -1)
						defaultDescription = defaultDescription.substring(0 , indexFirstSpace).trim();
				}
			}
			defaultDescription.trim();
			res.put(key  ,defaultDescription.trim());
		}
		return res;
	}
	
	public static void main (String [] args)
	{
//		/* Create a class extending AlgorithmParameters. The new fields are the ones to be read later from the map */
//		class Param 
//		{ 
//			public int name_int; public Integer name_Integer; public String name_String;  
//			public double name_double; public Double name_Double;   
//			public boolean name_boolean; public Boolean name_Boolean;   
//			public long name_long; public Long name_Long;   
//		}
//
//		Map<String,String> algs = new HashMap<String,String> ();
//		algs.put("name_long", "5");
//		algs.put("name_Long", "6");
//		algs.put("name_int", "7");
//		algs.put("name_Integer", "8");
//		algs.put("name_String", "I am a String");
//		algs.put("name_double", "9");
//		algs.put("name_Double", "10");
//		algs.put("name_boolean", "true");
//		algs.put("name_Boolean", "False");
//		
//		
//		//Param par = new Param (algs);
//		Param par = new Param (); InputParameter.read(algs , par);
//		System.out.println("Par:\n" + InputParameter.toString(par));
	}
}
