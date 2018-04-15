[![Build Status](https://travis-ci.org/citrum/querio.svg?branch=master)](https://travis-ci.org/citrum/querio)
[![Download](https://api.bintray.com/packages/citrum/maven/querio/images/download.svg)](https://bintray.com/citrum/maven/querio/_latestVersion)

_Querio_ is a scala ORM, DSL, and code generator for database queries.

## Installation & usage

Please see `example-mysql`, `example-postgresql` subprojects in this repo.

## Querio concepts

* Write SQL in Scala
* Minimal overhead
* Human-readable SQL on output
* Ability to work with different Mysql Databases or Postgresql Schemas within one project
* Type safety and type inference
* Database first: codegen from database structure
* Nested transactions
* Hooks
* Validation of modifications


### Write SQL in Scala

Querio DSL allows you to write SQL-like code in Scala.  
No magic behind the scenes. No hidden queries.
Explicit is better than implicit.

```scala
val records: Vector[(Bill, CompanyRequisites)] =
  Db.query(_
    select(Bill, CompanyRequisites)
    from Bill
    leftJoin CompanyRequisites on CompanyRequisites.id == Bill.companyReqId
    where Bill.status == BillStatus.MustBeRefunded
    fetch())
```

`(Bill, CompanyRequisites)` inferred from `select` clause.

Type safety works on all conditions:
* Ints: `CompanyRequisites.id == Bill.companyReqId`
* Enums: `Bill.status == BillStatus.MustBeRefunded`

But these will not compile:
* `CompanyRequisites.id == "123"` (comparing Int to String)
* `Bill.status == 1` (only enum allowed)

Another querio examples:

```scala
val maybeUser: Option[User] = Db.findById(User, 123)
```

```scala
val userNum: Int = Db.countByCondition(User, User.companyId == 5 && !User.ban)
```


### Minimal overhead

No reflections. Minimum boxing/unboxing.

Here is a part of generated code for one field:

```scala
val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
//                       1    2     3     4
```

Let's describe parameters of `TFD` class:
1. `"id"` - field name in database table
2. `_.id` - `TableRecord` value getter (a shorthand for `(t: MyTable) => t.id`)
3. `_.id` - `MutableTableRecord` value getter (a shorthand for `(t: MutableMyTable) => t.id`)
4. `_.id = _` - `MutableTableRecord` value setter (a shorthand for `(t: MutableMyTable, v: Int) => t.id = v`)

`TableRecord` - trait for *immutable* record that was read from database. For example:
```scala
class User(val id: Int,
           val phone: String,
           val createdOn: Instant) extends TableRecord
```

`MutableTableRecord` - trait for *mutable* record. Used to make inserts and updates to DB. Example:
```scala
class MutableUser extends MutableTableRecord[User] {
  var id: Int = _
  var phone: String = _
  var createdOn: Instant = _
  ...
}
```


### Human-readable SQL on output

Just compare Scala code

```scala
Db.query
  _.select(User.companyId.flat, Fun.count)
    from(Vac, User)
    where User.id == Vac.userId && User.companyId.in(companyIds) && Vac.vis == true &&
      (Vac.subCityId == cityId || (Vac.cityId == city.topCityId && Vac.nearbyCities.isTrue))
    groupBy User.companyId
    fetch()
```

to generated SQL

```sql
select user.id_account, count(*)
from vac, user
where ((user.id = vac.id_user and user.id_account in 1, 2, 3) and vac.vis = true
        and (vac.id_subcity = 1 or (vac.id_city = 1 and vac.nearby_cities))))
group by user.id_account
```


### Different Mysql Databases or Postgresql Schemas

Use as many databases/schemas as you want. Querio doesn't stick to one.
You can ever use both Mysql and Postgresql databases in one project without hassle.


### Database first: codegen from database structure

Smart code generation allows modification in generated classes (with some restrictions).

For every table in database querio generates a scala file with 3 classes and 1 object:

```scala
// 1
class UserTable(alias: String) extends Table[User, MutableUser]("ros", "user", alias) {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val companyId = new OptionInt_TF(TFD("id_account", ..., comment = "company id"))
  val email = new String_TF(TFD("email", ..., comment = "user email"))
  ...
}
// 2
object User extends UserTable(null)

// 3
class User(val id: Int,
           val companyId: Option[Int],
           val email: String) extends TableRecord {
  ...
}

// 4
class MutableUser extends MutableTableRecord[User] {
  var id: Int = _
  var companyId: Option[Int] = None
  var email: String = _
  ...
}
```

Let's describe this:
1. Table definition. Used to write SQL queries, and to transform data between database and TableRecord/MutableTableRecord.
2. Default implementation of table definition without alias.
3. Immutable POJO-like object. Represents table record.
4. Mutable POJO-like object. Represents table record that will be used in database modification.

Allowed modifications to generated files:
* Add a method or property to any generated class or object.
* Rename any field (notice `companyId` is already renamed). Generator will respect your names.
* Add any classes between generated ones. Generator should not touch your code.
* Generated classes can implement more traits, just add them after `extends` clause.


### Nested transactions

Querio allows you to use transactions any time you need them. Even if they are nested.

```scala
def delService(serviceId: Int, md: ModifyData): Unit = {
  Db.dataTrReadCommitted(md) {implicit dt =>
    Db.deleteByCondition(Service, Service.id == serviceId)
  }
}

def cronDelServices(): Unit = {
  Db.dataTrReadCommitted(Db.ModifyData(info = "Cron delete services")) {implicit dt =>
    ...
    delService(1, dt.md)
    ...
  }
}
```

Notice `cronDelServices` calls `delService` which leads to nested transactions.  
Querio will execute `SAVEPOINT` command for nested transactions. `ROLLBACK` whill be executed if nested transaction fails.


### Hooks

Hook support:
* On every database query.
* On data modification (when transaction successfully finishes only). Used for cache revalidation.



## License

_querio_ is licensed under [Apache License 2.0].

  [Apache License 2.0]: http://www.apache.org/licenses/LICENSE-2.0
