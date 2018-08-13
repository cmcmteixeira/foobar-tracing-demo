package com.example.tracing.logback
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import kamon.Kamon
import kamon.trace.{IdentityProvider, Span}

class LogbackSpanIDConverter extends ClassicConverter {

  override def convert(event: ILoggingEvent): String = {
    val currentSpan = Kamon.currentContext().get(Span.ContextKey)
    val spanID      = currentSpan.context().spanID

    if (spanID == IdentityProvider.NoIdentifier)
      "undefined"
    else
      spanID.string
  }

}
