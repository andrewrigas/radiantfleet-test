package com.radianfleet

sealed abstract class ServiceError(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

object ServiceError {
  case class FileLoadingError(message: String, cause: Option[Throwable] = None) extends ServiceError(message, cause)
  case class RepositoryError(message: String, cause: Option[Throwable] = None) extends ServiceError(message, cause)
}
