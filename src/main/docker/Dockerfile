FROM halcom/oracle-jdk8:151
VOLUME /tmp
ADD sandstorm-file-upload-service-*.jar sandstorm-file-upload-service.jar
RUN sh -c 'touch /sandstorm-file-upload-service.jar'
ENV JAVA_OPTS="-Xmx512m -Xss256k"
EXPOSE 8092
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Djavax.net.ssl.keyStore=/root/Documents/CDA/dockerFiles/cert.ks -Djavax.net.ssl.keyStorePassword=indiabulls -Djavax.net.ssl.trustStore=/root/Documents/CDA/dockerFiles/cert.ks -Djavax.net.ssl.trustStorePassword=indiabulls -jar /sandstorm-file-upload-service.jar --spring.config.location=/tmp/application-property-files/sandstorm-file-upload-service/ --endpoints.info.enabled=true --info.app.version=@project.version@" ]
