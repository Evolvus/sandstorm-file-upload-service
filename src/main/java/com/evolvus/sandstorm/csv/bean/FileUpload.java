package com.evolvus.sandstorm.csv.bean;

import java.util.Date;

import lombok.Data;

@Data
public class FileUpload {

	private String tenantId;
	
	private String entityId;

	private String wfInstanceId;

	private String processingStatus;

	private String fileIdentification;

	private String fileName;

	private String fileType;

	private String fileUploadStatus;

	private String totalTransaction;

	private String count;

	private String totalProcessedCount;

	private String totalFailedCount;

	private String uploadedBy;

	private String successLog;

	private String errorLog;

	private String uploaDateAndTime;

	private String enablFlag = "true";

	private String createdBy;

	private String updatedBy;

	private Date creatdDate;

	private Date lastUpdatedDate;

}
