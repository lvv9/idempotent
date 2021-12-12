package me.liuweiqiang.idempotent.dao;

import me.liuweiqiang.idempotent.component.Idempotent;
import me.liuweiqiang.idempotent.dao.model.RequestExample;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IdempotentTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private Idempotent idempotent;

    @Test
    public void init() {
        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andConsumerEqualTo("test");
        requestDAO.selectByExample(requestExample);
    }

    @Test
    public void testNestedProcessing() {
        logger.info(idempotent.nestedProcessing(""));
    }

    @Test
    public void testRequiresNewProcessing() {
        logger.info(idempotent.requiresNewProcessing(""));
    }

}