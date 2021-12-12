package me.liuweiqiang.idempotent.component;

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

    @Transactional
    public String process() {
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
        //do something one transaction
        RequestExample updateExample = new RequestExample();
        RequestExample.Criteria updateCriteria = updateExample.createCriteria();
        updateCriteria.andConsumerEqualTo(CONSUMER);
        updateCriteria.andRequestIdEqualTo(REQUEST_ID);
        Request update = new Request();
        update.setStatus(DONE);
        requestDAO.updateByExampleSelective(update, updateExample);
        return DONE;
    }
}
