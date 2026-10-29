package com.nz.admin.modules.system.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;

/** 安全复查只看已提交状态，避免发布线程的业务回滚提前撤销长连接。 */
@Component
class CommittedSecurityReader {
    private final TransactionTemplate transactions;

    CommittedSecurityReader(PlatformTransactionManager manager) {
        transactions = new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactions.setReadOnly(true);
    }

    <T> T read(Supplier<T> query) {
        return transactions.execute(status -> query.get());
    }
}
