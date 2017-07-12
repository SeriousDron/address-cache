# Agoda code challenge

### Task

This code is written as a code challenge. The initial task is [on Github](https://github.com/itplatform/addresscache).

Shortly, I was given a Java class without implementation (shortened version below) and my task is to implement it having concurrency in mind. Additionally, I was asked to wrap it into REST API. I'm writing this task in Scala so there are small changes.
 
    public class AddressCache {
        public AddressCache(long maxAge, TimeUnit unit) {}
        public boolean add(InetAddress address) {}
        public boolean remove(InetAddress address) {}
        public InetAddress peek() {}
        public InetAddress take() {}
    }

### Decisions made

1. As it supposed to be some kind of cache I decided that it gonna have much more reads than writes. In the original task, the only pure read method is "peek", but still I've decided to use ReadWriteLock to allow execute reads concurrently.
2. I've replaced peek result type to `Option[InetAddress]` so it reflects that it not always returns successful result (initial task offering to use `null`).
3. As I supposing write operations are pretty rare and not that time challenging and they do acquire a write lock anyway I've decided to make expired elements cleanup on writes (actually inspired by Google Guava LocalCache).
Additionally, I've added full cleanup if reading procedure founds that all elements are expired.
4. Initially, I thought of implementing API with Akka HTTP or another low-level HTTP library, but then I've decided that in the real world you will need features as logging, dependency injection container, tests infrastructure and so on and implemented API with Play Framework, but trying to minimize overhead.
5. As `AddressCache` operations are blocking in their nature I use separate execution context with different thread pool to avoid blocking in main thread pool of Play framework

### Results

AddressCache implementation itself is located at `src/main/scala/model/AddressCache`

API controller located at `src/main/scala/controllers/ApiController.scala`

API documentation is located at [http://docs.addresscacheapi.apiary.io/](http://docs.addresscacheapi.apiary.io/)

I've deployed API to play with it to [http://agoda.seriousdron.ru/](http://agoda.seriousdron.ru/). I'll eventually stop it so I can become unavailable after some point in time.

This project has 98% unit tests coverage (because I don't know how to make scoverage ignore a single line). You can find coverage report at [http://agoda.seriousdron.ru/coverage/](http://agoda.seriousdron.ru/coverage/). To run tests locally use `sbt test`.

It producing about 4000 RPS on my PC with concurrency level 50 and without any tuning so I believe performance is decent.
