package com.evolvus.sandstorm.csv.bean;



import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

import lombok.Data;

@CsvRecord(separator = ",", skipFirstLine = false)
@Data
public class UserBean {

	@DataField(pos = 1)
	private String userId;

	@DataField(pos = 2)
	private String userName;

	@DataField(pos = 3)
	private String designation;

	@DataField(pos = 4)
	private String role;

	@DataField(pos = 5)
	private String phoneNumber;

	@DataField(pos = 6)
	private String mobileNumber;

	@DataField(pos = 7)
	private String emailId;

	@DataField(pos = 8)
	private String fax;

	@DataField(pos = 9)
	private String country;

	@DataField(pos = 10)
	private String state;

	@DataField(pos = 11)
	private String city;

	@DataField(pos = 12)
	private String masterTimeZone;
	
	private String entityId;

	@DataField(pos = 13)
	private Integer individualTransactionLimit;

	@DataField(pos = 14)
	private Integer dailyLimit;

	@DataField(pos = 15)
	private String masterCurrency;

	@Override
	public String toString() {
		return String.format(
				"%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
				userId, userName, designation, role, phoneNumber, mobileNumber, emailId, fax, country, state, city,
				masterTimeZone, masterCurrency, individualTransactionLimit, dailyLimit, masterCurrency);
	}

}
