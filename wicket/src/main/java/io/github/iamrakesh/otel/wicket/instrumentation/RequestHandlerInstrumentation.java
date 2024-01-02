
package io.github.iamrakesh.otel.wicket.instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import org.apache.wicket.request.IRequestHandler;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class RequestHandlerInstrumentation implements TypeInstrumentation {

	@Override
	public ElementMatcher<TypeDescription> typeMatcher() {
		return named("org.apache.wicket.request.RequestHandlerExecutor");
	}

	@Override
	public void transform(TypeTransformer transformer) {
		transformer.applyAdviceToMethod(
				named("execute").and(takesArgument(0, named("org.apache.wicket.request.IRequestHandler"))),
				RequestHandlerInstrumentation.class.getName() + "$ExecuteAdvice");
	}

	public static class ExecuteAdvice {
		@Advice.OnMethodExit(suppress = Throwable.class)
		public static void onExit(@Advice.Argument(0) IRequestHandler handler) {
			Span rootSpan = LocalRootSpan.fromContextOrNull(Java8BytecodeBridge.currentContext());
			System.err.println("onExit() - rootSpan -> " + rootSpan);
			if (rootSpan == null) {
				return;
			}
			System.err.println("onExit() - rootSpan -> WORKS");
			rootSpan.setAttribute("ra.custom.attribute", "Extension works");
		}
	}
}
