package model.db
import querio.json.JSON4SExtension
import querio.postgresql.PGByteaExtension
import querio.vendor.PostgreSQLVendor

object PostgresSQLVendor extends PostgreSQLVendor
  with JSON4SExtension
  with PGByteaExtension
