/**
 * 
 */
package com.evolvus.sandstorm.camel.route;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.evolvus.sandstorm.Constants;
import com.evolvus.sandstorm.FileStatus;
import com.evolvus.sandstorm.InvalidFileException;
import com.evolvus.sandstorm.csv.bean.Response;
import com.evolvus.sandstorm.csv.bean.UserBean;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author EVOLVUS\shrimank
 *
 */
@Component
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UserUploadProcessor implements Processor {

	private static final String TOTAL_PROCESSED_COUNT = "totalProcessedCount";

	private static final String TOTAL_FAILED_COUNT = "totalFailedCount";
	
	private static final String TOTAL_TRANSACTION = "totalTransaction";

	private static final String PROCESSING_STATUS = "processingStatus";

	private static final String CAMEL_FILE_NAME = "CamelFileName";
	private static final String ERROR_LOG = "errorLog";
	private static final Logger LOGGER = LoggerFactory.getLogger(UserUploadProcessor.class);

	@Autowired
	private RestTemplate restTemplate;

	@Value("${platform.server}")
	private String platformServer;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Start :{}", exchange.getIn().getBody());
		}
		HttpHeaders httpHeader = new HttpHeaders();
		httpHeader.setContentType(MediaType.APPLICATION_JSON);
		httpHeader.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		String fileName = null;
		if(exchange.getIn().getHeader(CAMEL_FILE_NAME) != null) {
		 fileName=(String) exchange.getIn().getHeader(CAMEL_FILE_NAME);
		}
		
       String tenantId = fileName.substring( fileName.lastIndexOf("_")+1 , fileName.lastIndexOf("."));
	
       httpHeader.set("X-TENANT-ID", tenantId);
        final HttpEntity<String> entity = new HttpEntity<String>(httpHeader);

		List<String> createdUserList = new ArrayList();

		try {
			
			 final String url = platformServer+"/api/fileUpload?fileName="+exchange.getIn().getHeader(CAMEL_FILE_NAME);
             ResponseEntity<Response> responseFileUpload = restTemplate.exchange(url, HttpMethod.GET, entity, Response.class);
			
//			ResponseEntity<Response> responseFileUpload = restTemplate
//					.getForEntity(String.format("%s/api/fileUpload?fileName=%s", platformServer,
//							exchange.getIn().getHeader(CAMEL_FILE_NAME)), Response.class);
			
			if (responseFileUpload.hasBody()) {
				//Map<String, Object> fileUploadMap = (Map<String, Object>) responseFileUpload.getBody().getData();
				ArrayList<HashMap<String, Object>> obj = (ArrayList<HashMap<String, Object>>) responseFileUpload.getBody().getData();
				Map<String, Object> fileUploadMap = obj.get(0);
				 fileName=fileUploadMap.get("fileName").toString();
				httpHeader.set(Constants.TENANT_ID, fileUploadMap.get("tenantId").toString());
				httpHeader.set(Constants.ENTITY_ID, fileUploadMap.get("entityId").toString());
				httpHeader.set(Constants.ACCESS_LEVEL, fileUploadMap.get("accessLevel").toString());
				httpHeader.set(Constants.USER, fileUploadMap.get("createdBy").toString());

				List<UserBean> userBeanList = (List<UserBean>) exchange.getIn().getBody(List.class);
				List<Object> failedUsers= new ArrayList();
				Integer totalTransaction = userBeanList.size();
				Integer totalProcessCount = 0;
				Integer totalProcessedCount =0;
				Integer totalFailedCount = 0;
	
				for (UserBean userBean : userBeanList) {

					try {
						 final HttpEntity<String> header = new HttpEntity<String>(httpHeader);
						 final String urlForUser = platformServer+"/api/user?userId="+userBean.getUserId().toUpperCase();
			             ResponseEntity<Response> responseGetUser = restTemplate.exchange(urlForUser, HttpMethod.GET, header, Response.class);
			             final String urlForRole = platformServer+"/api/role?roleName="+userBean.getRole().toUpperCase();
			             ResponseEntity<Response> responseGetRole = restTemplate.exchange(urlForRole, HttpMethod.GET, header, Response.class);
						 final String urlForMasterCurrency = platformServer+"/api/masterCurrency?currencyName="+userBean.getMasterCurrency();
			             ResponseEntity<Response> responseMasterCurrency = restTemplate.exchange(urlForMasterCurrency, HttpMethod.GET, header, Response.class);
			             if(responseGetUser.hasBody() && responseGetRole.hasBody() ) {
			            	 System.out.println(responseGetUser.getBody().getData().toString());
			            	 if(responseGetUser.getBody().getData().toString() == "[]" && responseGetRole.getBody().getData().toString() != "[]" && responseMasterCurrency.getBody().getData().toString() != "[]") {
			            		  //entity id hardcoded for ban kcda
			         			userBean.setEntityId("H001B001");
								HttpEntity<UserBean> requestBody = new HttpEntity<UserBean>((UserBean) userBean, httpHeader);
								ResponseEntity<Response> responseUser = restTemplate.postForEntity(
										String.format("%s/api/user/bulk", platformServer), requestBody, Response.class);
								if (responseUser.hasBody()) {
									Response resp = responseUser.getBody();
									HashMap user = (LinkedHashMap) resp.getData();
									createdUserList.add(MessageFormat.format("{0},{1}", userBean.toString(),
											String.format("User Id [%s],tenant :[%s] created _id:%s", user.get("userId"),
													user.get("tenantId"), user.get("_id"))));
									if(resp.getStatus().equals("400")) {
										String errorLog="";
										totalFailedCount++;

										errorLog=errorLog+" line: "+totalProcessCount.toString();
										errorLog=errorLog+" message: "+resp.getDescription();
										failedUsers.add(errorLog);
									}else {
										totalProcessedCount++;
									}
										totalProcessCount++;
									
								}
			            	 } else {
			            		 String errorLog="";
			            		 totalFailedCount++;
			            		 totalProcessCount++;
			            		 if(responseGetUser.getBody().getData().toString() != "[]" && responseGetRole.getBody().getData().toString() == "[]" && responseMasterCurrency.getBody().getData().toString() == "[]") {
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message: Unable to add new User due to " + userBean.getUserName().toString()+ " user is already exist";
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message:  User save failed due to the Role " +userBean.getRole().toString() + " which is assigned to user not found";
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message:  User save failed due to the masterCurrency " +userBean.getMasterCurrency().toString() + " which is assigned to user is inValid";
				            			failedUsers.add(errorLog);
			            		 }else if(responseGetUser.getBody().getData().toString() == "[]" && responseGetRole.getBody().getData().toString() == "[]"  && responseMasterCurrency.getBody().getData().toString() == "[]") {
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message:  User save failed due to the Role "+ userBean.getRole().toString() + " which is assigned to user not found";
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message:  User save failed due to the masterCurrency " +userBean.getMasterCurrency().toString() + " which is assigned to user is inValid";
			            				failedUsers.add(errorLog);
			            		 } else if (responseGetUser.getBody().getData().toString() != "[]" && responseGetRole.getBody().getData().toString() == "[]" && responseMasterCurrency.getBody().getData().toString() != "[]") {
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message: Unable to add new User due to " + userBean.getUserName().toString() + " user is already exist";
			            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
			            			 errorLog=errorLog+" message:  User save failed due to the Role "+ userBean.getRole().toString() + " which is assigned to user not found";
			            				failedUsers.add(errorLog);
			            	 }else if(responseGetUser.getBody().getData().toString() != "[]" && responseGetRole.getBody().getData().toString() != "[]" && responseMasterCurrency.getBody().getData().toString() == "[]"){
			            		 errorLog=errorLog+" line: "+totalProcessCount.toString();
		            			 errorLog=errorLog+" message: Unable to add new User due to " + userBean.getUserName().toString() + " user is already exist";
		            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
		            			 errorLog=errorLog+" message:  User save failed due to the masterCurrency " +userBean.getMasterCurrency().toString() + " which is assigned to user is inValid";
		            				failedUsers.add(errorLog);
			            	 }else if(responseGetUser.getBody().getData().toString() == "[]" && responseGetRole.getBody().getData().toString() == "[]" && responseMasterCurrency.getBody().getData().toString() == "[]"){
			            		 errorLog=errorLog+" line: "+totalProcessCount.toString();
		            			 errorLog=errorLog+" message:  User save failed due to the Role "+ userBean.getRole().toString() + " which is assigned to user not found";
		            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
		            			 errorLog=errorLog+" message:  User save failed due to the masterCurrency " +userBean.getMasterCurrency().toString() + " which is assigned to user is inValid";
		            				failedUsers.add(errorLog);
			            	 }else if(responseGetUser.getBody().getData().toString() == "[]" && responseGetRole.getBody().getData().toString() != "[]" && responseMasterCurrency.getBody().getData().toString() == "[]"){
			            		 
		            			 errorLog=errorLog+" line: "+totalProcessCount.toString();
		            			 errorLog=errorLog+" message:  User save failed due to the masterCurrency " +userBean.getMasterCurrency().toString() + " which is assigned to user is inValid";
		            				failedUsers.add(errorLog);
			            	 }else if(responseGetUser.getBody().getData().toString() == "[]" && responseGetRole.getBody().getData().toString() == "[]" && responseMasterCurrency.getBody().getData().toString() != "[]"){
			            		 
			            		 errorLog=errorLog+" line: "+totalProcessCount.toString();
		            			 errorLog=errorLog+" message:  User save failed due to the Role "+ userBean.getRole().toString() + " which is assigned to user not found";
		            				failedUsers.add(errorLog);
			            	 }
//			            	 
			             }
			             }
			           
			
					} catch (Exception excep) {
						totalFailedCount++;
						if (LOGGER.isErrorEnabled()) {
							LOGGER.error("Exception while posting user :{}", excep);
						}
						createdUserList.add(
								String.format("User %s is not valid %s", userBean.getUserId(), excep.getMessage()));
					} finally {
						fileUploadMap.put(TOTAL_PROCESSED_COUNT, totalProcessedCount);
						fileUploadMap.put(TOTAL_FAILED_COUNT, totalFailedCount);
						fileUploadMap.put(TOTAL_TRANSACTION, totalTransaction);
						fileUploadMap.put(PROCESSING_STATUS, FileStatus.IN_PROGRESS);
						
//						ResponseEntity<Response> responseFileUploadUpdate = updateFileUpload(exchange, httpHeader,	fileUploadMap);
//						LOGGER.info("Updated FileRecord :{}", responseFileUploadUpdate);

					}
				}

				if (totalFailedCount > 0) {
					fileUploadMap.put(PROCESSING_STATUS, FileStatus.ERROR);
					fileUploadMap.put(ERROR_LOG,failedUsers );

				} else {
					fileUploadMap.put(PROCESSING_STATUS, FileStatus.COMPLETED);

				}
				ResponseEntity<Response> responseFileUploadUpdate = updateFileUpload(exchange, httpHeader,	fileUploadMap, fileName);
				LOGGER.info("Updated FileRecord :{}", responseFileUploadUpdate);

			} else {
				throw new InvalidFileException("File not available in FileUpload collection");
			}

		} catch (Exception execp) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Exception while getting fileUpload record  :{}", execp);
			}
		}

		exchange.getOut().setHeader(CAMEL_FILE_NAME, "PROCESSES_" + exchange.getIn().getHeader(CAMEL_FILE_NAME));
		exchange.getOut().setBody(createdUserList.toString());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("End :{}", createdUserList.size());
		}
		createdUserList.clear();

	}

	private ResponseEntity<Response> updateFileUpload(Exchange exchange, HttpHeaders httpHeader,
			Map<String, Object> fileUploadMap, String fileName) {
		HttpEntity<Map<String, Object>> requestFileUpload = new HttpEntity(fileUploadMap, httpHeader);
		
		
		final String url = platformServer+"/api/fileUpload/"+fileName;
		
		ResponseEntity<Response> tmpResponse = restTemplate.exchange(url,HttpMethod.PUT, requestFileUpload, Response.class);//postForEntity(url, temp1, Response.class);
	
		
		//ResponseEntity<Response> tmpResponse = restTemplate.postForEntity(String.format("%s/api/fileUpload/fileName=%s", platformServer,
				//exchange.getIn().getHeader(CAMEL_FILE_NAME)), requestFileUpload, Response.class);
		
		return tmpResponse;
	}
	
}
