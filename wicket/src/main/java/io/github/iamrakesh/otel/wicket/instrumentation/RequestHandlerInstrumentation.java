
package io.github.iamrakesh.otel.wicket.instrumentation;

import static io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRouteSource.CONTROLLER;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerRouteGetter;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.bootstrap.servlet.ServletContextPath;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;

public class RequestHandlerInstrumentation implements TypeInstrumentation {

	public static final HttpServerRouteGetter<IPageRequestHandler> SERVER_SPAN_NAME = (context, handler) -> {
		System.err.println("onExit() - serverSpanNameGetter : 1 : -> ");
		// using class name as page name
		String pageName = handler.getPageClass().getName();
		// wicket filter mapping without wildcard, if wicket filter is mapped to /*
		// this will be an empty string
		String filterPath = RequestCycle.get().getRequest().getFilterPath();

		System.err.println("onExit() - serverSpanNameGetter : 2 : -> " + pageName + " :: " + filterPath);
		return ServletContextPath.prepend(context, filterPath + "/" + pageName + ":OtelExtension");
	};

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
			System.err.println("onExit() - handler instanceof IPageRequestHandler === "
					+ (handler instanceof IPageRequestHandler));

			if (handler instanceof IPageRequestHandler) {
				HttpServerRoute.update(Java8BytecodeBridge.currentContext(), CONTROLLER, SERVER_SPAN_NAME,
						((IPageRequestHandler) handler));
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
}
