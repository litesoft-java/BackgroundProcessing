# BackgroundProcessing
Library to support long running Background Processes

    Create Shutdown code that supports registering GracefulShutdownable(s) ExecutorService

    Support ExecutorService as a special ExecutorShutdownable
    Plan on using Lightweight (Loom) Threads, but starting with a Executors.newCachedThreadPool()
        for doing Background processing.

Coming Soon...

On maven Central ([latest release](https://mvnrepository.com/artifact/org.litesoft/annotations/1.1.2)):
```html
  <dependency>
    <groupId>org.litesoft</groupId>
    <artifactId>background_processing</artifactId>
    <version>1.0.0</version>
  </dependency>
```
