package com.net2plan.api.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import com.net2plan.examples.general.offline.nfv.Offline_nfvPlacementILP_v1;
import com.net2plan.examples.ocnbook.offline.Offline_fa_ospfWeightOptimization_tabuSearch;
import com.net2plan.utils.Triple;
@RestController
public class AlgorithmController {

	//	Available algorithms
	Offline_nfvPlacementILP_v1 nfv = new Offline_nfvPlacementILP_v1();
	Offline_fa_ospfWeightOptimization_tabuSearch ts = new Offline_fa_ospfWeightOptimization_tabuSearch();
	
	List<Algorithm> alg = new ArrayList<>();
	List<Object> list = new ArrayList<Object>();

	
	public AlgorithmController ()
	{
		list.add(nfv);
		list.add(ts);
	}

	
	
	
	ObjectMapper mapper = new ObjectMapper();

	@GetMapping(value = "/test")
	public List<Object> getAllInfoFromAlgorithms()
	{
		return list;
	}
	@GetMapping(value="/testParameters")
	public List<Triple<String, String, String>> getParameters()
	{
		return nfv.getParameters();
	}
	
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
		AlgorithmRequest algReq = mapper.readValue(json, AlgorithmRequest.class);
		Algorithm al = new Algorithm(alg.size(), algReq.inputParameter, algReq.description);
		alg.add(al);
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
