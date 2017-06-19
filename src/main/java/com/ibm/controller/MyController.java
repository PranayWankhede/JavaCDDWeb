package com.ibm.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.ChainCodeResponse;
import org.hyperledger.fabric.sdk.ChaincodeLanguage;
import org.hyperledger.fabric.sdk.DeployRequest;
import org.hyperledger.fabric.sdk.FileKeyValStore;
import org.hyperledger.fabric.sdk.InvokeRequest;
import org.hyperledger.fabric.sdk.Member;
import org.hyperledger.fabric.sdk.QueryRequest;
import org.hyperledger.fabric.sdk.exception.ChainCodeException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.NoAvailableTCertException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@EnableAutoConfiguration
public class MyController {

	private static Log logger = LogFactory.getLog(MyController.class);

	private Member registrar;

	private String chainCodeID;
	
	private Chain chain;

	public MyController() throws Exception {
		logger.info("In MyController constructor ...");

		 chain = new Chain("javacdd");
		 chain.setDeployWaitTime(60*5);
		try {

			chain.setMemberServicesUrl("grpc://localhost:7054", null);
			// create FileKeyValStore
			Path path = Paths.get(System.getProperty("user.home"), "/test.properties");
			if (Files.notExists(path))
				Files.createFile(path);
			chain.setKeyValStore(new FileKeyValStore(path.toString()));
			chain.addPeer("grpc://localhost:7051", null);

			registrar = chain.getMember("admin");
			if (!registrar.isEnrolled()) {
				registrar = chain.enroll("admin", "Xurw3yU9zI0l");
			}
			logger.info("registrar is :" + registrar.getName() + ",secret:" + registrar.getEnrollmentSecret());
			chain.setRegistrar(registrar);
			chain.eventHubConnect("grpc://localhost:7053", null);

			chainCodeID = deploy();

		} catch (CertificateException | IOException  e) {
			logger.error(e.getMessage(), e);
			throw new Exception(e);
		}
		logger.info("Out MyController constructor.");

	}

	String deploy() throws ChainCodeException, NoAvailableTCertException, CryptoException, IOException {
		DeployRequest deployRequest = new DeployRequest();
		ArrayList<String> args = new ArrayList<String>();
		args.add("init");
		args.add("farmer");
		args.add("20");
		args.add("42");
		deployRequest.setArgs(args);
		deployRequest.setChaincodePath(Paths.get(System.getProperty("user.home"), "git", "JavaCDD").toString());
		deployRequest.setChaincodeLanguage(ChaincodeLanguage.JAVA);
		deployRequest.setChaincodeName(chain.getName());

		ChainCodeResponse chainCodeResponse = registrar.deploy(deployRequest);
		return chainCodeResponse.getChainCodeID();
	}

	@RequestMapping(method = RequestMethod.GET, path = "/executeContract", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	String executeContract(@RequestParam String clientName, @RequestParam String lon,
			@RequestParam String lat) throws JsonProcessingException {

		logger.info("Calling /executeContract ...");

		InvokeRequest invokeRequest = new InvokeRequest();
		ArrayList<String> args = new ArrayList<String>();
		args.add("executeContract");
		args.add(clientName);
		args.add(lon);
		args.add(lat);
		invokeRequest.setArgs(args);
		invokeRequest.setChaincodeLanguage(ChaincodeLanguage.JAVA);
		invokeRequest.setChaincodeID(chainCodeID);
		invokeRequest.setChaincodeName(chain.getName());

		try {
			
			
			ChainCodeResponse chainCodeResponse = registrar.invoke(invokeRequest);
			logger.info("End call /executeContract.");

			return new ObjectMapper().writeValueAsString(chainCodeResponse);
		} catch (ChainCodeException | NoAvailableTCertException | CryptoException | IOException e) {
			logger.error("Error", e);
			return new ObjectMapper().writeValueAsString(e);
		}

	}

	@RequestMapping(method = RequestMethod.GET, path = "/query", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	String query(@RequestParam String clientName) throws JsonProcessingException {

		logger.info("Calling /query ...");

		QueryRequest queryRequest = new QueryRequest();
		ArrayList<String> args = new ArrayList<String>();
		args.add("query");
		args.add(clientName);
		queryRequest.setArgs(args);
		queryRequest.setChaincodeLanguage(ChaincodeLanguage.JAVA);
		queryRequest.setChaincodeID(chainCodeID);

		try {
			ChainCodeResponse chainCodeResponse = registrar.query(queryRequest);
			logger.info("End call /query.");

			return new ObjectMapper().writeValueAsString(chainCodeResponse);
		} catch (ChainCodeException | NoAvailableTCertException | CryptoException | IOException e) {
			logger.error("Error", e);
			return new ObjectMapper().writeValueAsString(e);
		}

		
	}

	@RequestMapping(method = RequestMethod.GET, path = "/chain", produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	String chain() {

		logger.info("Calling /chain ...");
		logger.info("{name:\"" + registrar.getChain().getName() + "\",peersCount:"
				+ registrar.getChain().getPeers().size() + "}");
		return "{\"name\":\"" + registrar.getChain().getName() + "\",\"peersCount\":"
				+ registrar.getChain().getPeers().size() + "}";

	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(MyController.class, args);
	}

}