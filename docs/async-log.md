# 异步日志

``vert.x``是异步的，但是写日志的时候呢？写日志的IO操作会阻塞``event-loop``吗？
回答：实际不会。因为``log4j``之类的，都是先写入``Buffer``，就立即返回；刷盘是后续的动作。

>Most of loggers are **async** from the beginning , i.e. they are **not write information immediately**. Logs are stored into buffer, which is flushed by timeout or when it is full. So ``slf4j`` + ``log4j`` is good enough for most cases.


## log4j2

``log4j``推出了``version-2``，也就是``log4j2``。为什么呢？

- 支持参数：类似``slf4j``，用``{}``来支持``parameterized message``。


``{}``参数的好处，对比下两个日志语句：

语句1： 无``{}``的

``` java
LOG.debug("say hello: " + name);
```

语句2：有``{}``的

``` java
LOG.debug("say hello: {}", name);
```  

在 语句1 中，无论日志是否是``isDebugEnable``，都会拼接出 ``"say hello: " + name`` 字符串，接着进入日志发现``debugEnable=false``，再接着等待``GC``回收。为了避免频繁的拼接出没有用的日志字符串，可把语句1改写成：

``` java
if (LOG.isDebugEnable()) {
  LOG.debug("say hello: " + name);
}
```

可以避免上述问题，但是代码显得很臃肿。我们再看看语句2，它的魔力在哪呢？首先第一个参数``"say hello: {}"``是一个字符串常量，编译时会进入JVM常量池；第二个参数是变量，但是它的拼接是只有``debugEnable=true``才会拼接的。

语句2的形式，最早是``slf4j``提出来的，后来``log4j2``也实现了这类功能，同时``log4j2``支持异步IO。

## vertx logging

``vertx``有个日志类，但是它只是一个接口，具体实现可以是：``JDK Log``（又称``JUL``），``log4j``，``log4j2``和``slf4j``。为了减少对外部第三方jar的依赖，``vertx``默认实现选择了``JDK Log``。

### 默认实现

默认实现是``JDK Log``，日志配置文件默认是：``vertx-default-jul-logging.properties``。如果想用人为指定日志配置文件，可以``-Djava.util.logging.config.file=/some/path/to/log.properties``。

### 其他实现

如果不想用``JDK``的实现，也可用其他实现，当然我们得把第三方包引入。

- 使用 ``log4j``：

``` java
-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory
```


- 使用 ``log4j2``：

``` java
-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory
```

- 使用``slf4j``：

``` java
-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory
```
