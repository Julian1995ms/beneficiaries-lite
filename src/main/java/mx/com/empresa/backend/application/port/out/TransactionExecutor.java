package mx.com.empresa.backend.application.port.out;

import java.util.function.Supplier;

public interface TransactionExecutor {

    <T> T execute(Supplier<T> action);
}
