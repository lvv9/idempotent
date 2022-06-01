package me.liuweiqiang.idempotent.strict;

import me.liuweiqiang.idempotent.BizException;
import me.liuweiqiang.idempotent.UnknownException;
import me.liuweiqiang.idempotent.dao.RequestDAO;
import me.liuweiqiang.idempotent.dao.model.Request;
import me.liuweiqiang.idempotent.dao.model.RequestExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class IdempotentStrict {

    private static final String DONE = "0000";
    private static final String PROCESSING = "1111";

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private BizProxyStrict bizProxyStrict;

    @Transactional
    public String requiredProcessing(String consumer, String reqId, String req) {
        try {
            return bizProxyStrict.process(consumer, reqId, req);
        } catch (BizException e) {
            return logFailure(consumer, reqId, e.getResponseCode());
        } catch (Exception e) {
            return PROCESSING;
        }
    }

    private String logFailure(String consumer, String reqId, String code) {
        Request request = new Request();
        request.setConsumer(consumer);
        request.setRequestId(reqId);
        request.setStatus(code);
        try {
            requestDAO.insertSelective(request);
            return code;
        } catch (Exception e) {
            return PROCESSING;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public String nestedProcessing(String consumer, String reqId, String req) throws UnknownException {
        RequestExample selectExample = new RequestExample();
        RequestExample.Criteria selectCriteria = selectExample.createCriteria();
        selectCriteria.andConsumerEqualTo(consumer);
        selectCriteria.andRequestIdEqualTo(reqId);
        List<Request> requests = requestDAO.selectByExample(selectExample);
        if (requests != null && !requests.isEmpty()) {
            return requests.get(0).getStatus();
        }
        Request request = new Request();
        request.setConsumer(consumer);
        request.setRequestId(reqId);
        try {
            requestDAO.insertSelective(request);
        } catch (Exception e) { //DuplicateKeyException
            throw new UnknownException(e); //to rollback
        }
        String responseCode = DONE;
        try {
            bizProxyStrict.processOnNest(req);
        } catch (BizException e) {
            responseCode = e.getResponseCode();
        } catch (Exception e) {
            //rollback when processing encountered an unknown exception
            throw new UnknownException(e);
        }
        RequestExample updateExample = new RequestExample();
        RequestExample.Criteria updateCriteria = updateExample.createCriteria();
        updateCriteria.andConsumerEqualTo(consumer);
        updateCriteria.andRequestIdEqualTo(reqId);
        Request update = new Request();
        update.setStatus(responseCode);
        requestDAO.updateByExampleSelective(update, updateExample);
        return responseCode;
    }
}
