package me.liuweiqiang.idempotent.strict;

import me.liuweiqiang.idempotent.UnknownException;
import me.liuweiqiang.idempotent.aop.ExceptionAOP;
import me.liuweiqiang.idempotent.dao.NewTableDAO;
import me.liuweiqiang.idempotent.dao.RequestDAO;
import me.liuweiqiang.idempotent.dao.model.NewTable;
import me.liuweiqiang.idempotent.dao.model.NewTableExample;
import me.liuweiqiang.idempotent.dao.model.Request;
import me.liuweiqiang.idempotent.dao.model.RequestExample;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class IdempotentTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CONSUMER = "test";
    private static final String REQUEST_ID = "testReqId";
    private static final String REQ = "testContent";
    private static final String DONE = "0000";
    private static final String PROCESSING = "1111";
    private static final String SOME_BIZ_CODE = "2222";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private IdempotentStrict idempotentStrict;
    @Autowired
    private NewTableDAO newTableDAO;

    @Test
    public void testNestedSuccess() throws Exception {
        String ret = idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, REQ);
        Assertions.assertEquals(DONE, ret);

        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andRequestIdEqualTo(REQUEST_ID);
        criteria.andConsumerEqualTo(CONSUMER);
        List<Request> requestList = requestDAO.selectByExample(requestExample);

        NewTableExample newTableExample = new NewTableExample();
        NewTableExample.Criteria newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo(REQ);
        List<NewTable> newTables = newTableDAO.selectByExample(newTableExample);

        Assertions.assertEquals(REQ, newTables.get(0).getForTest());
        Assertions.assertEquals(DONE, requestList.get(0).getStatus());

        newTableDAO.deleteByPrimaryKey(newTables.get(0).getId());
        requestDAO.deleteByExample(requestExample);

    }

    @Test
    public void testNestedIdempotentSuccess() throws Exception {
        idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, REQ);
        String ret = idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, REQ);
        Assertions.assertEquals(DONE, ret);

        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andRequestIdEqualTo(REQUEST_ID);
        criteria.andConsumerEqualTo(CONSUMER);
        List<Request> requestList = requestDAO.selectByExample(requestExample);
        Assertions.assertEquals(DONE, requestList.get(0).getStatus());

        NewTableExample newTableExample = new NewTableExample();
        NewTableExample.Criteria newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo(REQ);
        List<NewTable> newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertEquals(REQ, newTables.get(0).getForTest());

        requestDAO.deleteByExample(requestExample);
        newTableDAO.deleteByPrimaryKey(newTables.get(0).getId());
    }

    @Test
    public void testNestedFail() throws Exception {
        String ret = idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, "");
        Assertions.assertEquals(SOME_BIZ_CODE, ret);

        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andRequestIdEqualTo(REQUEST_ID);
        criteria.andConsumerEqualTo(CONSUMER);
        List<Request> requestList = requestDAO.selectByExample(requestExample);
        Assertions.assertEquals(SOME_BIZ_CODE, requestList.get(0).getStatus());

        NewTableExample newTableExample = new NewTableExample();
        NewTableExample.Criteria newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo("");
        List<NewTable> newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertTrue(newTables.isEmpty());

        newTableExample = new NewTableExample();
        newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo("fail");
        newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertFalse(newTables.isEmpty());

        requestDAO.deleteByExample(requestExample);
        newTableDAO.deleteByPrimaryKey(newTables.get(0).getId());
    }

    @Test
    public void testNestedIdempotentFail() throws Exception {
        idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, "");
        String ret = idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, "");
        Assertions.assertEquals(SOME_BIZ_CODE, ret);

        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andRequestIdEqualTo(REQUEST_ID);
        criteria.andConsumerEqualTo(CONSUMER);
        List<Request> requestList = requestDAO.selectByExample(requestExample);
        Assertions.assertEquals(SOME_BIZ_CODE, requestList.get(0).getStatus());

        NewTableExample newTableExample = new NewTableExample();
        NewTableExample.Criteria newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo("");
        List<NewTable> newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertTrue(newTables.isEmpty());

        newTableExample = new NewTableExample();
        newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo("fail");
        newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertFalse(newTables.isEmpty());

        requestDAO.deleteByExample(requestExample);
        newTableDAO.deleteByPrimaryKey(newTables.get(0).getId());
    }

    @Test
    public void testRequiredAfterRequiresNewThrow() {
        String ret = idempotentStrict.requiredProcessing(CONSUMER, REQUEST_ID, ExceptionAOP.THROW_EXCEPTION);
        Assertions.assertEquals(PROCESSING, ret);

        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andRequestIdEqualTo(REQUEST_ID);
        criteria.andConsumerEqualTo(CONSUMER);
        List<Request> requestList = requestDAO.selectByExample(requestExample);
        Assertions.assertEquals(DONE, requestList.get(0).getStatus());

        NewTableExample newTableExample = new NewTableExample();
        NewTableExample.Criteria newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestEqualTo(ExceptionAOP.THROW_EXCEPTION);
        List<NewTable> newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertEquals(ExceptionAOP.THROW_EXCEPTION, newTables.get(0).getForTest());

        requestDAO.deleteByExample(requestExample);
        newTableDAO.deleteByPrimaryKey(newTables.get(0).getId());
    }

    @Test
    public void testRequiredAfterNestedThrow() {
        try {
            idempotentStrict.nestedProcessing(CONSUMER, REQUEST_ID, ExceptionAOP.THROW_EXCEPTION);
            Assertions.fail("no exception thrown");
        } catch (UnknownException e) {
            logger.info("processing", e);
        }

        RequestExample requestExample = new RequestExample();
        RequestExample.Criteria criteria = requestExample.createCriteria();
        criteria.andRequestIdEqualTo(REQUEST_ID);
        criteria.andConsumerEqualTo(CONSUMER);
        List<Request> requestList = requestDAO.selectByExample(requestExample);
        Assertions.assertTrue(requestList.isEmpty());

        NewTableExample newTableExample = new NewTableExample();
        NewTableExample.Criteria newTableCriteria = newTableExample.createCriteria();
        newTableCriteria.andForTestIn(Arrays.asList(ExceptionAOP.THROW_EXCEPTION, "fail"));
        List<NewTable> newTables = newTableDAO.selectByExample(newTableExample);
        Assertions.assertTrue(newTables.isEmpty());
    }

}