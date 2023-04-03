package com.net2plan.interfaces.networkDesign;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ReaderNetPlanFromJson {

	
	@JsonProperty("name") private String name;
	@JsonProperty("description") private String description;
	@JsonProperty("version") private int version;
	@JsonProperty("nodes") private List<Node> nodes;
	@JsonProperty("links") private List<Link> links;
	@JsonProperty("demands") private List<Demand> demands;
	@JsonProperty("routes") private List<Route> routes;
	

	
	public ReaderNetPlanFromJson() {}
	
}
