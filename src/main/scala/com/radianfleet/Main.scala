package com.radianfleet

import java.time.LocalDate
import cats.syntax.all.*
// UI exchange rates, and other information about the exchange

@main
def main = {
  val exchangeRateHistoryLoader = ExchangeRateHistoryLoader.live
  val exchangeRepository = ExchangeRepository.live

  val inputDate = LocalDate.of(2026, 4, 29)
  val inputBaseCurrency = "EUR"
  val inputTargetCurrency = "USD"
  val inputAmount = BigDecimal(100.0)

  val euroCurrencySymbol = "EUR"

  for {
    euroExchangeRateHistory <- exchangeRateHistoryLoader.loadFile("euro-exchange-history.csv")
    euroCurrencyId <- exchangeRepository.upsertCurrency(euroCurrencySymbol)
    _ = println(s"EUR: $euroCurrencyId")
    //    currencies <- euroExchangeRateHistory.values.headOption.map(_.keys.toSet).toRight(ServiceError.FileLoadingError("No exchange rate data found in the file"))
    _ <- euroExchangeRateHistory.toList.traverse { case (date, data) =>
      for {
        currencyData <- data.toList.traverse {
          case (currency, exchange) =>
            exchangeRepository.upsertCurrency(currency).map(currencyId => currencyId -> exchange)
        }
        _ = if(date == inputDate) println(s"Date: $date, Currency Data: $currencyData")
        currencyDataFiltered = currencyData.toMap.collect { case (key, Some(exchange)) => key -> exchange } // Filter out currencies with no exchange rate
        _ <- exchangeRepository.insertExchangeRate(baseCurrencyID = euroCurrencyId, data = Map(date -> currencyDataFiltered))
      } yield ()
    }
    inputBaseCurrencyId<- exchangeRepository.upsertCurrency(inputBaseCurrency)
    _ = println(s"EUR: $inputBaseCurrencyId")
    inputTargetCurrencyId <- exchangeRepository.upsertCurrency(inputTargetCurrency)
    _ = println(s"USD: $inputTargetCurrencyId")
    value <- exchangeRepository.getExchangeRate(inputBaseCurrencyId, inputTargetCurrencyId, inputDate)
    _ = println(value)
  } yield ()


}
