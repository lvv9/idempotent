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

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private BizProxy bizProxy;

    public String requiredProcessing(String consumer, String reqId, String req) {
        try {
            return bizProxy.process(consumer, reqId, req);
        } catch (BizException e) {
            return e.getResponseCode();
        } catch (Exception e) {
            return PROCESSING;
        }
    }

    @Transactional
    public String nestedProcessing(String consumer, String reqId, String req) {
        RequestExample selectExample = new RequestExample();
        RequestExample.Criteria selectCriteria = selectExample.createCriteria();
        selectCriteria.andConsumerEqualTo(consumer);
        selectCriteria.andRequestIdEqualTo(reqId);
        List<Request> requests = requestDAO.selectByExample(selectExample);
        if (requests != null && !requests.isEmpty() && DONE.equals(requests.get(0).getStatus())) {
            return requests.get(0).getStatus();
        }
        Request request = new Request();
        request.setConsumer(consumer);
        request.setRequestId(reqId);
        request.setStatus(DONE);
        requestDAO.insertSelective(request);
        bizProxy.processOnNest(req);
        return DONE;
    }
}
