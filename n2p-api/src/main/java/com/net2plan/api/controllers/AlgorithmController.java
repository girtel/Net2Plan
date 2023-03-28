package com.net2plan.api.controllers;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.net2plan.api.models.Algorithm;
@RestController
public class AlgorithmController {

	List<Algorithm> alg = new ArrayList<>();
	ObjectMapper mapper = new ObjectMapper();

	@GetMapping(value = "/algorithms")
	public List<Algorithm> getAlgorithms()
	{
		return alg;
	}
	
	@GetMapping(value = "/algorithm/{id}")
	public Algorithm getAlgorithmById(@PathVariable int id)
	{
		if (alg.size() == 0) return new Algorithm();
		return alg.get(id);
	}
	
	@PostMapping (value = "/algorithm")
	public List<Algorithm> setAlgorithm (@RequestBody String json) throws JsonMappingException, JsonProcessingException
	{
		Algorithm algorithm = mapper.readValue(json, Algorithm.class);
		alg.add(algorithm);
		return alg;
	}
	
	@PostMapping (value = "/algorithm/{id}")
	public Map<String, String> launchAlgorithmById (@PathVariable int id)
	{
		Map <String, String> h = new HashMap<>();
		if (alg.get(id) == null) h.put("message", "404. AlgorithmId not found");
		else h.put("message", "Launched Algorithm");
		
		return h;
	}
	@PutMapping(value = "/algorithm/{id}")
	public Map<String, String> setInputParatmeters (@PathVariable int algorithmId, @RequestBody String inputParameter)
	{
		Map <String, String> h = new HashMap<>();
		if (alg.get(algorithmId) == null) h.put("message", "404. AlgorithmId not found");
		alg.get(algorithmId).setInputParameter(inputParameter);
		h.put("message", "New inputParameter value defined");
		return h;
	}
	@DeleteMapping (value = "/algorithm/{id}")
	public Map<String, String> deleteAlgorithm (@PathVariable int algorithmId)
	{
		Map <String, String> h = new HashMap<>();
		if (alg.get(algorithmId) == null) h.put("message", "404. AlgorithmId not found");
		alg.remove(algorithmId);
		h.put("message", "algorithm deleted");
		return h;
	}}
