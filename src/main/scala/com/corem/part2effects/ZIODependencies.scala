//package com.corem.part2effects
//
//import zio.*
//
//import java.util.concurrent.TimeUnit
//
//object ZIODependencies extends ZIOAppDefault{
//
//  // App to subscribe users to newsletter
//  case class User(name: String, email: String)
//
//  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
//    def notifyUser(user: User): Task[Unit] =
//      for {
//        _ <- emailService.email(user)
//        _ <- userDatabase.insert(user)
//      } yield ()
//  }
//
//  object UserSubscription {
//    def create(emailService: EmailService, userDatabase: UserDatabase) =
//      new UserSubscription(emailService, userDatabase)
//
//    val live: ZLayer[EmailService & UserDatabase, Nothing, UserSubscription] =
//      ZLayer.fromFunction(create _)
//  }
//
//  class EmailService {
//    def email(user: User): Task[Unit] =
//      ZIO.succeed(println(s"You've just been subscribed to Rock the JVM. Welcome ${user.name}"))
//  }
//
//  object EmailService {
//    def create(): EmailService = new EmailService
//
//    val live: ZLayer[Any, Nothing, EmailService] =
//      ZLayer.succeed(create())
//  }
//
//  class UserDatabase(connectionPool: ConnectionPool) {
//    def insert(user: User): Task[Unit] = for {
//      conn <- connectionPool.get
//      _ <- conn.runQuery(s"insert into subscribers(name, email) values (${user.name}, ${user.email})")
//    } yield ()
//  }
//
//  object UserDatabase {
//    def create(connectionPool: ConnectionPool) =
//      new UserDatabase(connectionPool)
//
//    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
//      ZLayer.fromFunction(create _)
//  }
//
//  class ConnectionPool(nConnection: Int) {
//    def get: Task[Connection] =
//      ZIO.succeed(println("Acquired connection")) *> ZIO.succeed(Connection())
//  }
//
//  object ConnectionPool {
//    def create(nConnections: Int) =
//      new ConnectionPool(nConnections)
//
//    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
//      ZLayer.succeed(create(nConnections))
//  }
//
//  case class Connection() {
//    def runQuery(query: String): Task[Unit] =
//      ZIO.succeed(println(s"Executing query: $query"))
//  }
//
//  val subscriptionService = ZIO.succeed( // Dependency injection
//    UserSubscription.create(
//      EmailService.create(),
//      UserDatabase.create(
//        ConnectionPool.create(10)
//      )
//    )
//  )
//
//  /*
//    Clean DI but has drawbacks
//      - does not scale for many services
//      - DI can be 100x worse
//        - Pass dependencies partially
//        - Not having all dependencies in the same place
//        - Passing dependencies multiple times can cause problems
//  */
//
//  def subscribe(user: User): ZIO[Any, Throwable, Unit] = for {
//    sub <- subscriptionService // Service is instantiated at the point of call
//    _ <- sub.notifyUser(user)
//  } yield ()
//
//  // Risk leaking resources if you subscribe multiple users in the same program
//  val program = for {
//    _ <- subscribe(User("Daniel", "daniel@rockthejvm.com"))
//    _ <- subscribe(User("Corem", "corem@core.com"))
//  } yield ()
//
//  // Alternative
//  def subscribeV2(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
//    sub <- ZIO.service[UserSubscription] // ZIO[UserSubscription, Nothing, UserSubscription]
//    _ <- sub.notifyUser(user)
//  } yield ()
//
//  val programV2 = for {
//    _ <- subscribeV2(User("Daniel", "daniel@rockthejvm.com"))
//    _ <- subscribeV2(User("Corem", "corem@core.com"))
//  } yield ()
//
//
//  /*
//    - We don't need to care about dependencies until the end of the world
//    - All ZIOs requiring this dependency will use the same instance
//    - Can use different instances of the same type for different needs (e.g. testing)
//    - Layers can be created and composed much like regular ZIOs with a rich API
//  */
//
//  /**
//   * ZLayers
//   */
//  val connectionPoolLayer: ZLayer[Any, Nothing, ConnectionPool] = ZLayer.succeed(ConnectionPool.create(10))
//  val databaseLayer: ZLayer[ConnectionPool, Nothing, UserDatabase] = ZLayer.fromFunction(UserDatabase.create _)
//  val emailServiceLayer: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(EmailService.create())
//  val userSubscriptionServiceLayer: ZLayer[UserDatabase & EmailService, Nothing, UserSubscription] = ZLayer.fromFunction(UserSubscription.create _)
//
//  // Composing layers
//  // Vertical composition >>>
//  val databaseLayerFull: ZLayer[Any, Nothing, UserDatabase] = connectionPoolLayer >>> databaseLayer
//
//  // Horizontal composition: combines the dependencies of both layers AND the values of both layers
//  val subscriptionRequirementsLayer: ZLayer[Any, Nothing, UserDatabase & EmailService] = databaseLayerFull ++ emailServiceLayer
//
//  // Mix & match
//  val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscription] =
//    subscriptionRequirementsLayer >>> userSubscriptionServiceLayer
//
//  val runnableProgram = programV2.provide(userSubscriptionLayer)
//
//  // Magic
//  val runnableProgramV2 = programV2.provide(
//    UserSubscription.live,
//    EmailService.live,
//    UserDatabase.live,
//    ConnectionPool.live(10),
//    // ZIO will tell you if you're missing a Layer
//    // and also tell you if you have multiple layers of the same type:
//    // UserSubscription.live,
//    // and tell you the dependency graph:
//    //ZLayer.Debug.tree,
//    ZLayer.Debug.mermaid,
//  )
//
//  // Magic V2
//  val userSubscriptionLayerV2: ZLayer[Any, Nothing, UserSubscription] = ZLayer.make[UserSubscription](
//    UserSubscription.live,
//    EmailService.live,
//    UserDatabase.live,
//    ConnectionPool.live(10),
//  )
//
//  // Passthrough
//  val dbWithPoolLayer: ZLayer[ConnectionPool, Nothing, ConnectionPool & UserDatabase] =
//    UserDatabase.live.passthrough
//
//  // Service = take a dependency and expose it as a valur to further layers
//  val dbService = ZLayer.service[UserDatabase]
//
//  // Launch
//  val subscriptionLaunch: ZIO[EmailService & UserDatabase, Nothing, Nothing] = UserSubscription.live.launch
//
//  // Memoization
//  // By default except if you mark a layer as .fresh (e.g. EmailService.live.fresh)
//
//  // Already provided services: Clock, Random, System, Console
//  val getTime = Clock.currentTime(TimeUnit.SECONDS)
//  val randomValue = Random.nextInt
//  val sysVariable = System.env("HADOOP_HOME")
//  val printLnEffect = Console.printLine("This is ZIO")
//
//  def run = runnableProgramV2
//}
