package com.radianfleet

import java.util.UUID

opaque type CurrencyID = UUID

object CurrencyID {
  def apply(id: UUID): CurrencyID = id
  
  def generate(): CurrencyID = UUID.randomUUID()
  
  extension (c: CurrencyID) {
    def value: UUID = c
  }
}