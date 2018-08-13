package com.example.tracing.operation
import java.util.UUID

import kamon.http4s.NameGenerator
import org.http4s.{Request, Uri}

import scala.language.higherKinds

class UUIDNameGenerator extends NameGenerator {

  import java.util.Locale
  private val normalizePattern = """\$([^<]+)<[^>]+>""".r
  private val regex            = "\\/(\\w+-){4}\\w+".r

  override def generateHttpClientOperationName[F[_]](request: Request[F]): String =
    s"${request.uri.authority.getOrElse("_")}.${getNormalizedPath(request)}"

  override def generateOperationName[F[_]](request: Request[F]): String =
    getNormalizedPath(request)

  private def getNormalizedPath[F[_]](request: Request[F]) = {
    val cleanPath = regex.replaceAllIn(request.uri.path, "/uuid")
    val p         = normalizePattern.replaceAllIn(cleanPath, "$1").replace('/', '.').dropWhile(_ == '.')
    val normalisedPath = {
      if (p.lastOption.exists(_ != '.')) s"$p."
      else p
    }
    s"$normalisedPath${request.method.name.toLowerCase(Locale.ENGLISH)}"
  }
}
