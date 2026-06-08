package com.radianfleet

import java.time.LocalDate
import java.util.UUID

case class ExchangeRate(currencyID: CurrencyID, currency: String, localDate: LocalDate, rate: Option[BigDecimal])
