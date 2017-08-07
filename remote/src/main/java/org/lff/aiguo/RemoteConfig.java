package org.lff.aiguo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

/**
 * @author Feifei Liu
 * @datetime Aug 07 2017 10:35
 */
@Component
@PropertySource("classpath:application.properties")
public class RemoteConfig {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public boolean isManualDelay() {
        return manualDelay;
    }

    public void setManualDelay(boolean manualDelay) {
        this.manualDelay = manualDelay;
    }

    @Value("${manualDelay}")
    private boolean manualDelay;

    @PostConstruct
    public void init() {
        logger.info("Config : manualDelay = {}", manualDelay);
    }

}
