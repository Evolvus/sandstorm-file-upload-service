/**
 * 
 */
package com.evolvus.sandstorm.camel.route;

import org.apache.camel.builder.ErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spi.DataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author EVOLVUS\shrimank
 * 
 * 
 */
@Configuration
public class CamelRouteBuilder extends RouteBuilder {
	
	ErrorHandlerBuilder errorHandlerBuilder;

	@Value("${SANDSTORM_HOME}")
	private String sandStormHome;

	@Autowired
	private UserUploadProcessor userProcessor;
	
	@Autowired
	private UserUploadPreProcessor userPreProcessor;


	@Override
	public void configure() throws Exception {

		DataFormat bindy = new BindyCsvDataFormat(com.evolvus.sandstorm.csv.bean.UserBean.class);
		from("file://"+sandStormHome+"/UPLOAD/USER" ).process(userPreProcessor).choice().when().simple("${header.fileStatus} != 'ERROR'").unmarshal(bindy).routeId("USER_UPLOAD_ROUTE")
				.process(userProcessor);

	}

}
