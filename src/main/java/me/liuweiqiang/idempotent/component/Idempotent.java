package me.liuweiqiang.idempotent.component;

import me.liuweiqiang.idempotent.BizException;
import me.liuweiqiang.idempotent.dao.RequestDAO;
import me.liuweiqiang.idempotent.dao.model.Request;
import me.liuweiqiang.idempotent.dao.model.RequestExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class Idempotent {

    private static final String DONE = "0000";
    private static final String PROCESSING = "1111";
    private static final String CONSUMER = "test";
    private static final String REQUEST_ID = "testReqId";

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private BizProxy bizProxy;

    @Transactional
    public String requiresNewProcessing(String req) {
        String response;
        try {
            response = bizProxy.init(CONSUMER, REQUEST_ID);
        } catch (Exception e) {
            return PROCESSING;
        }
        if (response != null) {
            return response;
        }
        try {
            bizProxy.processOnNew(CONSUMER, REQUEST_ID, req);
        } catch (BizException e) {
            if (!PROCESSING.equals(e.getResponseCode())) {
                int count = refreshCode(e.getResponseCode());
                if (count < 1) {
                    return PROCESSING;
                }
            }
            return e.getResponseCode();
        } catch (Exception e) {
            return PROCESSING;
        }
        return DONE;
    }

    private int refreshCode(String code) {
        RequestExample updateExample = new RequestExample();
        RequestExample.Criteria updateCriteria = updateExample.createCriteria();
        updateCriteria.andConsumerEqualTo(CONSUMER);
        updateCriteria.andRequestIdEqualTo(REQUEST_ID);
        updateCriteria.andStatusEqualTo(PROCESSING);
        Request update = new Request();
        update.setStatus(code);
        int count;
        try {
            count = requestDAO.updateByExampleSelective(update, updateExample);
        } catch (Exception e) {
            //rollback when processing had unknown exception
            throw new BizException(e, PROCESSING);
        }
        return count;

    }

    @Transactional
    public String nestedProcessing(String req) {
        RequestExample selectExample = new RequestExample();
        RequestExample.Criteria selectCriteria = selectExample.createCriteria();
        selectCriteria.andConsumerEqualTo(CONSUMER);
        selectCriteria.andRequestIdEqualTo(REQUEST_ID);
        List<Request> requests = requestDAO.selectByExample(selectExample);
        if (requests != null && !requests.isEmpty()) {
            return requests.get(0).getStatus();
        }
        Request request = new Request();
        request.setConsumer(CONSUMER);
        request.setRequestId(REQUEST_ID);
        try {
            requestDAO.insertSelective(request);
        } catch (Exception e) { //DuplicateKeyException
            return PROCESSING;
        }
        String responseCode = DONE;
        try {
            bizProxy.processOnNest(req);
        } catch (BizException e) {
            responseCode = e.getResponseCode();
        } catch (Exception e) {
            //rollback when processing had unknown exception
            throw new BizException(e, PROCESSING);
        }
        RequestExample updateExample = new RequestExample();
        RequestExample.Criteria updateCriteria = updateExample.createCriteria();
        updateCriteria.andConsumerEqualTo(CONSUMER);
        updateCriteria.andRequestIdEqualTo(REQUEST_ID);
        Request update = new Request();
        update.setStatus(responseCode);
        requestDAO.updateByExampleSelective(update, updateExample);
        return responseCode;
    }
}
