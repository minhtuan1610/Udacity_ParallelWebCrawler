package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

	private final Clock clock;

	// TODO: You will need to add more instance fields and constructor arguments to this class.
	private final Object delegate;
	private final ProfilingState profilingState;
	private final ZonedDateTime time;

	ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState profilingState, ZonedDateTime time) {
		this.clock = Objects.requireNonNull(clock);
		this.delegate = delegate;
		this.profilingState = profilingState;
		this.time = time;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// TODO: This method interceptor should inspect the called method to see if it is a profiled
		//       method. For profiled methods, the interceptor should record the start time, then
		//       invoke the method using the object that is being profiled. Finally, for profiled
		//       methods, the interceptor should record how long the method call took, using the
		//       ProfilingState methods.
		Object invokedObj = new Object();

		Instant startInstant;

		boolean profiled = method.getAnnotation(Profiled.class) != null;

		startInstant = profiled ? clock.instant() : null;

		try {
			invokedObj = method.invoke(delegate, args);
		} catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(); // To ensure that proxy does not throw UndeclaredThrowableException
		} finally {
			if (profiled) {
				Duration duration = Duration.between(startInstant, clock.instant());
				profilingState.record(delegate.getClass(), method, duration);
			}
		}

		return invokedObj;
	}
}
