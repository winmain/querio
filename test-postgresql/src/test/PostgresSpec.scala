package test
import java.sql.{Connection, DriverManager, SQLException, Statement}


trait PostgresSpec extends BeforeAllAfterAll {
  //  http://www.superloopy.io/articles/2013/scala-slick-postgresql-unit-tests.html
  //trait PostgresSpec extends Suite with BeforeAndAfterEach with BeforeAndAfterAll with DatabaseProvider {
  //  this: { def ddl: SchemaDescription } =>
  //
  private val dbname = getClass.getSimpleName.toLowerCase
  private val driver = "org.postgresql.Driver"

  private val dbConnection = s"jdbc:postgresql://localhost:5432/$dbname"
  private val dbUser = "root"
  private val dbPassword = ""

  Class.forName(driver).newInstance()

  protected def inStatement(f: Statement => Any) {
    var connection: Connection = null
    var statement: Statement = null
    try {
      connection = DriverManager.getConnection(dbConnection, dbUser, dbPassword)
      statement = connection.createStatement()
      f(statement)
    } catch {
      case se: SQLException => se.printStackTrace()
      case e: Exception => e.printStackTrace()
    } finally {
      try {
        if (statement != null)
          statement.close()
      } catch {
        case se: SQLException => se.printStackTrace()
      }
      try {
        if (connection != null)
          connection.close()
      } catch {
        case se: SQLException => se.printStackTrace()
      }
    }
  }

  override def beforeAll() {

    //    val sql: String = "CREATE TABLE REGISTRATION " +
    //      "(id INTEGER not NULL, " +
    //      " first VARCHAR(255), " +
    //      " last VARCHAR(255), " +
    //      " age INTEGER, " +
    //      " PRIMARY KEY ( id ))";

    inStatement {stmt =>
      stmt.executeUpdate(s"DROP DATABASE IF EXISTS $dbname")
      stmt.executeUpdate(s"CREATE DATABASE $dbname")
    }

  }
  //
  override def afterAll() {
    inStatement {stmt =>
      stmt.executeUpdate(s"DROP DATABASE $dbname")
    }
  }
  //
  //  val database = Database.forURL(s"jdbc:postgresql:$dbname", driver = driver)
  //
  //  override def beforeEach() {
  //    database withSession { implicit ss: Session =>
  //      ddl.create
  //    }
  //  }
  //
  //  override def afterEach() {
  //    database withSession { implicit ss: Session =>
  //      ddl.drop
  //    }
  //  }
}