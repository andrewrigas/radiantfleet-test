package com.radianfleet

import scala.io.Source
import scala.util.Using
import cats.syntax.all.*

import java.time.LocalDate
import scala.util.control.Exception.allCatch

trait ExchangeRateHistoryLoader {
  def loadFile(filePath: String): Either[ServiceError.FileLoadingError, Map[LocalDate, Map[String, Option[BigDecimal]]]]
}

object ExchangeRateHistoryLoader {

  private final class ExchangeRateHistoryLoaderImpl extends ExchangeRateHistoryLoader {

    case class InternalExchangeRateLine(date: LocalDate, currencyExchangeRates: Map[String, Option[BigDecimal]]) // (Date, List of exchange rates as Option[BigDecimal])

    override def loadFile(filePath: String): Either[ServiceError.FileLoadingError, Map[LocalDate, Map[String, Option[BigDecimal]]]] = Using(Source.fromResource(filePath)) { buffer =>
      val allLines = buffer.getLines()

      for {
        header <- allLines.take(1).toList.headOption.toRight(ServiceError.FileLoadingError("File is empty")) // Assuming the header is in the format: "Date,USD,EUR,GBP,..."
        body = allLines.drop(1).toList
        columnTitles = header.split(",").map(_.trim).toList
        currencies = columnTitles.drop(1) // Drop the first column which is "Date"
        internalExchangeRateLines <- body.zipWithIndex.traverse { case (line, idx) =>
          for {
            values = line.split(",").map(_.trim)
            dateRaw <- values.headOption.toRight(ServiceError.FileLoadingError(s"Missing date in line: $line, index: [$idx]"))
            date <- allCatch.either(LocalDate.parse(dateRaw)).left.map(e => ServiceError.FileLoadingError(s"Invalid date format: $dateRaw in line: $line, index: [$idx]", Some(e)))
            exchangeRatesRaw = values.drop(1).toList
            exchangeRates <- exchangeRatesRaw.traverse { rateStr =>
              if (rateStr.isEmpty || rateStr == "N/A") Right(Option.empty)
              else allCatch.either(BigDecimal(rateStr)).map(Some(_)).left.map(e => ServiceError.FileLoadingError(s"Invalid exchange rate value: $rateStr in line: $line, index: [$idx]", Some(e)))
            }
            _ <- if (exchangeRates.length != currencies.length)
              Left(ServiceError.FileLoadingError(s"Number of exchange rates does not match number of currencies in line: $line, index: [$idx]"))
            else Right(())
            currencyExchangeRates = currencies.zip(exchangeRates).toMap
          } yield InternalExchangeRateLine(date, currencyExchangeRates)
        }
      } yield internalExchangeRateLines.map(l => l.date -> l.currencyExchangeRates).toMap
    }.toEither.left.map(e => ServiceError.FileLoadingError(s"Failed to load file: $filePath", Some(e))).flatten
  }

  
  val live: ExchangeRateHistoryLoader = new ExchangeRateHistoryLoaderImpl
}
