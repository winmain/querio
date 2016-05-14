package model.db
import querio.json.JSON4SExtension
import querio.vendor.PostgreSQL

object PostgresSQLVendor extends PostgreSQL with JSON4SExtension
