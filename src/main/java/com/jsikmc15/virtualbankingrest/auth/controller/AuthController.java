package com.jsikmc15.virtualbankingrest.auth.controller;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsikmc15.virtualbankingrest.auth.service.AuthService;
import com.jsikmc15.virtualbankingrest.dtos.TokenDTO;
import com.jsikmc15.virtualbankingrest.dtos.TradingDTO;
import com.jsikmc15.virtualbankingrest.transfer.service.TransferService;
import com.jsikmc15.virtualbankingrest.utils.MyUtils;
import com.jsikmc15.virtualbankingrest.utils.ResponeCode;
import com.jsikmc15.virtualbankingrest.utils.TransactionGenerator;

import net.bytebuddy.description.ByteCodeElement.Token;

@RestController
public class AuthController {

	@Autowired
	AuthService authservice;
	
	@Autowired
	TransferService transferService;
	
	@Autowired
	TransactionGenerator transactionGenerator;
	
	//본인인증 url 생성 및 전달 
	@GetMapping(value="/oauth",produces = {"application/json;charset=utf-8"})
	public Map getAuthUrl(@RequestParam Map map,HttpServletRequest req) {
		
		if(req.getHeader("USER_SEQ_NO")!=null) {
			map.put("USER_SEQ_NO", req.getHeader("USER_SEQ_NO"));
			map.put("ACCESS_TOKEN", req.getHeader("ACCESS_TOKEN"));
			map.put("USER_CI", req.getHeader("USER_CI"));
			map.put("callbackUrl", req.getHeader("callbackUrl"));
		}
		Map result = authservice.getAuthUrl(map);
		
		if(result.get("location")!=null) {
			result.put("resp_code",ResponeCode.OK);
		}else {
			result.put("resp_code",ResponeCode.ERROR);
		}
		return result;
	}
	
	
	//vbankServer단 인증
	@PostMapping(value="/oauth/token")
	public Map setToken(@RequestBody Map map) {
		Map error = new HashMap();
		Map result;
		try {
		//계좌 등록코드를 받으면 AuthCode를 Token 정보를 모두 가져옴
		result = authservice.setToken(map);
		error.put("access_token", result.get("access_token"));
		
		System.out.println("clear");
		System.out.println(result.get("user_seq_no"));
		int affect = 0;
		
		if(result.get("user_seq_no")==null) {
			result = new HashMap();
			result.put("resp_code", ResponeCode.ERROR_AUTHTOKEN);
			return result;
		}
		
		Set<String> keys = result.keySet();
		
		for(String key : keys) {
			System.out.println(key+ " - " + result.get(key));
		}

		//seq를 통해 이미 등록된 사용자인지 판단
		if(result.get("user_seq_no")!=null && authservice.isUser(result)) {
			//계좌가 1회 이상 등록된 사용자라면, update 시나리오 탐
			affect =authservice.registOther(result);
			
		}else {
			//최초 계좌 생성인 경우, insert 시나리오 
			affect =authservice.registNew(result);
			
		}
		String fin = result.get("fintech_use_num").toString();
		error.put("fintech_use_num",fin);
		System.out.println("◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀TEST▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶");
		int kind = transactionGenerator.getOfficialCnt();
		
		
		System.out.println("■■■■■■");
		//OTT 결제 
		List<TradingDTO> data = transactionGenerator.getContent((new Random().nextInt(15-3+1)+3),
				new java.util.Date(),fin,(new Random().nextInt(kind)));
		System.out.println("■■■■■■");
		for(TradingDTO dto : data) {
			System.out.println(dto.toString());
			transferService.insertTestSet(dto);
		}
		//비정기 결제 
		System.out.println("▲▲▲▲▲▲");
		data = transactionGenerator.getExternalContent((new Random().nextInt(15-3+1)+3),
				new java.util.Date(),true, fin,(new Random().nextInt(kind)));
		System.out.println("▲▲▲▲▲▲");
		for(TradingDTO dto : data) {

			System.out.println(dto.toString());
			transferService.insertTestSet(dto);
		}
		
		
		//비정기 결제 
		data = transactionGenerator.getExternalContent((new Random().nextInt(15-3+1)+3),
				new java.util.Date(),false, fin,(new Random().nextInt(kind)));
		for(TradingDTO dto : data) {
			System.out.println(dto.toString());
			transferService.insertTestSet(dto);
		}

		
		if(affect ==0 ) {
			result.put("resp_code",ResponeCode.ERROR );
		}else {
			result.put("resp_code",ResponeCode.OK);
		}
		
		System.out.println("◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀◀TEST▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶▶");
		
		}catch(Exception e) {
			e.printStackTrace();
			return error;
		}

		
		return result;
	}
	

	@GetMapping("/sample/regist")
	public Map tmp(@RequestParam Map map, HttpServletRequest req) {
		System.out.println("access_token:"+ req.getHeader("access_token"));
		map.put("access_token", req.getHeader("access_token"));
		Map result = authservice.getAccountOpenBaking(map);
			
		return result;
	}
	
	//인증 및 계좌 첫 등록을 하는 경우
	@PostMapping(value="/user/info",produces = {"application/json"})
	public Map registNewAccount(@RequestBody Map<String,String> map) {
		Map result = new HashMap();
		for(Map.Entry<String, String> entry : map.entrySet()) {
			System.out.println(entry.getKey()+"-"+entry.getValue());
		}
		
		int affect =authservice.registNew(map);
		
		if(affect !=0) {
			result.put("resp_code",ResponeCode.OK);
		}else {
			result.put("resp_code",ResponeCode.ERROR);
		}
		return result;
	}
	
	//인증된 상태에서 계좌를 추가하는 경우
	@PostMapping("/user/info/{count}")
	public Map registOtherAccount(@RequestParam Map map,@PathVariable("count") int count) {
		Map result = new HashMap();
		int affect =authservice.registOther(map);
		
		if(affect !=0) {
			result.put("resp_code",ResponeCode.OK);
		}else {
			result.put("resp_code",ResponeCode.ERROR);
		}
		return result;
	}
	
}
