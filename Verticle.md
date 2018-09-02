# Verticle

`Vert.x 3.x`, `Groovy 2.4`

## IntelliJ Run/Debug Configuration

`IntelliJ 2017.2`

* Configuarion: `Application`
* Main class: `io.vertx.core.Launcher`
* VM options: Enable Vert.x to use the SLF4J logger instead of JUL, so that Netty (automatically detects SLF4J), Vert.x and your own code log to the same Logger:
    `-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory`
* Program arguments: `run com.n20s.vertx.ExampleVerticle -conf src/main/conf/application-configuration.json`
* Enable `Single instance` to prevent duplicate instances trying to bind to the same port.


## HTTP-Server Template Example 

```
   @Override
   void start() throws Exception {

      BuildInformation buildInformation = new BuildInformation()
      String buildVersion = buildInformation.getBuildVersion()
      def host = config().getJsonObject('verticle')?.getString("host")
      def port = config().getJsonObject('verticle')?.getInteger("port")
      
      // Define a context path. Note: Consider using header information as an alternative.
      def basePath = config().getJsonObject('verticle')?.getString('basePath')

      log.info "${'*'.multiply(80)}"
      log.info "Starting verticle, version ${buildVersion} on ${host}:${port}"

      // Configurations
      // ...

      // Routing handlers
      // ...

      // Launch http server

      HttpServerOptions httpServerOptions = new HttpServerOptions()
      httpServerOptions.host = host
      httpServerOptions.port = port

      HttpServer server = vertx.createHttpServer(httpServerOptions)

      server.requestHandler() { request ->
         router.&accept(request.setExpectMultipart(true))
      }
   
      server.listen() { result ->
         if (result.succeeded()) {
            log.info "Now listening on ${host}:${port}"
         } else {
            log.error "Could not listen on ${host}:${port}, caused by: ${result.cause()}"
            // End execution. Add a delay to prevent loops with system autostart mechanisms.
            vertx.setTimer(5000) { id ->
                vertx.close()
            }
         }
      }
   }
```

## Logging with SLF4J/Logback

build.gradle

```
dependencies {

    compile "org.slf4j:slf4j-api:1.7.25"
    compile "ch.qos.logback:logback-core:1.0.13"
    compile "ch.qos.logback:logback-classic:1.0.13"
    ...
}    
```
src/main/resources/logback.groovy

```
def templatePattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = templatePattern
    }
}
appender("FILE", RollingFileAppender) {
    file = "logs/myserver.log"
    encoder(PatternLayoutEncoder) {
        pattern = templatePattern
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "logs/myserver-%d{yyyy-MM-dd}.log"
    }
}

root(INFO, ["CONSOLE", "FILE"])
logger("com.myserver", DEBUG, ["CONSOLE", "FILE"], false)
```
CodeClass.groovy

```
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CodeClass {

   static Logger log = LoggerFactory.getLogger(CodeClass.class)

   void method() {
      log.info "logged information"
   }
}
```
Results in:

```
2018-09-02 15:00:01.310 [vert.x-eventloop-thread-0] INFO  c.n.e.CodeClass - logged information
```