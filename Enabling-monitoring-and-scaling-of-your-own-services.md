You can use OpenShift Monitoring for your own services in addition to monitoring the cluster. This way, you do not need to use an additional monitoring solution. This helps keeping monitoring centralized. Additionally, you can extend the access to the metrics of your services beyond cluster administrators. This enables developers and arbitrary users to access these metrics.

This is based on OpenShift 4.3. At this time it is only a Technical Preview. See https://docs.openshift.com/container-platform/4.3/monitoring/monitoring-your-own-services.html

## Enabling monitoring of your own services

Make sure you are logged in as cluster-admin.

```bash
cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    techPreviewUserWorkload:
      enabled: true
EOF
```

and check that the prometheus-user-workload pods were created

```bash
oc get pod -n openshift-user-workload-monitoring 
NAME                                   READY   STATUS    RESTARTS   AGE
prometheus-operator-7bcc9cc899-p8cbr   1/1     Running   1          10h
prometheus-user-workload-0             5/5     Running   6          10h
prometheus-user-workload-1             5/5     Running   6          10h
```

## Create metrics colletion role

Create a new role for setting up metrics collection.

```bash
$ cat <<EOF | oc apply -f -
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
 name: monitor-crd-edit
rules:
- apiGroups: ["monitoring.coreos.com"]
  resources: ["prometheusrules", "servicemonitors", "podmonitors"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
EOF
```

## Create a new project

Create a new project (monitor-demo) and give a normal user (developer) admin rights onto the project. Add the new created role to the user.

```bash
$ oc new-project monitor-demo
You can add applications to this project with the 'new-app' command. For example, try:

    oc new-app django-psql-example

to build a new example application in Python. Or use kubectl to deploy a simple Kubernetes application:

    kubectl create deployment hello-node --image=gcr.io/hello-minikube-zero-install/hello-node
$ oc policy add-role-to-user admin developer -n monitor-demo 
clusterrole.rbac.authorization.k8s.io/admin added: "developer"
$ oc policy add-role-to-user monitor-crd-edit developer -n monitor-demo 
clusterrole.rbac.authorization.k8s.io/monitor-crd-edit added: "developer"
```

## Login as the normal user

```bash
$ oc login -u developer
Authentication required for https://api.rbaumgar.demo.net:6443 (openshift)
Username: developer
Password: 
Login successful.

You have one project on this server: "monitor-demo"

Using project "monitor-demo".
```

## Create a sample application

Deploying a sample application *monitor-demo-app* end expose a route.

The application is based on an example you will find at [GitHub - rbaumgar/monitor-demo-app: Quarkus demo app to show Application Performance Monitoring(APM)](https://github.com/rbaumgar/monitor-demo-app) 

```bash
cat <<EOF |oc apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: monitor-demo-app
  name: monitor-demo-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: monitor-demo-app
  template:
    metadata:
      labels:
        app: monitor-demo-app
    spec:
      containers:
      - image: quay.io/rbaumgar/monitor-demo-app-jvm
        imagePullPolicy: IfNotPresent
        name: monitor-demo-app
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: monitor-demo-app
  name: monitor-demo-app
spec:
  ports:
  - port: 8080
    protocol: TCP
    targetPort: 8080
    name: web
  selector:
    app: monitor-demo-app
  type: ClusterIP
EOF
deployment.apps/monitor-demo-app created
service/monitor-demo-app created
```

```bash
$ oc expose svc monitor-demo-app 
route.route.openshift.io/monitor-demo-app exposed
```

:star: It is very important, that you define labels at the Deployment and Service. Those will later be referenced!

# Check new monitoring app

Check the router url with */hello* and see the hello message and the pod name. Do this multiple times.

```bash
export URL=$(oc get route monitor-demo-app -o jsonpath='{.spec.host}')
curl $URL/hello
hello from monitor-demo-app monitor-demo-app-78fc685c94-mtm28
```

# Check the availalable metrics

See all available metrics */metrics* and only application specific metrics */metrics/application*.

```bash
curl $URL/metrics/application
# HELP application_org_example_rbaumgar_GreetingResource_greetings_total How many greetings we've given.
# TYPE application_org_example_rbaumgar_GreetingResource_greetings_total counter
application_org_example_rbaumgar_GreetingResource_greetings_total 1.0
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_rate_per_second gauge
application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_rate_per_second 0.0
# TYPE application_org_example_rbaumgar_PrimeNumberChecker_checksTimer_one_min_rate_per_second gauge
...
```

 With *greetings_total* you will see how often you have called the */hello* url.

###### Setting up metrics collection

To use the metrics exposed by your service, you need to configure OpenShift Monitoring to scrape metrics from the /metrics endpoint. You can do this using a ServiceMonitor, a custom resource definition (CRD) that specifies how a service should be monitored, or a PodMonitor, a CRD that specifies how a pod should be monitored. The former requires a Service object, while the latter does not, allowing Prometheus to directly scrape metrics from the metrics endpoint exposed by a Pod.

```bash
cat <<EOF | oc apply -f -
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  labels:
    k8s-app: monitor-demo-monitor
  name: monitor-demo-monitor
spec:
  endpoints:
  - interval: 30s
    port: web
    scheme: http
  selector:
    matchLabels:
      app: monitor-demo-app
EOF
servicemonitor.monitoring.coreos.com/monitor-demo-monitor created
$ oc get servicemonitor
NAME                   AGE
monitor-demo-monitor   42s
```

:star: The *matchLabels* must be the same like you defined at the Deployment and Service!

## Accessing the metrics of your service

Once you have enabled monitoring your own services, deployed a service, and set up metrics collection for it, you can access the metrics of the service as a cluster administrator, as a developer, or as a user with view permissions for the project.

1. Access the Prometheus web interface:
   
   - To access the metrics as a cluster administrator, go to the OpenShift Container Platform web console, switch to the Administrator Perspective, and click **Monitoring** → **Metrics**.
     
     :star: Cluster administrators, when using the Administrator Perspective, have access to all cluster metrics and to custom service metrics from all projects.
     
     :star: Only cluster administrators have access to the Alertmanager and Prometheus UIs.
   
   - To access the metrics as a developer or a user with permissions, go to the OpenShift Container Platform web console, switch to the Developer Perspective, then click **Advanced** → **Metrics**. Select the project you want to see the metrics for.
     
     :star: Developers can only use the Developer Perspective. They can only query metrics from a single project.

2. Use the PromQL interface to run queries for your services.

Here is an example:

![](/home/rbaumgar/demo/quarkus/monitor-demo-app/images/metrics_view.png)

You can generate load onto your application, so will see more on the graph.

> ```bash
> $ for i in {1..1000}; do curl $URL/hello; sleep 10; done
> ```

Example: If you want to see the nummer of requests per second (rated in 2 minutes) on the sample service, you can use following query

> sum(rate(application_org_example_rbaumgar_GreetingResource_greetings_total{namespace="monitor-demo"}[2m]))

# Exposing custom application metrics for autoscaling

You can export custom application metrics for the horizontal pod autoscaler.

This is based on OpenShift 4.3 Prometheus Adapter. Prometheus Adapter is a Technology Preview feature only. See [Exposing custom application metrics for autoscaling | Monitoring | OpenShift Container Platform 4.3](https://docs.openshift.com/container-platform/4.3/monitoring/exposing-custom-application-metrics-for-autoscaling.html)

## Create service account

Create a new service account for your Prometheus Adapter in the user namespace.

```bash
$cat <<EOF | oc apply -f -
kind: ServiceAccount
apiVersion: v1
metadata:
  name: custom-metrics-apiserver
EOF
serviceaccount/custom-metrics-apiserver created
```

## Create new required cluster roles

Login again as cluster admin!

> ```bash
> cat <<EOF | oc apply -f -
> apiVersion: rbac.authorization.k8s.io/v1
> kind: ClusterRole
> metadata:
>   name: custom-metrics-server-resources
> rules:
> - apiGroups:
>   - custom.metrics.k8s.io
>   resources: ["*"]
>   verbs: ["*"]
> ---
> apiVersion: rbac.authorization.k8s.io/v1
> kind: ClusterRole
> metadata:
>   name: custom-metrics-resource-reader
> rules:
> - apiGroups:
>   - ""
>   resources:
>   - namespaces
>   - pods
>   - services
>   verbs:
>   - get
>   - list
> EOF
> clusterrole.rbac.authorization.k8s.io/custom-metrics-server-resources created
> clusterrole.rbac.authorization.k8s.io/custom-metrics-resource-reader created
> ```

Add the newly created (cluster-) role bindings for the service account.

```bash
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: custom-metrics-auth-reader
  namespace: kube-system
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: extension-apiserver-authentication-reader
subjects:
- kind: ServiceAccount
  name: custom-metrics-apiserver
  namespace: monitor-demo
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: custom-metrics-resource-reader
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: custom-metrics-resource-reader
subjects:
- kind: ServiceAccount
  name: custom-metrics-apiserver
  namespace: monitor-demo
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: hpa-controller-custom-metrics
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: custom-metrics-server-resources
subjects:
- kind: ServiceAccount
  name: horizontal-pod-autoscaler
  namespace: kube-system
EOF
rolebinding.rbac.authorization.k8s.io/custom-metrics-auth-reader created
clusterrolebinding.rbac.authorization.k8s.io/custom-metrics-resource-reader created
clusterrolebinding.rbac.authorization.k8s.io/hpa-controller-custom-metrics created
```

:star: If you are using a different namespace, please replace the namespace. (montor-demo)

## You need an additional role, whih is currently not documented

```bash
cat <<EOF | oc apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: custom-metrics:system:auth-delegator
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: system:auth-delegator
subjects:
- kind: ServiceAccount
  name: custom-metrics-apiserver
  namespace: monitor-demo
EOF
```

```
logging error output: "Internal Server Error: \"/apis/custom.metrics.k8s.io/v1beta1?timeout=32s\": subjectaccessreviews.authorization.k8s.io is forbidden: User \"system:serviceaccount:monitor-demo:custom-metrics-apiserver\" cannot create resource \"subjectaccessreviews\" in API group \"authorization.k8s.io\" at the cluster scope\n"
 [hyperkube/v1.16.2 (linux/amd64) kubernetes/ebf9a26/controller-discovery 10.128.0.1:43004]
E0414 10:43:35.168164       1 webhook.go:196] Failed to make webhook authorizer request: subjectaccessreviews.authorization.k8s.io is forbidden: User "system:serviceaccount:monitor-demo:custom-metrics-apiserver" cannot create resource "subjectaccessreviews" in API group "authorization.k8s.io" at the cluster scope
E0414 10:43:35.168288       1 errors.go:77] subjectaccessreviews.authorization.k8s.io is forbidden: User "system:serviceaccount:monitor-demo:custom-metrics-apiserver" cannot create resource "subjectaccessreviews" in API group "authorization.k8s.io" at the cluster scope
I0414 10:43:35.168323       1 wrap.go:47] GET /apis/custom.metrics.k8s.io/v1beta1?timeout=32s: (1.963244ms) 500
```

## Create an APIService for the custom metrics for Prometheus Adapter

```bash
cat <<EOF | oc apply -f -
apiVersion: apiregistration.k8s.io/v1beta1
kind: APIService
metadata:
  name: v1beta1.custom.metrics.k8s.io
spec:
  service:
    name: prometheus-adapter
    namespace: monitor-demo
  group: custom.metrics.k8s.io
  version: v1beta1
  insecureSkipTLSVerify: true
  groupPriorityMinimum: 100
  versionPriority: 100
EOF
apiservice.apiregistration.k8s.io/v1beta1.custom.metrics.k8s.io created
```

:star: If you are using a different namespace, pleace replace the namespace. (montor-demo)

## Show the Prometheus Adapter image to use.

Will be required later!

```bash
$ oc get -n openshift-monitoring deploy/prometheus-adapter -o jsonpath="{..image}"
quay.io/openshift-release-dev/ocp-v4.0-art-dev@sha256:a8e3c383b36684a28453a4f5bb65863167bbeb409b91c9c3f5f50e1d5e923dc9
```

Create Prometheus Adapater

## Login as the normal user

Make sure you stay in the right namespace.

```bash
$ oc login -u developer
Authentication required for https://api.rbaumgar.demo.net:6443 (openshift)
Username: developer
Password: 
Login successful.

You have one project on this server: "monitor-demo"

Using project "monitor-demo".
```

## Create a config map for the custom metrics for Prometheus Adapter

```bash
cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: adapter-config
data:
  config.yaml: |
    rules:
    - seriesQuery: 'http_requests_total{namespace!="",pod!=""}' 
      resources:
        overrides:
          namespace: {resource: "namespace"}
          pod: {resource: "pod"}
          service: {resource: "service"}
      name:
        matches: "^(.*)_total"
        as: "${1}_per_second" 
      metricsQuery: 'sum(rate(<<.Series>>{<<.LabelMatchers>>}[2m])) by (<<.GroupBy>>)'
EOF
configmap/adapter-config created
```

## Create a service and an APIService for the custom metrics for Prometheus Adapter

```bash
cat <<EOF | oc apply -f -
apiVersion: v1
kind: Service
metadata:
  annotations:
    service.alpha.openshift.io/serving-cert-secret-name: prometheus-adapter-tls
  labels:
    name: prometheus-adapter
  name: prometheus-adapter
spec:
  ports:
  - name: https
    port: 443
    targetPort: 6443
  selector:
    app: prometheus-adapter
  type: ClusterIP
EOF
service/prometheus-adapter created
```

## Create a configmap

```
$ cat <<EOF | oc apply -f -
kind: ConfigMap
apiVersion: v1
metadata:
  name: prometheus-adapter-prometheus-config
data:
  prometheus-config.yaml: |
    apiVersion: v1
    clusters:
    - cluster:
        server: https://thanos-querier.openshift-monitoring.svc:9092
        insecure-skip-tls-verify: true
      name: prometheus-k8s
    contexts:
    - context:
        cluster: prometheus-k8s
        user: prometheus-k8s
      name: prometheus-k8s
    current-context: prometheus-k8s
    kind: Config
    preferences: {}
    users:
    - name: prometheus-k8s
      user:
        tokenFile: /var/run/secrets/kubernetes.io/serviceaccount/token
```

## Configuration for deploying prometheus-adapter

:star: Replace the image name with the correct name you got! (spec.template.spec.containers.image)

```
$ cat <<EOF | oc apply -f - 
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: prometheus-adapter
  name: prometheus-adapter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus-adapter
  template:
    metadata:
      labels:
        app: prometheus-adapter
      name: prometheus-adapter
    spec:
      serviceAccountName: custom-metrics-apiserver
      containers:
      - name: prometheus-adapter
        image: openshift-release-dev/ocp-v4.3-art-dev 
        args:
        - --prometheus-auth-config=/etc/prometheus-config/prometheus-config.yaml
        - --secure-port=6443
        - --tls-cert-file=/var/run/serving-cert/tls.crt
        - --tls-private-key-file=/var/run/serving-cert/tls.key
        - --logtostderr=true
        - --prometheus-url=https://thanos-querier.openshift-monitoring.svc:9092
        - --metrics-relist-interval=1m
        - --v=4
        - --config=/etc/adapter/config.yaml
        ports:
        - containerPort: 6443
        volumeMounts:
        - name: volume-serving-cert
          mountPath: /var/run/serving-cert
          readOnly: true
        - name: config
          mountPath: /etc/adapter/
          readOnly: true
        - name: prometheus-adapter-prometheus-config
          mountPath: /etc/prometheus-config
        - name: tmp-vol
          mountPath: /tmp
      volumes:
      - name: volume-serving-cert
        secret:
          secretName: prometheus-adapter-tls
      - name: config
        configMap:
          name: adapter-config
      - name: prometheus-adapter-prometheus-config
        configMap:
          name: prometheus-adapter-prometheus-config
          defaultMode: 420
      - name: tmp-vol
        emptyDir: {}
EOF
deployment.apps/prometheus-adapter created
```

## Problem:

I0416 14:41:03.620353 1 round_trippers.go:438] GET 
https://thanos-querier.openshift-monitoring.svc:9092/api/v1/series?match%5B%5D=application_org_example_rbaumgar_GreetingResource_greetings_total%7Bnamespace%21%3D%22%22%2Cpod%21%3D%22%22%7D&start=1587046863.618
 400 Bad Request in 1 milliseconds
I0416 14:41:03.620379 1 round_trippers.go:444] Response Headers:
I0416 14:41:03.620390 1 round_trippers.go:447] Content-Type: 
text/plain; charset=utf-8
I0416 14:41:03.620399 1 round_trippers.go:447] 
X-Content-Type-Options: nosniff
I0416 14:41:03.620407 1 round_trippers.go:447] Content-Length:
 56
I0416 14:41:03.620414 1 round_trippers.go:447] Date: Thu, 16 
Apr 2020 14:41:03 GMT
I0416 14:41:03.620432 1 api.go:74] GET 
https://thanos-querier.openshift-monitoring.svc:9092/api/v1/series?match%5B%5D=application_org_example_rbaumgar_GreetingResource_greetings_total%7Bnamespace%21%3D%22%22%2Cpod%21%3D%22%22%7D&start=1587046863.618
 400 Bad Request
I0416 14:41:03.620453 1 api.go:93] Response Body: Bad Request. The
 request or configuration is malformed.
E0416 14:41:03.620528 1 provider.go:209] unable to update list of 
all metrics: unable to fetch metrics for query 
"application_org_example_rbaumgar_GreetingResource_greetings_total{namespace!=\"\",pod!=\"\"}":
 bad_response: invalid character 'B' looking for beginning of value
I0416 14:41:05.446248 1 request.go:942] Request Body: 
{"kind":"SubjectAccessReview","apiVersion":"authorization.k8s.io/v1beta1","metadata":{"creationTimestamp":null},"spec":{"nonResourceAttributes":{"path":"/apis/custom.metrics.k8s.io/v1beta1","verb":"get"},"user":"system:serviceaccount:openshift-cluster-version:default","group":["system:serviceaccounts","system:serviceaccounts:openshift-cluster-version","system:authenticated"]},"status":{"allowed":false}}
I0416 14:41:05.446413 1 round_trippers.go:419] curl -k -v -XPOST 
-H "Accept: application/json, */*" -H "Content-Type: application/json" 
-H "User-Agent: cm-adapter/v0.0.0 (linux/amd64) kubernetes/$Format" -H 
"Authorization: Bearer 
eyJhbGciOiJSUzI1NiIsImtpZCI6IlJhQVQwSDNBbTdYRHRPRTdRZkVMbC1IMWZEZE45MDE1d0c5dmhxS2FLZlkifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJtb25pdG9yLWRlbW8iLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlY3JldC5uYW1lIjoiY3VzdG9tLW1ldHJpY3MtYXBpc2VydmVyLXRva2VuLXptMmtmIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImN1c3RvbS1tZXRyaWNzLWFwaXNlcnZlciIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjBhN2YzMTQ2LTAzNmYtNDJjYy05NDE4LWJiZmUzZGVmNGUwZCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDptb25pdG9yLWRlbW86Y3VzdG9tLW1ldHJpY3MtYXBpc2VydmVyIn0.RexN6c4kPOzdK3dgE2oJItzxg99E9G5PDEyJ_-sB7MF3yF5fOGfGE9iqw8uIyzz70iAOWenqmgQ0WU84gO5ns6DzAULzuwwFB9xJVb2Np3_wnblJHhKBlFl8RM8xuBXw7WHC9IcWQ_AaSn0L_w-FcjdwGQjFw3YjfTELkJpooXR8X-kg1wW-AUIyY1L3IOWJvS0G2y4FQK2StAPjfk9fhfHhdiMeOjY6BxjBBN3MHK7xKGJn9Y0Ccku4-Tzo4DVVV2C4PVUtAYuq0ttQHC_AKyEQyUNTiyK37J_PMq-ayqiZv_u4OweH-AqpKbCfuRIcj4EkPHJVpA5eksIz5l7Iqg"

'https://172.30.0.1:443/apis/authorization.k8s.io/v1beta1/subjectaccessreviews'
I0416 14:41:05.449325 1 round_trippers.go:438] POST 
https://172.30.0.1:443/apis/authorization.k8s.io/v1beta1/subjectaccessreviews
 201 Created in 2 milliseconds
I0416 14:41:05.449346 1 round_trippers.go:444] Response Headers:
I0416 14:41:05.449356 1 round_trippers.go:447] Content-Length:
 564
I0416 14:41:05.449363 1 round_trippers.go:447] Date: Thu, 16 
Apr 2020 14:41:05 GMT
I0416 14:41:05.449371 1 round_trippers.go:447] Audit-Id: 
c71e5f53-2417-4bc7-a724-bd835457af3c
I0416 14:41:05.449379 1 round_trippers.go:447] Cache-Control: 
no-cache, private
I0416 14:41:05.449387 1 round_trippers.go:447] Content-Type: 
application/json
I0416 14:41:05.449454 1 request.go:942] Response Body: 
{"kind":"SubjectAccessReview","apiVersion":"authorization.k8s.io/v1beta1","metadata":{"creationTimestamp":null},"spec":{"nonResourceAttributes":{"path":"/apis/custom.metrics.k8s.io/v1beta1","verb":"get"},"user":"system:serviceaccount:openshift-cluster-version:default","group":["system:serviceaccounts","system:serviceaccounts:openshift-cluster-version","system:authenticated"]},"status":{"allowed":true,"reason":"RBAC:
 allowed by ClusterRoleBinding \"system:openshift:discovery\" of 
ClusterRole \"system:openshift:discovery\" to Group 
\"system:authenticated\""}}



## check custom.metrics

```
GET /apis/custom-metrics/v1alpha1/namespaces/webapp/metrics/queue-length
```

oc get pod

check log

oc logs 

### Double-Checking Your Work

With that all set, your custom metrics API should show up in discovery.

Try fetching the discovery information for it:

```
$ kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1
```

Since you've set up Prometheus to collect your app's metrics, you should
see a `pods/http_request` resource show up. This represents the `http_requests_total` metric, converted into a rate, aggregated to have
one datapoint per pod. Notice that this translates to the same API that
our HorizontalPodAutoscaler was trying to use above.

You can check the value of the metric using `kubectl get --raw`, which
sends a raw GET request to the Kubernetes API server, automatically
injecting auth information:

```
$ kubectl get --raw "/apis/custom.metrics.k8s.io/v1beta1/namespaces/default/pods/*/http_requests?selector=app%3Dsample-app"
```

Because of the adapter's configuration, the cumulative metric `http_requests_total` has been converted into a rate metric, `pods/http_requests`, which measures requests per second over a 2 minute
interval. The value should currently be close to zero, since there's no
traffic to your app, except for the regular metrics collection from
Prometheus.
