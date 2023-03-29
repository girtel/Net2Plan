package com.net2plan.api.models;

public class Algorithm {
	
	public int algorithmId;
	public String algorithmName;
	public String inputParameter;
	public String outputParameter;
	public String description;
	
	
	public Algorithm(int algorithmId, String algorithmName, String inputParameter, String outputParameter,
			String description) 
	{
		super();
		this.algorithmId = algorithmId;
		this.algorithmName = algorithmName;
		this.inputParameter = inputParameter;
		this.outputParameter = outputParameter;
		this.description = description;
	}

	public Algorithm () 
	{
		this.algorithmId = -1;
		this.algorithmName = "None";
		this.inputParameter = "None";
		this.outputParameter = "None";
		this.description = "None";	
	}	
	
	public int getAlgorithmId() {
		return algorithmId;
	}

	public void setAlgorithmId(int algorithmId) {
		this.algorithmId = algorithmId;
	}
	
	public String getAlgorithmName() {
		return algorithmName;
	}
	public void setAlgorithmName(String algorithmName) {
		this.algorithmName = algorithmName;
	}

	public String getInputParameter() {
		return inputParameter;
	}

	public void setInputParameter(String inputParameter) {
		this.inputParameter = inputParameter;
	}

	public String getOutputParameter() {
		return outputParameter;
	}

	public void setOutputParameter(String outputParameter) {
		this.outputParameter = outputParameter;
	}
	
	
}
