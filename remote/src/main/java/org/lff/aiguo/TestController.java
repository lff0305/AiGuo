package org.lff.aiguo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Created by liuff on 2017/8/12 22:03
 */
@Service
@RequestMapping("/test")
public class TestController {

    static StringBuffer s = new StringBuffer();
    static {
        for (int i = 0; i < 10 * 1024 * 1024; i++) {
            s.append(String.valueOf(i % 10));
        }
    }

    static byte[] b = s.toString().getBytes();

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @RequestMapping("/f")
    public @ResponseBody String t(HttpServletResponse response) {
        try {
            long l0 = System.currentTimeMillis();
            logger.info("Start to write");
            logger.info("Finished to write {}", System.currentTimeMillis() - l0);
            return s.toString();
        } finally {
            logger.info("Send test data ok");
        }
    }
}
