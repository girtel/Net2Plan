package com.net2plan.api.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.net2plan.api.models.Algorithm;
import com.net2plan.examples.general.offline.nfv.Offline_nfvPlacementILP_v1;
import com.net2plan.examples.ocnbook.offline.Offline_fa_ospfWeightOptimization_tabuSearch;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.ReaderNetPlanFromJson;
import com.net2plan.utils.Triple;
@RestController
public class AlgorithmController {

	//	Available algorithms
	Offline_nfvPlacementILP_v1 nfv = new Offline_nfvPlacementILP_v1();
	Offline_fa_ospfWeightOptimization_tabuSearch ts = new Offline_fa_ospfWeightOptimization_tabuSearch();
	
	List<Algorithm> alg = new ArrayList<>();
	List<IAlgorithm> list = new ArrayList<>();

	
	public AlgorithmController ()
	{
		
		nfv.setAlgorithmId(0); list.add(nfv);
		ts.setAlgorithmId(1); list.add(ts);
	}

	ObjectMapper mapper = new ObjectMapper();
	
	/* ALGORITHMS */
	@GetMapping(value = "/algorithms")
	public ResponseEntity<Object> getAllInfoFromAlgorithms()
	{
		if (list.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The system does not include any algorithm"));
		return ResponseEntity.ok(list);
	}
	@GetMapping(value = "/algorithm/{algId}")
	public ResponseEntity<Object> getAlgorithmById(@PathVariable int algId)
	{
		if (list.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The system does not include any algorithm"));
		if (algId > list.size() - 1) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The requested algorithm is not present"));
		return ResponseEntity.ok(list.get(algId));
	}
	@PostMapping(value = "/algorithm/{algId}")
	public ResponseEntity<Object> executeAlgorithmById(@PathVariable int algId)
	{
		if (list.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The system does not include any algorithm"));
		if (algId > list.size() - 1) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The requested algorithm is not present"));
		//list.get(algId).executeAlgorithm(null, null, null)
		return ResponseEntity.ok(list.get(algId));
	}
	@GetMapping(value = "/algorithm/{algId}/parameters")
	public ResponseEntity<Object> getAlgorithmParametersById(@PathVariable int algId)
	{
		if (list.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The system does not include any algorithm"));
		if (list.get(algId).getParameters().isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The requested algorithm is not present"));
		return ResponseEntity.ok(list.get(algId).getParameters());
	}
	@GetMapping(value = "/algorithm/{algId}/description")
	public ResponseEntity<Object> getAlgorithmDescriptionById(@PathVariable int algId)
	{
		if (list.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The system does not include any algorithm"));
		if (list.get(algId).getDescription().isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The requested algorithm is not present"));
		return ResponseEntity.ok(list.get(algId).getDescription());
	}
	@GetMapping(value = "/algorithm/{algId}/parameter/{paramId}")
	public ResponseEntity<Object> getAlgorithmParameterById(@PathVariable int algId, @PathVariable int paramId)
	{
		if (list.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The algorithm parameters are not present"));
		if (list.get(algId).getParameters().isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ReturnMessage("The requested algorithm is not present"));
		return ResponseEntity.ok(list.get(algId).getParameters().get(paramId));
	}
	
	/* NETPLAN */
	@GetMapping(value = "/netplan")
	public ResponseEntity<Object> getNetplanModel()
	{
		NetPlan netplan = new NetPlan();
		try {
			return ResponseEntity.ok(mapper.writeValueAsString(netplan));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ReturnMessage("EERRRRORRRR"));
			//e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
//		String currentWorkingDirectory = System.getProperty("user.dir");
//		System.out.println("Current working directory: " + currentWorkingDirectory);
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
            // read the JSON file into a User object
			ReaderNetPlanFromJson n2p = objectMapper.readValue(new File("test.json"), ReaderNetPlanFromJson.class);

            // print the user object
            System.out.println(n2p);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		String json = Files.readString(Paths.get("test.json"));
		ReaderNetPlanFromJson myObject = objectMapper.readValue(json, ReaderNetPlanFromJson.class);

		
//		ReaderNetPlanFromJson reader = new  ReaderNetPlanFromJson();
//		String jsonString = objectMapper.writeValueAsString(reader);
//		ReaderNetPlanFromJson myObject = objectMapper.readValue(jsonString, ReaderNetPlanFromJson.class);
		System.out.println(myObject);

	}
	
	
}
