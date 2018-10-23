package com.evolvus.sandstorm;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.evolvus.sandstorm.csv.bean.FileUpload;
import com.evolvus.sandstorm.csv.bean.Response;

/**
 * 
 * @author EVOLVUS\shrimank
 * @Modified EVOLVUS\mahendrar
 *
 */
@RestController
@RequestMapping("/api/v0.1")
public class FileUploadController {

	@Value("${file.extension.property}")
	private String fileExtension;

	@Value("${file.size.property}")
	private String fileSizeInMB;

	@Value("${file.path.property}")
	private String filePath;

	private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadController.class);

	@Value("${platform.server}")
	private String platformServer;

	// @Value("${platform.fileupload.post.url}")
	// private String fileUploadPostUrl;

	@Autowired
	private RestTemplate restTemplate;

	@RequestMapping(value = "upload", method = RequestMethod.POST)
	public ResponseEntity<Response> upload(final @RequestParam("file") MultipartFile file,
			final @RequestParam("lookupCode") String lookupCode, final @RequestParam("value") String value,
			final HttpServletRequest request) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Start :{}", file.getName());
		}

		Response response = new Response(Constants.OK_STATUS);
		File uploadedFile = null;
		HttpHeaders headers = setHeaders(request);

		HttpEntity<?> requestEntity = new HttpEntity<>(headers);
		try {
			
			Map<String, Object> lookupMap = this.getLookupMap(lookupCode, value, requestEntity);

			this.validateFileExtension(file, lookupMap);
			//uploadedFile = new File(lookupMap.get(filePath).toString());
			//file.transferTo(uploadedFile);
			//this.validateFileSize(uploadedFile, lookupMap);
                        uploadedFile = saveUploadedFileToPath(file, lookupMap.get(filePath).toString());
			response.setDescription("File uploaded successfully.");

		} catch (InvalidFileException e) {
			response.setDescription(e.getMessage());
			response.setStatus("400");
			if (uploadedFile != null)
				uploadedFile.delete();
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("InvalidFileException while uploading file :{}", e);
			}
		} catch (Exception e) {
			response.setDescription(e.getMessage());
			response.setStatus("500");
			if (uploadedFile != null)
				uploadedFile.delete();

			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Error while uploading file :{}", e);
			}

		} finally {
			// Store the record in fileUpload Collection
			if (response.getStatus().equalsIgnoreCase(Constants.OK_STATUS)) {
				this.insertFileRecord(file, value, request, response);
			}
		}
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	/**
	 * 
	 * @param file
	 * @param value
	 * @param request
	 * @param response
	 */
	private void insertFileRecord(final MultipartFile file, final String value, final HttpServletRequest request,
			Response response) {
		FileUpload fileUpload = new FileUpload();
		fileUpload.setFileName(file.getOriginalFilename());
		fileUpload.setEnablFlag(Constants.TRUE);
		fileUpload.setFileIdentification(value);
		fileUpload.setFileUploadStatus(Constants.INITIALIZED);
		fileUpload.setTenantId(request.getHeader(Constants.TENANT_ID));
		fileUpload.setCreatdDate(new Date());
		fileUpload.setLastUpdatedDate(new Date());
		fileUpload.setCreatedBy(request.getHeader(Constants.USER));
		LOGGER.info("FileUpload before saving/posting object is :{}", fileUpload);
		try {
			Response fileUploadResponse = restTemplate
					.postForObject(String.format("%s/api/fileUpload/", platformServer), fileUpload, Response.class);
			LOGGER.debug("FileUpload saved response status :{}", fileUploadResponse);
		} catch (Exception rce) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Error while inserting fileUpload object :{}", rce);
			}
			response.setDescription(rce.getMessage());
			response.setStatus("500");
		}
	}

	/**
	 * 
	 * @param lookupCode
	 * @param value
	 * @param entity
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> getLookupMap(final String lookupCode, final String value, HttpEntity<HttpHeaders> requestEntity) {
		
                Map<String, Object> tmpMap;
                final String url = platformServer+"/api/lookup?lookupCode="+lookupCode+"&value="+value;
                ResponseEntity<Response> resp = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Response.class);

		ArrayList<HashMap<String, Object>> obj = (ArrayList<HashMap<String, Object>>) resp.getBody().getData();
		tmpMap = obj.get(0);

		return tmpMap;
	}

	/**
	 * 
	 * @param request
	 * @param headers
	 * @return
	 */
	private HttpHeaders setHeaders(final HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
		headers.set(Constants.TENANT_ID, request.getHeader(Constants.TENANT_ID));
		headers.set(Constants.ACCESS_LEVEL, request.getHeader(Constants.ACCESS_LEVEL));
		headers.set(Constants.ENTITY_ID, request.getHeader(Constants.ENTITY_ID));
		headers.set(Constants.USER, request.getHeader(Constants.USER));
		headers.set(Constants.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		return headers;
	}

	/**
	 * 
	 * @param uploadedFile
	 * @param lookupMap
	 * @throws InvalidFileException
	 */
	private void validateFileSize(File uploadedFile, Map<String, Object> lookupMap) throws InvalidFileException {
		boolean isFileSizeExceeded = this.validateFileSize(uploadedFile,
				Integer.valueOf(lookupMap.get(fileSizeInMB).toString()));

		if (!isFileSizeExceeded) {
			throw new InvalidFileException(
					String.format("File Size cannot exceed %sMB", lookupMap.get(fileSizeInMB).toString()));
		}
	}

	/**
	 * 
	 * @param file
	 * @param lookupMap
	 * @throws InvalidFileException
	 */
	private void validateFileExtension(final MultipartFile file, Map<String, Object> lookupMap)
			throws InvalidFileException {
		// valueTwo to check extension
		boolean isExtensionValid = this.validateExtension(file.getOriginalFilename(),
				lookupMap.get(fileExtension).toString());
		if (!isExtensionValid) {
			throw new InvalidFileException(
					String.format("File Extension %s not supported", lookupMap.get(fileExtension).toString()));
		}
	}

	/**
	 * 
	 * @param file
	 * @param maxSizeInMB
	 * @return
	 */
	private boolean validateFileSize(File file, Integer maxSizeInMB) {
		return this.getFileSizeMegaBytes(file) > maxSizeInMB;

	}

	/**
	 * 
	 * @param fileName
	 * @param extension
	 * @return
	 */
	private boolean validateExtension(String fileName, String extension) {
		//return fileName.substring(fileName.lastIndexOf(Constants.DOT)).equalsIgnoreCase(extension);
                return fileName.contains(extension);

	}

	/**
	 * 
	 * @param file
	 * @return
	 */
	private double getFileSizeMegaBytes(File file) {
		return (double) file.length() / (1024 * 1024);
	}

        /**
	 *
	 * @param multipartfile
	 * @param targetFolderPath
	 * @return sourceFile
	 */

	private File saveUploadedFileToPath(MultipartFile multipartfile, String targetFolderPath) {
		File sourceFile = null;
		try {
			String fileName = multipartfile.getOriginalFilename();
			LOGGER.debug("FileName uploaded {}", fileName);

			File sourceDirectory = new File(targetFolderPath);
			if (!sourceDirectory.exists()) {
				sourceDirectory.mkdirs();
			}
			sourceFile = new File(sourceDirectory + File.separator + fileName);
			File tmpDirectory = new File(File.separator + "tmp" + File.separator + sourceDirectory);
			if (!tmpDirectory.exists()) {
				tmpDirectory.mkdirs();
			}
			File tmpFile = new File(
					File.separator + "tmp" + File.separator + sourceDirectory + File.separator + fileName);
			boolean isCreated = tmpFile.createNewFile();
			if (isCreated) {
				multipartfile.transferTo(sourceFile);
			} else {
				throw new FileNotFoundException(
						"Unable to Create File " + fileName + " in directory " + sourceDirectory);
			}
			tmpFile.renameTo(sourceFile);
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("Exception in saveUploadedFile To Path: {} ", e);
			}
		}
		return sourceFile;
	}

}
