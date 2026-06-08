package com.radianfleet

import java.time.LocalDate
import scala.collection.mutable
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.Exception.allCatch

trait ExchangeRepository {
  def getExchangeRate(baseCurrencyID: CurrencyID, targetCurrencyID: CurrencyID, date: LocalDate): Either[ServiceError.RepositoryError, Option[BigDecimal]]

  def insertExchangeRate(baseCurrencyID: CurrencyID, data: Map[LocalDate, Map[CurrencyID, BigDecimal]]): Either[ServiceError.RepositoryError, Unit]

  def upsertCurrency(currency: String): Either[ServiceError.RepositoryError, CurrencyID]
}

object ExchangeRepository {

  private final class ExchangeRepositoryImpl extends ExchangeRepository {
    val currenciesTable: mutable.Map[CurrencyID, String] = mutable.Map.empty // Placeholder for actual exchange rate data

    val exchangeRateCurrencyHistoryTable: mutable.Map[CurrencyID, Map[LocalDate, Map[CurrencyID, BigDecimal]]] = mutable.Map.empty // Placeholder for actual exchange rate data

    override def upsertCurrency(currency: String): Either[ServiceError.RepositoryError, CurrencyID] =
      currenciesTable.find { case (_, curr) => curr.equalsIgnoreCase(currency) }.map { case (id, _) => Right(id) }.getOrElse {
        val newCurrencyID = CurrencyID.generate()
        allCatch.either(currenciesTable += (newCurrencyID -> currency)).left.map(e => ServiceError.RepositoryError(s"Failed to insert new currency: $currency", Some(e))).map(_ => newCurrencyID)
      }

    override def getExchangeRate(baseCurrencyID: CurrencyID, targetCurrencyID: CurrencyID, date: LocalDate): Either[ServiceError.RepositoryError, Option[BigDecimal]] =
      allCatch
        .either(
          exchangeRateCurrencyHistoryTable
            .get(baseCurrencyID)
            .flatMap(_.get(date)).flatMap(_.get(targetCurrencyID))).left.map(e => ServiceError.RepositoryError(s"Failed to get exchange rate for base currency ID: $baseCurrencyID, target currency ID: $targetCurrencyID, date: $date", Some(e)))

    override def insertExchangeRate(baseCurrencyID: CurrencyID, data: Map[LocalDate, Map[CurrencyID, BigDecimal]]): Either[ServiceError.RepositoryError, Unit] = allCatch.either{
      val updatedData = exchangeRateCurrencyHistoryTable.get(baseCurrencyID) match {
        case Some(existingData) =>
          exchangeRateCurrencyHistoryTable +=  (baseCurrencyID -> (existingData ++ data)) // Merge existing data with new data, new data will overwrite existing data for the same date
        case None => exchangeRateCurrencyHistoryTable += (baseCurrencyID -> data) // Insert new data for the base currency ID
      }
    }.left.map(e => ServiceError.RepositoryError(s"Failed to insert exchange rate for base currency ID: $baseCurrencyID", Some(e))).map(_ => ())
  }

  val live: ExchangeRepository = new ExchangeRepositoryImpl
}
