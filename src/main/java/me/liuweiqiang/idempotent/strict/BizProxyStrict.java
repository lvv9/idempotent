package me.liuweiqiang.idempotent.strict;

import me.liuweiqiang.idempotent.BizException;
import me.liuweiqiang.idempotent.UnknownException;
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
public class BizProxyStrict {

    private static final String DONE = "0000";
    private static final String SOME_BIZ_CODE = "2222";

    @Autowired
    private RequestDAO requestDAO;
    @Autowired
    private NewTableDAO newTableDAO;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String process(String consumer, String reqId, String req) throws UnknownException {
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
        request.setStatus(DONE);
        try {
            requestDAO.insertSelective(request);
        } catch (Exception e) { //DuplicateKeyException
            throw new UnknownException(e); //to rollback
        }
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
    }

    public void failCommit(String req) {
        NewTable test = new NewTable();
        test.setForTest(req);
        int count = newTableDAO.insertSelective(test);
    }
}
