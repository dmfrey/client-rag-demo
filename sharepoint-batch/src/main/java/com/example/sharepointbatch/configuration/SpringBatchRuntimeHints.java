package com.example.sharepointbatch.configuration;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.batch.core.scope.context.StepContext;

// ChangedItemReader is @StepScope with a #{jobExecutionContext['...']} SpEL binding (see that
// class), which Spring Batch's StepScope.resolveContextualObject resolves by reflectively
// invoking StepContext.getJobExecutionContext() via a bean-property accessor - a real, live
// crash on a real deploy: "Cannot reflectively invoke method
// 'public java.util.Map org.springframework.batch.core.scope.context.StepContext
// .getJobExecutionContext()'". Spring Batch's own bundled native-image metadata doesn't cover
// this specific method. Registers the whole StepContext class (small, few members) rather than
// just this one method, in case other step-scoped SpEL bindings elsewhere touch a different
// accessor later.
public class SpringBatchRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(StepContext.class,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.ACCESS_DECLARED_FIELDS);
    }
}
