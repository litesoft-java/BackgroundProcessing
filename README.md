# BackgroundProcessing
Library to support long running Background Processes

    Create Shutdown code that supports registering GracefulShutdownable(s) ExecutorService

    Support ExecutorService as a special ExecutorShutdownable
    Plan on using Lightweight (Loom) Threads, but starting with a Executors.newCachedThreadPool()
        for doing Background processing.

