package me.liuweiqiang.idempotent.component;

import me.liuweiqiang.idempotent.BizException;
import me.liuweiqiang.idempotent.dao.RequestDAO;
import me.liuweiqiang.idempotent.dao.model.Request;
import me.liuweiqiang.idempotent.dao.model.RequestExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BizProxy {

    private static final String CONSUMER = "test";
    private static final String REQUEST_ID = "testReqId";
    private static final String DONE = "0000";
    private static final String PROCESSING = "1111";
    private static final String SOME_BIZ_CODE = "2222";

    @Autowired
    private RequestDAO requestDAO;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    //materialize for locking
    public String init(String consumer, String reqId) {
        RequestExample selectExample = new RequestExample();
        RequestExample.Criteria selectCriteria = selectExample.createCriteria();
        selectCriteria.andConsumerEqualTo(consumer);
        selectCriteria.andRequestIdEqualTo(reqId);
        List<Request> requests = requestDAO.selectByExample(selectExample);
        String result;
        if (requests != null && !requests.isEmpty() && !PROCESSING.equals((result = requests.get(0).getStatus()))) {
            return result;
        }
        if (requests == null || requests.isEmpty()) {
            Request request = new Request();
            request.setConsumer(consumer);
            request.setRequestId(reqId);
            request.setStatus(PROCESSING);
            try {
                requestDAO.insertSelective(request);
            } catch (Exception e) { //DuplicateKeyException
                throw new BizException(e, PROCESSING);
            }
        }
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOnNew(String consumer, String reqId, String req) {
        RequestExample updateExample = new RequestExample();
        RequestExample.Criteria updateCriteria = updateExample.createCriteria();
        updateCriteria.andConsumerEqualTo(consumer);
        updateCriteria.andRequestIdEqualTo(reqId);
        updateCriteria.andStatusEqualTo(PROCESSING);
        Request update = new Request();
        update.setStatus(DONE);
        int count;
        try {
            count = requestDAO.updateByExampleSelective(update, updateExample);
        } catch (Exception e) {
            throw new BizException(e, PROCESSING);
        }
        if (count < 1) {
            throw new BizException(PROCESSING);
        }
        internalProcessing(req);
    }

    @Transactional(propagation = Propagation.NESTED)
    public void processOnNest(String req) {
        internalProcessing(req);
    }

    private void internalProcessing(String req) {
        if ("".equals(req)) {
            throw new BizException(SOME_BIZ_CODE);
        }
    }
}
