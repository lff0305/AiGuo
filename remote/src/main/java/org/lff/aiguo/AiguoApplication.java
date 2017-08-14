package org.lff.aiguo;

import org.apache.catalina.core.AprLifecycleListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AiguoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiguoApplication.class, args);
	}

	AprLifecycleListener arpLifecycle = null;

	@Value("${server.apr:false}")
	private boolean enableApr;

	@Bean
	public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
		if (enableApr) {
			arpLifecycle = new AprLifecycleListener();
			tomcat.setProtocol("org.apache.coyote.http11.Http11AprProtocol");
		}
		return tomcat;
	}
}
