# Vert.x tutorial

## blocking-code

- [blocking-code.md](blocking-code.md)
- [async-call.md](async-call.md)

### Example02

测试HTTP请求响应：

``` bash
$ curl -X POST -i -d '{"filename": "Hello"}' -H "Content-Type: application/json" http://localhost:8080/file
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Transfer-Encoding: chunked

a random string just for having a passing example%
```

后台服务端日志：

```
Verticle deployed
Blocking Read File: Hello in Thread: vert.x-worker-thread-0
Async Result Got in Thread: vert.x-eventloop-thread-1
```

### Example03

- [future-callback-hell.md](docs/future-callback-hell.md)
- 


# 参考资料

- [vertx-understanding-core-concepts](http://www.enterprise-integration.com/blog/vertx-understanding-core-concepts/)
