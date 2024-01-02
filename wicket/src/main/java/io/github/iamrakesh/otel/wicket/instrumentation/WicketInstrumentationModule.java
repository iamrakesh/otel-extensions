
package io.github.iamrakesh.otel.wicket.instrumentation;

import java.util.Collections;
import java.util.List;

import com.google.auto.service.AutoService;

import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * This is a demo instrumentation which hooks into servlet invocation and
 * modifies the http response.
 */
@AutoService(InstrumentationModule.class)
public final class WicketInstrumentationModule extends InstrumentationModule {

	public WicketInstrumentationModule() {
		super("wicket-requests", "request-handler");
	}

	/*
	 * We want this instrumentation to be applied after the standard servlet
	 * instrumentation. The latter creates a server span around http request. This
	 * instrumentation needs access to that server span.
	 */
	@Override
	public int order() {
		return 1;
	}

	@Override
	public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
		return AgentElementMatchers.hasClassesNamed("org.apache.wicket.request.RequestHandlerExecutor");
	}

	@Override
	public List<TypeInstrumentation> typeInstrumentations() {
		return Collections.singletonList(new RequestHandlerInstrumentation());
	}

	@Override
	public boolean isHelperClass(String className) {
		return className.startsWith("io.github.iamrakesh.otel.wicket.instrumentation");
	}
}
