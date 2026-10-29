package app.keystone.infrastructure.config;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;

/** Registers MyBatis' reflective SLF4J logger constructor for native images. */
public final class MyBatisRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        try {
            hints.reflection().registerConstructor(Slf4jImpl.class.getDeclaredConstructor(String.class), ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("MyBatis SLF4J logger constructor is unavailable", ex);
        }
    }
}
