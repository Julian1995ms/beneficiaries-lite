package mx.com.empresa.backend.infrastructure.config;

import java.util.function.Supplier;

import mx.com.empresa.backend.application.port.out.TransactionExecutor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class JakartaTransactionExecutor implements TransactionExecutor {

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public <T> T execute(Supplier<T> action) {
        return action.get();
    }
}
