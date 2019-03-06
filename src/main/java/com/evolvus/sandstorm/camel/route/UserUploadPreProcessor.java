package com.evolvus.sandstorm.camel.route;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.beanio.BeanReader;
import org.beanio.BeanReaderErrorHandlerSupport;
import org.beanio.InvalidRecordException;
import org.beanio.MalformedRecordException;
import org.beanio.RecordContext;
import org.beanio.StreamFactory;
import org.beanio.UnexpectedRecordException;
import org.beanio.UnidentifiedRecordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.evolvus.sandstorm.Constants;
import com.evolvus.sandstorm.FileStatus;
import com.evolvus.sandstorm.InvalidFileException;
import com.evolvus.sandstorm.csv.bean.Response;

import lombok.Data;

@Data
@Component

public class UserUploadPreProcessor implements Processor {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${platform.server}")
	private String platformServer;

	private static final String PROCESSING_STATUS = "processingStatus";

	private static final String CAMEL_FILE_NAME = "CamelFileName";
	private static final String ERROR_LOG = "errorLog";
	private static final Logger LOGGER = LoggerFactory.getLogger(UserUploadProcessor.class);
	private Resource streamMapping;
	Reader reader = null;
	InputStream in = null;
	private String msgType = null;
	private List<String> errorMsgList;

	private String fileStatus;
	private int count = 0;

	@Override
	public void process(Exchange exchange) throws Exception {
		StreamFactory factory = StreamFactory.newInstance();
		Resource resource = new ClassPathResource("beanio-user.xml");
		factory.load(resource.getInputStream());
 		errorMsgList = new ArrayList<>();
		fileStatus = "SUCCESS";
		BeanReader in = factory.createReader("users", exchange.getIn().getBody(File.class));
		beanIoErrorHandler(in);
		if (in.read() == null && count == 0) {
			fileStatus = "ERROR";
			errorMsgList.add("uploaded file is empty");
		}else {
			while(in.read()!=null) {
				
			}
		}
		exchange.getIn().setHeader("fileStatus", fileStatus);
		if (errorMsgList.size() > 0) {
			errorMsgList.add("Uploaded File has incorrect records, Please update the file again with correct records");
			getFileUpload(exchange);
		}
		in.close();
	}

	public void beanIoErrorHandler(BeanReader beanReader) {
		try {
			beanReader.setErrorHandler(new BeanReaderErrorHandlerSupport() {
				public void invalidRecord(InvalidRecordException ex) throws Exception {
					count++;
					for (int i = 0, j = ex.getRecordCount(); i < j; i++) {
						RecordContext context = ex.getRecordContext(i);					
							if(context.getRecordText().split(",").length !=15) {
								logError("line number " +context.getRecordLineNumber() + " has incorrect fields, it should have 15 fields");
							}
						if (context.hasRecordErrors()) {
							for (String error : context.getRecordErrors()) {
								logError(error);
							}
						}
						if (context.hasFieldErrors()) {
							for (String field : context.getFieldErrors().keySet()) {
								for (String error : context.getFieldErrors(field)) {
									logError(field + " : " + error);
								}
							}
						}
					}
				}

				public void unexpectedRecord(UnexpectedRecordException ex) throws Exception {
					for (int i = 0, j = ex.getRecordCount(); i < j; i++) {
						RecordContext context = ex.getRecordContext(i);
						if (context.hasRecordErrors()) {
							for (String error : context.getRecordErrors()) {
								logError(error);
							}
						}
					}
				}

				public void malformedRecord(MalformedRecordException ex) throws Exception {
					for (int i = 0, j = ex.getRecordCount(); i < j; i++) {
						RecordContext context = ex.getRecordContext(i);
						if (context.hasRecordErrors()) {
							for (String error : context.getRecordErrors()) {
								logError(error);
							}
						}
					}
				}

				public void unidentifiedRecord(UnidentifiedRecordException ex) throws Exception {
					for (int i = 0, j = ex.getRecordCount(); i < j; i++) {
						RecordContext context = ex.getRecordContext(i);
						if (context.hasRecordErrors()) {
							for (String error : context.getRecordErrors()) {
								logError(error);
							}
						}
					}
				}
			});
		} catch (Exception e1) {

			LOGGER.error("Exception while been reader  :{}", e1);
		}

	}

	private void logError(String error) {
		fileStatus = "ERROR";
		try {
			errorMsgList.add(error + "\n");
		} catch (Exception e) {
			LOGGER.error("Exception while adding error messages to arraylist  :{}", e);
		}
	}

	private void getFileUpload(Exchange exchange) {
		try {
			HttpHeaders httpHeader = new HttpHeaders();
			httpHeader.setContentType(MediaType.APPLICATION_JSON);
			httpHeader.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			String fileName = null;
			if (exchange.getIn().getHeader(CAMEL_FILE_NAME) != null) {
				fileName = (String) exchange.getIn().getHeader(CAMEL_FILE_NAME);
			}

			String tenantId = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf("."));

			httpHeader.set("X-TENANT-ID", tenantId);
			final HttpEntity<String> entity = new HttpEntity<String>(httpHeader);
			final String fildUrl = platformServer + "/api/fileUpload?fileName="
					+ exchange.getIn().getHeader(CAMEL_FILE_NAME);
			ResponseEntity<Response> responseFileUpload = restTemplate.exchange(fildUrl, HttpMethod.GET, entity,
					Response.class);

			if (responseFileUpload.hasBody()) {
				ArrayList<HashMap<String, Object>> obj = (ArrayList<HashMap<String, Object>>) responseFileUpload
						.getBody().getData();
				Map<String, Object> fileUploadMap = obj.get(0);
				fileName = fileUploadMap.get("fileName").toString();
				httpHeader.set(Constants.TENANT_ID, fileUploadMap.get("tenantId").toString());
				httpHeader.set(Constants.ENTITY_ID, fileUploadMap.get("entityId").toString());
				httpHeader.set(Constants.ACCESS_LEVEL, fileUploadMap.get("accessLevel").toString());
				httpHeader.set(Constants.USER, fileUploadMap.get("createdBy").toString());

				fileUploadMap.put(PROCESSING_STATUS, FileStatus.ERROR);
				fileUploadMap.put(ERROR_LOG, errorMsgList);

				ResponseEntity<Response> responseFileUploadUpdate = updateFileUpload(exchange, httpHeader,
						fileUploadMap, fileName);
				LOGGER.info("Updated FileRecord :{}", responseFileUploadUpdate);
			} else {
				throw new InvalidFileException("File not available in FileUpload collection");
			}
		} catch (Exception execp) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Exception while getting fileUpload record  :{}", execp);
			}
		}
	}

	private ResponseEntity<Response> updateFileUpload(Exchange exchange, HttpHeaders httpHeader,
			Map<String, Object> fileUploadMap, String fileName) {
		HttpEntity<Map<String, Object>> requestFileUpload = new HttpEntity(fileUploadMap, httpHeader);

		final String url = platformServer + "/api/fileUpload/" + fileName;

		ResponseEntity<Response> tmpResponse = restTemplate.exchange(url, HttpMethod.PUT, requestFileUpload,
				Response.class);// postForEntity(url, temp1, Response.class);
		return tmpResponse;
	}

}
