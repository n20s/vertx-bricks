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