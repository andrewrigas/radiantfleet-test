package com.radianfleet

import java.time.LocalDate

case class ParsedExchangeRateHistory(currencyID: CurrencyID, exchangeRateHistory: Map[LocalDate, Map[CurrencyID, List[Option[BigDecimal]]]])
