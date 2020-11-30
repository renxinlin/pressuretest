Changes by Version
==================
Release Notes.

6.6.0
------------------

#### Project
- [**IMPORTANT**] Local span and exit span are not treated as endpoint detected at client and local. Only entry span is the endpoint. Reduce the load of register and memory cost.
* Support MiniKube, Istio and SkyWalking on K8s deployment in CI.
* Support Windows and MacOS build in GitHub Action CI.
* Support ElasticSearch 7 in official dist.
* Hundreds plugin cases have been added in GitHub Action CI process.

#### Java Agent
* Remove the local/exit span operation name register mechanism.
* Add plugin for JDK Threading classes.
* Add plugin for Armeria.
* Support set operation name in async span.
* Enhance webflux plugin, related to Spring Gateway plugin. Webflux plugin is in optional, due to JDK8 required.
* Fix a possible deadlock.
* Fix NPE when OAL scripts are different in different OAP nodes, mostly in upgrading stage.
* Fix bug about wrong peer in ES plugin.
* Fix NPE in Spring plugin.
* Fix wrong class name in Dubbo 2.7 conflict patch.
* Fix spring annotation inheritance problem.

#### OAP-Backend
* Remove the local/exit span operation name register mechanism.
* Remove client side endpoint register in service mesh.
* Service instance dependency and related metrics.
* Support `min` func in OAL
* Support `apdex` func in OAL
* Support custom ES config setting at the index level.
* Envoy ALS proto upgraded.
* Update JODA lib as bugs in UTC +13/+14.
* Support topN sample period configurable.
* Ignore no statement DB operations in slow SQL collection.
* Fix bug in docker-entrypoint.sh when using MySQL as storage

#### UI
* Service topology enhancement. Dive into service, instance and endpoint metrics on topo map.
* Service instance dependency view and related metrics.
* Support using URL parameter in trace query page.
* Support apdex score in service page.
* Add service dependency metrics into metrics comparison.
* Fix alarm search not working.

#### Document
* Update user list and user wall.
* Add document link for CLI.
* Add deployment guide of agent in Jetty case.
* Modify Consul cluster doc.
* Add document about injecting traceId into the logback with logstack in JSON format.
* ElementUI license and dependency added.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/37?closed=1)

6.5.0
------------------

#### Project
- TTL E2E test (#3437)
- Test coverage is back in pull request check status (#3503)
- Plugin tests begin to be migrated into main repo, and is in process. (#3528, #3756, #3751, etc.)
- Switch to SkyWalking CI (exclusive) nodes (#3546)
- MySQL storage e2e test. (#3648)
- E2E tests are verified in multiple jdk versions, jdk 8, 9, 11, 12 (#3657)
- Jenkins build jobs run only when necessary (#3662)

#### OAP-Backend
- Support dynamically configure alarm settings (#3557)
- Language of instance could be null (#3485)
- Make query max window size configurable. (#3765)
- Remove two max size 500 limit. (#3748)
- Parameterize the cache size. (#3741)
- ServiceInstanceRelation set error id (#3683)
- Makes the scope of alarm message more semantic. (#3680)
- Add register persistent worker latency metrics (#3677)
- Fix more reasonable error (#3619)
- Add GraphQL getServiceInstance instanceUuid field. (#3595)
- Support namespace in Nacos cluster/configuration (#3578)
- Instead of datasource-settings.properties, use application.yml for MySQLStorageProvider (#3564)
- Provide consul dynamic configuration center implementation (#3560)
- Upgrade guava version to support higher jdk version (#3541)
- Sync latest als from envoy api (#3507)
- Set telemetry instanced id for Etcd and Nacos plugin (#3492)
- Support timeout configuration in agent and backend. (#3491)
- Make sure the cluster register happens before streaming process. (#3471)
- Agent supports custom properties. (#3367)
- Miscellaneous bug fixes (#3567)

#### UI 
- Feature: node detail display in topo circle-chart view.
- BugFix: the jvm-maxheap & jvm-maxnonheap is -1, free is no value
- Fix bug: time select operation not in effect 
- Fix bug: language initialization failed 
- Fix bug: not show instance language
- Feature: support the trace list display export png
- Feature: Metrics comparison view
- BugFix: Fix dashboard top throughput copy

#### Java Agent
- Spring async scenario optimize (#3723)
- Support log4j2 AsyncLogger (#3715)
- Add config to collect PostgreSQL sql query params (#3695)
- Support namespace in Nacos cluster/configuration (#3578)
- Provide plugin for ehcache 2.x (#3575)
- Supporting RequestRateLimiterGatewayFilterFactory (#3538)
- Kafka-plugin compatible with KafkaTemplate (#3505)
- Add pulsar apm plugin (#3476)
- Spring-cloud-gateway traceId does not transmit #3411 (#3446)
- Gateway compatible with downstream loss (#3445)
- Provide cassandra java driver 3.x plugin (#3410)
- Fix SpringMVC4 NoSuchMethodError (#3408)
- BugFix: endpoint grouping rules may be not unique (#3510)
- Add feature to control the maximum agent log files (#3475)
- Agent support custom properties. (#3367)
- Add Light4j plugin (#3323)

#### Document
- Remove travis badge (#3763)
- Replace user wall to typical users in readme page (#3719)
- Update istio docs according latest istio release (#3646)
- Use chart deploy sw docs (#3573)
- Reorganize the doc, and provide catalog (#3563)
- Committer vote and set up document. (#3496)
- Update als setup doc as istio 1.3 released (#3470)
- Fill faq reply in official document. (#3450)

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/36?closed=1)


6.4.0
------------------

#### Project
* Highly recommend to upgrade due to Pxx metrics calculation bug.
* Make agent working in JDK9+ Module system.

#### Java Agent
* Make agent working in JDK9+ Module system.
* Support Kafka 2.x client libs.
* Log error in OKHTTP OnFailure callback.
* Support injecting traceid into logstack appender in logback.
* Add OperationName(including endpoint name) length max threshold.
* Support using Regex to group operation name. 
* Support Undertow routing handler.
* RestTemplate plugin support operation name grouping.
* Fix ClassCastException in Webflux plugin.
* Ordering zookeeper server list, to make it better in topology.
* Fix a Dubbo plugin incompatible issue.
* Fix MySQL 5 plugin issue.
* Make log writer cached.
* Optimize Spring Cloud Gateway plugin
* Fix and improve gRPC reconnect mechanism.
* Remove Disruptor dependency from agent.

#### Backend
* Fix Pxx(p50,p75,p90,p95,p99) metrics func bug.(Critical)
* Support Gateway in backend analysis, even when it doesn't have suitable language agent.
* Support using HTTPs SSL accessing ElasticSearch storage.
* Support Zookeeper ACL.
* Make alarm records listed in order.
* Fix Pxx data persistence failure in some cases.
* Fix some bugs in MySQL storage.
* Setup slow SQL length threshold.
* Fix TTL settings is not working as expected.
* Remove scope-meta file.

#### UI
* Enhance alarm page layout.
* Support trace tree chart resize.
* Support trace auto completion when partial traces abandoned somehow.
* Fix dashboard endpoint slow chart.
* Add radial chart in topology page.
* Add trace table mode.
* Fix topology page bug.
* Fix calender js bug.
* Fix "The "topo-services" component did not update the data in time after modifying the time range on the topology page.

#### Document
* Restore the broken Istio setup doc.
* Add etcd config center document.
* Correct span_limit_per_segment default value in document.
* Enhance plugin develop doc.
* Fix error description in build document.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/35?closed=1)


6.3.0
------------------

#### Project
* e2e tests have been added, and verify every pull request.
* Use ArrayList to replace LinkedList in DataCarrier for much better performance.
* Add plugin instrumentation definition check in CI.
* DataCarrier performance improvement by avoiding false-sharing.

#### Java Agent
* Java agent supports JDK 9 - 12, but don't support Java Module yet.
* Support JVM class auto instrumentation, cataloged as bootstrap plugin.
* Support JVM HttpClient and HttpsClient plugin.[Optional]
* Support backend upgrade without rebooting required.
* Open Redefine and Retransform by other agents.
* Support Servlet 2.5 in Jetty, Tomcat and SpringMVC plugins.
* Support Spring @Async plugin.
* Add new config item to restrict the length of span#peer.
* Refactor `ContextManager#stopSpan`.
* Add gRPC timeout.
* Support Logback AsyncAppender print tid 
* Fix gRPC reconnect bug.
* Fix trace segment service doesn't report `onComplete`.
* Fix wrong logger class name.
* Fix gRPC plugin bug.
* Fix `ContextManager.activeSpan()` API usage error.

#### Backend
* Support agent reset command downstream when the storage is erased, mostly because of backend upgrade.
* Backend stream flow refactor.
* High dimensionality metrics(Hour/Day/Month) are changed to lower priority, to ease the storage payload.
* Add OAP metrics cache to ease the storage query payload and improve performance.
* Remove DataCarrier in trace persistent of ElasticSearch storage, by leveraging the elasticsearch bulk queue.
* OAP internal communication protocol changed. Don't be compatible with old releases.
* Improve ElasticSearch storage bulk performance.
* Support etcd as dynamic configuration center.
* Simplify the PxxMetrics and ThermodynamicMetrics functions for better performance and GC.
* Support JVM metrics self observability.
* Add the new OAL runtime engine.
* Add gRPC timeout.
* Add Charset in the alarm web hook.
* Fix buffer lost.
* Fix dirty read in ElasticSearch storage.
* Fix bug of cluster management plugins in un-Mixed mode.
* Fix wrong logger class name.
* Fix delete bug in ElasticSearch when using namespace.
* Fix MySQL TTL failure.
* Totally remove `IDs can't be null` log, to avoid misleading.
* Fix provider has been initialized repeatedly.
* Adjust providers conflict log message.
* Fix using wrong gc time metrics in OAL.

#### UI
* Fix refresh is not working after endpoint and instance changed.
* Fix endpoint selector but.
* Fix wrong copy value in slow traces.
* Fix can't show trace when it is broken partially(Because of agent sampling or fail safe).
* Fix database and response time graph bugs.

#### Document
* Add bootstrap plugin development document.
* Alarm documentation typo fixed.
* Clarify the Docker file purpose.
* Fix a license typo.


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/34?closed=1)


6.2.0
------------------

#### Project
* ElasticSearch implementation performance improved, and CHANGED totally. Must delete all existing indexes to do upgrade.
* CI and Integration tests provided by ASF INFRA.
* Plan to enhance tests including e2e, plugin tests in all pull requests, powered by ASF INFRA.
* DataCarrier queue write index controller performance improvement. 3-5 times quicker than before.
* Add windows compile support in CI.

#### Java Agent
* Support collect SQL parameter in MySQL plugin.[Optional]
* Support SolrJ plugin.
* Support RESTEasy plugin.
* Support Spring Gateway plugin for 2.1.x[Optional]
* TracingContext performance improvement.
* Support Apache ShardingSphere(incubating) plugin.
* Support `span#error` in application toolkit.
* Fix OOM by empty stack of exception.
* FIx wrong cause exception of stack in span log.
* Fix unclear the running context in SpringMVC plugin.
* Fix CPU usage accessor calculation issue.
* Fix SpringMVC plugin span not stop bug when doing HTTP forward.
* Fix lettuce plugin async commend bug and NPE.
* Fix webflux plugin cast exception.
* [CI]Support `import` check.

#### Backend
* Support time serious ElasticSearch storage.
* Provide dynamic configuration module and implementation. Slow SQL threshold supports dynamic config today.
* Dynamic Configuration module provide multiple implementations, DCS(gRPC based), Zookeeper, Apollo, Nacos.
* Provide P99/95/90/75/50 charts in topology edge.
* New topology query protocol and implementation.
* Support Envoy ALS in Service Mesh scenario.
* Support Nacos cluster management.
* Enhance metric exporter. Run in increment and total modes.
* Fix module provider is loaded repeatedly.
* Change TOP slow SQL storage in ES to Text from Keyword, as too long text issue.
* Fix H2TopologyQuery tiny bug.
* Fix H2 log query bug.(No feature provided yet)
* Filtering pods not in 'Running' phase in mesh scenario.
* Fix query alarm bug in MySQL and H2 storage.
* Codes refactor.

#### UI
* Fix some `ID is null` query(s).
* Page refactor, especially time-picker, more friendly.
* Login removed.
* Trace timestamp visualization issue fixed.
* Provide P99/95/90/75/50 charts in topology edge.
* Change all P99/95/90/75/50 charts style. More readable.
* Fix 404 in trace page.

#### Document
* Go2Sky project has been donated to SkyAPM, change document link.
* Add FAQ for ElasticSearch storage, and links from document.
* Add FAQ fro WebSphere installation.
* Add several open users.
* Add alarm webhook document.


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/33?closed=1)

6.1.0
------------------

#### Project
**SkyWalking graduated as Apache Top Level Project**.
- Support compiling project agent, backend, UI separately.

#### Java Agent
- Support Vert.x Core 3.x plugin.
- Support Apache Dubbo plugin.
- Support `use_qualified_name_as_endpoint_name` and `use_qualified_name_as_operation_name` configs in SpringMVC plugin.
- Support span async close APIs in core. Used in Vert.x plugin.
- Support MySQL 5,8 plugins.
- Support set instance id manually(optional).
- Support customize enhance trace plugin in optional list.
- Support to set peer in Entry Span.
- Support Zookeeper plugin.
- Fix Webflux plugin created unexpected Entry Span. 
- Fix Kafka plugin NPE in Kafka 1.1+
- Fix wrong operation name in postgre 8.x plugin.
- Fix RabbitMQ plugin NPE.
- Fix agent can't run in JVM 6/7, remove `module-info.class`.
- Fix agent can't work well, if there is whitespace in agent path.
- Fix Spring annotation bug and inheritance enhance issue.
- Fix CPU accessor bug.

#### Backend
**Performance improved, especially in CPU limited environment. 3x improvement in service mesh scenario(no trace) in 8C16G VM. 
Significantly cost less CPU in low payload.**

- Support database metrics and SLOW SQL detection.
- Support to set max size of metadata query. And change default to 5000 from 100.
- Support ElasticSearch template for new feature in the future.
- Support shutdown Zipkin trace analysis, because it doesn't fit production environment.
- Support log type, scope HTTP_ACCESS_LOG and query. No feature provided, prepare for future  versions.
- Support .NET clr receiver.
- Support Jaeger trace format, no analysis.
- Support group endpoint name by regax rules in mesh receiver.
- Support `disable` statement in OAL.
- Support basic auth in ElasticSearch connection.
- Support metrics exporter module and gRPC implementor.
- Support `>, <, >=, <=` in OAL.
- Support role mode in backend.
- Support Envoy metrics.
- Support query segment by service instance.
- Support to set host/port manually at cluster coordinator, rather than based on core settings.
- Make sure OAP shutdown when it faces startup error.
- Support set separated gRPC/Jetty ip:port for receiver, default still use core settings.
- Fix JVM receiver bug.
- Fix wrong dest service in mesh analysis.
- Fix search doesn't work as expected.
- Refactor `ScopeDeclaration` annotation.
- Refactor register lock mechanism.
- Add SmartSql component for .NET
- Add integration tests for ElasticSearch client.
- Add test cases for exporter.
- Add test cases for queue consume.

#### UI
- RocketBot UI has been accepted and bind in this release.
- Support CLR metrics.

#### Document
- Documents updated, matching Top Level Project requirement.
- UI licenses updated, according to RocketBot UI IP clearance.
- User wall and powered-by list updated.
- CN documents removed, only consider to provide by volunteer out of Apache.


All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/32?closed=1)


6.0.0-GA
------------------

#### Java Agent
- Support gson plugin(optional).
- Support canal plugin.
- Fix missing ojdbc component id.
- Fix dubbo plugin conflict.
- Fix OpenTracing tag match bug.
- Fix a missing check in ignore plugin.

#### Backend
- Adjust service inventory entity, to add properties.
- Adjust service instance inventory entity, to add properties.
- Add nodeType to service inventory entity.
- Fix when operation name of local and exit spans in ref, the segment lost.
- Fix the index names don't show right in logs. 
- Fix wrong alarm text.
- Add test case for span limit mechanism.
- Add telemetry module and prometheus implementation, with grafana setting.
- A refactor for register API in storage module.
- Fix H2 and MySQL endpoint dependency map miss upstream side.
- Optimize the inventory register and refactor the implementation.
- Speed up the trace buffer read.
- Fix and removed unnecessary inventory register operations.

#### UI
- Add new trace view.
- Add word-break to tag value. 

#### Document
- Add two startup modes document.
- Add PHP agent links.
- Add some cn documents.
- Update year to 2019
- User wall updated.
- Fix a wrong description in `how-to-build` doc.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/30?closed=1)

6.0.0-beta
------------------

#### Protocol
- Provide Trace Data Protocol v2
- Provide SkyWalking Cross Process Propagation Headers Protocol v2.

#### Java Agent
- Support Trace Data Protocol v2
- Support SkyWalking Cross Process Propagation Headers Protocol v2.
- Support SkyWalking Cross Process Propagation Headers Protocol v1 running in compatible way. Need declare open explicitly.
- Support SpringMVC 5
- Support webflux
- Support a new way to override agent.config by system env. 
- Span tag can override by explicit way.
- Fix Spring Controller Inherit issue.
- Fix ElasticSearch plugin NPE.
- Fix agent classloader dead lock in certain situation.
- Fix agent log typo.
- Fix wrong component id in resettemplete plugin.
- Fix use transform `ignore()` in wrong way.
- Fix H2 query bug.

#### Backend
- Support Trace Data Protocol v2. And Trace Data Protocol v1 is still supported.
- Support MySQL as storage.
- Support TiDB as storage.
- Support a new way to override application.yml by system env.
- Support service instance and endpoint alarm.
- Support namespace in istio receiver.
- Support service throughput(cpm), successful rate(sla), avg response time and p99/p95/p90/p75/p50 response time.
- Support backend trace sampling.
- Support Zipkin format again.
- Support init mode.
- Support namespace in Zookeeper cluster management.
- Support consul plugin in cluster module.
- OAL generate tool has been integrated into main repo, in the maven `compile` stage.
- Optimize trace paging query.
- Fix trace query don't use fuzzy query in ElasticSearch storage.
- Fix alarm can't be active in right way.
- Fix unnecessary condition in database and cache number query.
- Fix wrong namespace bug in ElasticSearch storage.
- Fix `Remote clients selector error: / by zero `.
- Fix segment TTL is not working.

#### UI
- Support service throughput(cpm), successful rate(sla), avg response time and p99/p95/p90/p75/p50 response time.
- Fix TopN endpoint link doesn't work right.
- Fix trace stack style.
- Fix CI.

#### Document
- Add more agent setting documents.
- Add more contribution documents.
- Update user wall and powered-by page.
- Add RocketBot UI project link in document.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/31?closed=1)

6.0.0-alpha
------------------

SkyWalking 6 is totally new milestone for the project. At this point, we are not just a distributing
tracing system with analysis and visualization capabilities. We are an **Observability Analysis Platform(OAL)**.

The core and most important features in v6 are
1. Support to collect telemetry data from different sources, such as multiple language agents and service mesh.
1. Extensible stream analysis core. Make SQL and cache analysis available in core level, although haven't
provided in this release.
1. Provide **Observability Analysis Language(OAL)** to make analysis metrics customization available.
1. New GraphQL query protocol. Not binding with UI now.
1. UI topology is better now.
1. New alarm core provided. In alpha, only on service related metrics.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/29?closed=1)

5.x releases
------------------
You could find all CHANGES of 5.x at [here](https://github.com/apache/skywalking/blob/5.x/CHANGES.md)
