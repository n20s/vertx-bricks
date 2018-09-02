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

## Gradle Configuration

```
buildscript {
    repositories {
        mavenCentral() }
        mavenLocal()
    }
    dependencies {
    }
}

plugins {
    id 'groovy'
    id 'idea'
    id 'application'
    id "com.github.johnrengelman.shadow" version "2.0.4"
}

// Version of the application
version = '1.0'

String serverDescription = "My Server"
String serverName = "my-server"
String companyDescription = "My Company"
String mainVerticleName = "com.myserver.ExampleVerticle"

sourceCompatibility = '1.8'
String vertxVersion = "3.4.2"
mainClassName = 'io.vertx.core.Launcher'
String runWatchForChange = "src/**/*"
String runDoOnChange = "./gradlew classes"

// Do not create the base jar with the build task, because we also build a fat/shadow jar.
jar.enabled = false

/*
 * Execute command and return output String. Return null if exit value != 0. 
 */
def executeCommand = { String cmd ->
    Process process = cmd.execute()
    // wait for execution to be finished and exitValue() is 0
    if (process.waitFor() != 0) {
       return null
    }
    return process.text.trim()
}

/*
 * Derive git repository revision (if existent) to be included in the jar packaging.
 */
def gitRevision = {

    // Get short description of revision.
    def versionInfo = executeCommand('git rev-parse --short HEAD')    
    if (versionInfo == null) {
      // Return empty string if not successful. Accept fail, e.g. when the directory is not git managed at all.
      return '' 
    }

	// Get dirty flag on managed files, does not reflect untracked files. Status code is 0 in both cases dirty or not.
    def diffStat = executeCommand('git diff --shortstat') 
    if (diffStat == null) {
       // As the command before succeeded, we expected this command to succeed too.
       throw new RuntimeException("Unexpected resulting status code in git command") 
    }
    if (diffStat.length() > 0) { 
        // Diff is not empty, local git repository has been modified.
        versionInfo += '-mod'
    }
    return versionInfo
}

// create a single jar package with all dependencies
shadowJar {
    classifier = 'all'

    mergeGroovyExtensionModules()
    mergeServiceFiles {
        include 'META-INF/services/io.vertx.core.spi.VerticleFactory'
    }

    def versionInfo = gitRevision()
    if (versionInfo) {
        version += "-${versionInfo}"
    }
    manifest {
        attributes 'Implementation-Title': "${serverDescription} jar-packaged vert.x verticle",
                'Implementation-Version': version,
                'Implementation-Vendor': companyDescription,
                'Main-Class': mainClassName,
                'Main-Verticle': mainVerticleName,
                'Git-Tag': versionInfo
    }
    baseName = serverName
}

run {
    // Run the main verticle class with a default development configuration.
    // Use auto-redeploy (restarts the server when a change is detected)
    args = ['run', mainVerticleName,
            "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory",
            "-conf", "src/main/conf/application-configuration.json",
            "--redeploy=$runWatchForChange",
            "--launcher-class=$mainClassName",
            "--on-redeploy=$runDoOnChange"]
}

repositories {
    mavenCentral()
}

dependencies {

    compile "org.slf4j:slf4j-api:1.7.25"
    compile "ch.qos.logback:logback-core:1.0.13"
    compile "ch.qos.logback:logback-classic:1.0.13"

    compile "io.vertx:vertx-core:$vertxVersion"
    compile "io.vertx:vertx-lang-groovy:$vertxVersion"
    compile "io.vertx:vertx-config:$vertxVersion"
    compile "io.vertx:vertx-web:$vertxVersion"
    compile "io.vertx:vertx-auth-shiro:$vertxVersion"

    compile fileTree(dir: 'lib', include: ['*.jar'])

    testCompile "junit:junit:4.12"
    testCompile "io.vertx:vertx-unit:$vertxVersion"
}
```

## Application Configuration File

src/main/conf/application-configuration.json

```
{
  "verticle": {
    "host": "localhost",
    "port": 8080,
    "basePath": "mycontext"
  },
  ...
}
```
This would imply calling `http://localhost:8080/mycontext/...`.

The base path (or context) will be used in the Verticle when defining routers. Alternatively the base path might be derived from headers provided by a reverse proxy.

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
/**
 * Centralized logging with slf4j/logback.
 *
 * Note: Use VM option
 *
 *   -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory
 *
 * in order to let vert.x log to this logger.
 * Netty will automatically detect slf4j.
 */
 
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
        // Note: Don't use compression if you require log search also to be possible in rotated archives older than one day.
        fileNamePattern = "logs/myserver-%d{yyyy-MM-dd}.log"
    }
}

// General setup
root(INFO, ["CONSOLE", "FILE"])

// More details for the specified package prefix.
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