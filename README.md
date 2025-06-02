# monitor-demo-app

Quarkus demo app to show Application Performance Monitoring(APM)

The application has two APIs */hello* and */prim*

# Interactive run

You can run the application by

```shell
$ mvn quarkus:dev
[INFO] Scanning for projects...
[INFO] 
[INFO] --------------------< org.example:monitor-demo-app >--------------------
[INFO] Building monitor-demo-app 1.1-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
Listening for transport dt_socket at address: 5005
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2022-01-18 13:32:31,110 INFO  [io.quarkus] (Quarkus Main Thread) monitor-demo-app-micro 1.1-SNAPSHOT on JVM (powered by Quarkus 2.6.2.Final) started in 1.481s. Listening on: http://localhost:8080

2022-01-18 13:32:31,112 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2022-01-18 13:32:31,113 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, kubernetes, resteasy, smallrye-context-propagation, smallrye-metrics, vertx]
```

and from another window

```shell
$ curl localhost:8080/hello
hello from monitor-demo-app unknown
$ curl localhost:8080/hello/2xx
Got 2xx Response
$ curl localhost:8080/hello/5xx
Got 5xx Response
$ curl -X POST localhost:8080/hello/alert-hook -H 'content-type: application/json' -d '{ "records": [ {"key": "key-1","value": "sales-lead-0003"}, {"key": "key-2","value": "sales-lead-0003"} ] }'
OK
```

*unknown* is because of running outside of a container. Running in a container *unknown* will be the container name.

Testing the prime checker

```shell
$ curl localhost:8080/prime/1
1 is not prime.
$ curl localhost:8080/prime/3
3 is prime.
$ curl localhost:8080/prime/6578394793
6578394793 is prime.
$ curl localhost:8080/prime/6578394797
6578394797 is not prime, is divisible by 13.
```

# Using metrics

You will get the metrics by */metrics*, */metrics/application*, */metrics/vendor* or */metrics/base*. E.g.

```shell
$ curl -H "Accept: application/json" -L localhost:8080/metrics/application

{
    "org.example.rbaumgar.PrimeNumberChecker.performedChecks": 5,
    "org.example.rbaumgar.PrimeNumberChecker.checksTimer": {
        "p99": 2.991601,
        "min": 0.072527,
        "max": 2.991601,
        "mean": 0.5104884004555529,
        "p50": 0.100322,
        "p999": 2.991601,
        "stddev": 0.9673168847668333,
        "p95": 2.991601,
        "p98": 2.991601,
        "p75": 0.228018,
        "fiveMinRate": 0.009309148273644256,
        "fifteenMinRate": 0.004555985922747539,
        "meanRate": 0.008758395349093426,
        "count": 5,
        "oneMinRate": 0.006104575965485283
    },
    "greetings": 1,
    "org.example.rbaumgar.PrimeNumberChecker.highestPrimeNumberSoFar": 6578394793
$ curl -L localhost:8080/metrics/applications
# HELP application_greetings_total How many greetings we've given.
# TYPE application_greetings_total counter
application_greetings_total 1.0
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_rate_per_second gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_rate_per_second 0.008020888995483285
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_one_min_rate_per_second gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_one_min_rate_per_second 0.0026530377782952724
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_five_min_rate_per_second gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_five_min_rate_per_second 0.007880023887936873
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_fifteen_min_rate_per_second gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_fifteen_min_rate_per_second 0.004309778023828962
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_min_seconds gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_min_seconds 7.2527E-5
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_max_seconds gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_max_seconds 0.002991601
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_mean_seconds gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_mean_seconds 5.104884004555529E-4
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_stddev_seconds gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_stddev_seconds 9.673168847668332E-4
# HELP application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds A measure of how long it takes to perform the primality test.
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds summary
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds_count 5.0
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds{quantile="0.5"} 1.00322E-4
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds{quantile="0.75"} 2.28018E-4
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds{quantile="0.95"} 0.002991601
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds{quantile="0.98"} 0.002991601
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds{quantile="0.99"} 0.002991601
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_seconds{quantile="0.999"} 0.002991601
# HELP application_org_example_rbaumgar_PrimeNumberChecker_highestPrimeNumberSoFar Highest prime number so far.
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_highestPrimeNumberSoFar gauge
application_org_example_rbaumgar_PrimeNumberChecker_highestPrimeNumberSoFar 6.578394793E9
# HELP application_org_example_rbaumgar_PrimeNumberChecker_performedChecks_total How many primality checks have been performed.
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_performedChecks_total counter
application_org_example_rbaumgar_PrimeNumberChecker_performedChecks_total 5.0

```

So the number of API calls to *hello* is **greetings**.

# Build an image with Podman

```shell
$ mvn clean package -DskipTests
...
$ podman build -f src/main/docker/Dockerfile.jvm -t quarkus/monitor-demo-app-jvm .
exec java -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -javaagent:/opt/agent-bond/agent-bond.jar=jmx_exporter{{9779:/opt/agent-bond/jmx_exporter_config.yml}} -XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:+ExitOnOutOfMemoryError -cp . -jar /deployments/app.jar
2020-04-17 08:15:33,649 INFO  [io.quarkus] (main) monitor-demo-app 1.0-SNAPSHOT (running on Quarkus 1.0.0.CR1) started in 0.593s. Listening on: http://0.0.0.0:8080
2020-04-17 08:15:33,667 INFO  [io.quarkus] (main) Profile prod activated. 
2020-04-17 08:15:33,667 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy, smallrye-metrics]
```

You can also use *docker*.

# Build a Quarkus native image

You need to install Graal VM and set the correct pointer.

```shell
$ mvn package -Pnative -DskipTests
$ ls file target/monitor-demo-app-1.1-SNAPSHOT-runner

$ target/monitor-demo-app-1.1-SNAPSHOT-runner

$ podman build -f src/main/docker/Dockerfile.native -t quarkus/monitor-demo-app-native .
 

```

```
$ oc new-build quay.io/quarkus/ubi-quarkus-native-binary-s2i:1.0 --binary --name=monitor-demo -l app=monitor-demo

This build uses the new Red Hat Universal Base Image, providing foundational software needed to run most applications, while staying at a reasonable size.

And then start and watch the build, which will take about a minute or two to complete:

$ oc start-build monitor-demo --from-file=target/monitor-demo-app-1.0-SNAPSHOT-runner --follow

Once that's done, we'll deploy it as an OpenShift application:

$ oc new-app monitor-demo

and expose it to the world:

$ oc expose service monitor-demo

Finally, make sure it's actually done rolling out:

$ oc rollout status -w dc/monitor-demo
```

# Run the image

```shell
$ podman run -i --rm -p 8080:8080 quarkus/monitor-demo-app-jvm
```

# Push the image to the registry

Find the *image id* and push it. You might need to login at first.

```shell
$ podman images localhost/quarkus/monitor-demo-app-jvm
REPOSITORY                               TAG      IMAGE ID       CREATED      SIZE
localhost/quarkus/monitor-demo-app-jvm   latest   0a68fa7e569f   2 days ago   108 MB
$ podman push `podman images localhost/quarkus/monitor-demo-app-jvm -q` docker://quay.io/rbaumgar/monitor-demo-app-jvm
```

# Test prime

```
while [ true ] ; do
        BITS=$(( ( RANDOM % 60 )  + 1 ))
        NUM=$(openssl prime -generate -bits $BITS)
        curl http://localhost:8080/prime/${NUM}
        sleep 2
done
```
