package me.liuweiqiang.idempotent.component;

import me.liuweiqiang.idempotent.BizException;
import me.liuweiqiang.idempotent.dao.NewTableDAO;
import me.liuweiqiang.idempotent.dao.RequestDAO;
import me.liuweiqiang.idempotent.dao.model.NewTable;
import me.liuweiqiang.idempotent.dao.model.Request;
import me.liuweiqiang.idempotent.dao.model.RequestExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BizProxy {

    private static final String DONE = "0000";
    private static final String SOME_BIZ_CODE = "2222";
    private static final String SOME_BIZ_CODE_NEXT = "3333";

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private NewTableDAO newTableDAO;

    @Transactional
    public String process(String consumer, String reqId, String req) {
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
        this.internalProcessing(req);
        return DONE;
    }

    @Transactional(propagation = Propagation.NESTED)
    public void processOnNest(String req) {
        this.internalProcessing(req);
    }

    private void internalProcessing(String req) {
        NewTable test = new NewTable();
        test.setForTest(req);
        int count = newTableDAO.insertSelective(test);
        if ("".equals(req)) {
            throw new BizException(SOME_BIZ_CODE);
        }
        if (" ".equals(req)) {
            throw new BizException(SOME_BIZ_CODE_NEXT);
        }
    }
}
