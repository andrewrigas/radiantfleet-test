package com.radianfleet

import java.time.LocalDate
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.util.control.Exception.allCatch

trait ExchangeRepository {
  def getExchangeRate(baseCurrencyID: CurrencyID, targetCurrencyID: CurrencyID, date: LocalDate): Either[ServiceError.RepositoryError, Option[BigDecimal]]

  def insertExchangeRate(baseCurrencyID: CurrencyID, data: Map[LocalDate, Map[CurrencyID, BigDecimal]]): Either[ServiceError.RepositoryError, Unit]

  def upsertCurrency(currency: String): Either[ServiceError.RepositoryError, CurrencyID]
}

object ExchangeRepository {

  private final class ExchangeRepositoryImpl extends ExchangeRepository {

    val currenciesTable: mutable.Map[CurrencyID, String] = mutable.Map.empty // Placeholder for actual exchange rate data
    val exchangeRateCurrencyHistoryTable: mutable.Map[CurrencyID, TreeMap[LocalDate, Map[CurrencyID, BigDecimal]]] = mutable.Map.empty // Placeholder for actual exchange rate data

    val descendingLocalDateOrdering: Ordering[LocalDate] = Ordering[LocalDate].reverse
    val emptyTreeMap = TreeMap.empty[LocalDate, Map[CurrencyID, BigDecimal]](using descendingLocalDateOrdering)

    override def upsertCurrency(currency: String): Either[ServiceError.RepositoryError, CurrencyID] =
      currenciesTable.find { case (_, curr) => curr.equalsIgnoreCase(currency) }.map { case (id, _) => Right(id) }.getOrElse {
        val newCurrencyID = CurrencyID.generate()
        allCatch.either(currenciesTable += (newCurrencyID -> currency)).left.map(e => ServiceError.RepositoryError(s"Failed to insert new currency: $currency", Some(e))).map(_ => newCurrencyID)
      }

    override def getExchangeRate(baseCurrencyID: CurrencyID, targetCurrencyID: CurrencyID, date: LocalDate): Either[ServiceError.RepositoryError, Option[BigDecimal]] =
      allCatch
        .either {
          for {
            exchangeRateCurrencyHistory <- exchangeRateCurrencyHistoryTable.get(baseCurrencyID)
            currencyExchangeRate <- exchangeRateCurrencyHistory.get(date) orElse exchangeRateCurrencyHistory.minAfter(date).map { case (_, exchangeRate) => exchangeRate }
            exchangeRate <- currencyExchangeRate.get(targetCurrencyID)
          } yield exchangeRate
        }
        .left.map(e => ServiceError.RepositoryError(s"Failed to get exchange rate for base currency ID: $baseCurrencyID, target currency ID: $targetCurrencyID, date: $date", Some(e)))

    override def insertExchangeRate(baseCurrencyID: CurrencyID, data: Map[LocalDate, Map[CurrencyID, BigDecimal]]): Either[ServiceError.RepositoryError, Unit] =
      allCatch.either {

        val existingData = exchangeRateCurrencyHistoryTable.getOrElse(
          baseCurrencyID,
          emptyTreeMap
        )

        val newData = data.foldLeft(emptyTreeMap) { case (acc, (date, newRates)) =>
          val mergedRates = existingData.get(date) match {
            case Some(existingRates) => existingRates ++ newRates
            case None => newRates
          }

          acc + (date -> mergedRates)
        }

        val updatedData = existingData ++ newData

        exchangeRateCurrencyHistoryTable += (baseCurrencyID -> updatedData)
      }.left.map(e => ServiceError.RepositoryError(s"Failed to insert exchange rate for base currency ID: $baseCurrencyID", Some(e))).map(_ => ())
  }

  val live: ExchangeRepository = new ExchangeRepositoryImpl
}
