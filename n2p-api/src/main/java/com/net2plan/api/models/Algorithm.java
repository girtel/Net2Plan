package com.net2plan.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import nonapi.io.github.classgraph.json.Id;

public class Algorithm {

	@Id
	public int algorithmId;
	
	@JsonProperty("inputParameter") public String inputParameter;
	@JsonProperty("outputParameter") public String outputParameter;
	@JsonProperty("description") public String description;
	
	public Algorithm (int algorithmId, String inputParameter, String description)
	{
		this.algorithmId = algorithmId;
		this.inputParameter = inputParameter;
		this.description = description;
		this.outputParameter = inputParameter + " algorithmId=" +algorithmId;
	}
	public Algorithm () {
		this.description = "404. Algorithm not found";
	}	

	public int getAlgorithmId() {
		return algorithmId;
	}

	public void setAlgorithmId(int algorithmId) {
		this.algorithmId = algorithmId;
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
