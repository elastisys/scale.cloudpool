### Learning from kubectl
It is possible to have `kubectl` output all requests made to the apiserver.
This serves as a good source for learning raw REST API use.


#### Seeing REST API requests
To get debug output and see all requests made from `kubectl`, 
use `--v=64` or similar. For example: 

    kubectl scale --replicas=1 rc/nginx --v=64


#### See REST API responses

Most commands allow you to specify the output format. To see the
raw API results, use `-o=json`. For example,

    kubectl get pods -o=json


#### Authentication

With auth token:

    curl --insecure --header "Authorization: Bearer $(cat token)" <url>

With certificate

	curl --cacert ~/.minikube/ca.crt --cert ~/.minikube/apiserver.crt --key ~/.minikube/apiserver.key <url>

or without verifying apiserver certificate

	curl --insecure --cert ~/.minikube/apiserver.crt --key ~/.minikube/apiserver.key <url>




### Sample API calls to manage a ReplicationController

First set `${CLIENT_CERT}` and `${CLIENT_KEY}` to refer to 
an admin cert and key for the apiserver. Also set `${APISERVER}`
to the base URL of the apiserver, such as `https://<host>:<port>`.
	
#### Get metadata about replication controller

The command `kubectl get rc -n default nginx -o json` would correspond to 
a REST API call similar to: 

    curl --insecure --cert ${CLIENT_CERT} --key ${CLIENT_KEY} ${APISERVER}/api/v1/namespaces/default/replicationcontrollers/nginx
	
which may yield output similar to this. Pay special attention to the `selector`, 
which is used by the replication controller to define the set of pods it 
controls. If empty, it defaults to `template.metadata.labels`.

```
{
  "kind": "ReplicationController",
  "apiVersion": "v1",
  "metadata": {
    "name": "nginx",
    "namespace": "default",
    "selfLink": "/api/v1/namespaces/default/replicationcontrollers/nginx",
    "uid": "3858fabd-f9cc-11e6-8130-080027ff7576",
    "resourceVersion": "15447",
    "generation": 1,
    "creationTimestamp": "2017-02-23T13:30:19Z",
    "labels": {
      "app": "nginx"
    }
  },
  "spec": {
    "replicas": 1,
    "selector": {
      "app": "nginx"
    },
    "template": {
      "metadata": {
        "name": "nginx",
        "creationTimestamp": null,
        "labels": {
          "app": "nginx"
        }
      },
      "spec": {
        "containers": [
          {
            "name": "nginx",
            "image": "nginx:1.11.10",
            "ports": [
              {
                "containerPort": 80,
                "protocol": "TCP"
              }
            ],
            "resources": {},
            "terminationMessagePath": "/dev/termination-log",
            "imagePullPolicy": "IfNotPresent"
          }
        ],
        "restartPolicy": "Always",
        "terminationGracePeriodSeconds": 30,
        "dnsPolicy": "ClusterFirst",
        "securityContext": {}
      }
    }
  },
  "status": {
    "replicas": 1,
    "fullyLabeledReplicas": 1,
    "readyReplicas": 1,
    "availableReplicas": 1,
    "observedGeneration": 1
  }
}
```

#### Get metadata about pod replicas

The command `kubectl get pods --selector="app=nginx" -n=default -o=json` would correspond to 
a REST API call similar to: 

    curl --insecure --cert ${CLIENT_CERT} --key ${CLIENT_KEY} ${APISERVER}/api/v1/namespaces/default/pods?labelSelector=app%3Dnginx

which might produce output similar to:

```
{
  "kind": "PodList",
  "apiVersion": "v1",
  "metadata": {
    "selfLink": "/api/v1/namespaces/default/pods",
    "resourceVersion": "16187"
  },
  "items": [
    {
      "metadata": {
        "name": "nginx-bm4sv",
        "generateName": "nginx-",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/pods/nginx-bm4sv",
        "uid": "385a2020-f9cc-11e6-8130-080027ff7576",
        "resourceVersion": "15446",
        "creationTimestamp": "2017-02-23T13:30:19Z",
        "labels": {
          "app": "nginx"
        },
        "annotations": {
          "kubernetes.io/created-by": "{\"kind\":\"SerializedReference\",\"apiVersion\":\"v1\",\"reference\":{\"kind\":\"ReplicationController\",\"namespace\":\"default\",\"name\":\"nginx\",\"uid\":\"3858fabd-f9cc-11e6-8130-080027ff7576\",\"apiVersion\":\"v1\",\"resourceVersion\":\"15433\"}}\n"
        },
        "ownerReferences": [
          {
            "apiVersion": "v1",
            "kind": "ReplicationController",
            "name": "nginx",
            "uid": "3858fabd-f9cc-11e6-8130-080027ff7576",
            "controller": true
          }
        ]
      },
      "spec": {
        "volumes": [
          {
            "name": "default-token-dr9g5",
            "secret": {
              "secretName": "default-token-dr9g5",
              "defaultMode": 420
            }
          }
        ],
        "containers": [
          {
            "name": "nginx",
            "image": "nginx:1.11.10",
            "ports": [
              {
                "containerPort": 80,
                "protocol": "TCP"
              }
            ],
            "resources": {},
            "volumeMounts": [
              {
                "name": "default-token-dr9g5",
                "readOnly": true,
                "mountPath": "/var/run/secrets/kubernetes.io/serviceaccount"
              }
            ],
            "terminationMessagePath": "/dev/termination-log",
            "imagePullPolicy": "IfNotPresent"
          }
        ],
        "restartPolicy": "Always",
        "terminationGracePeriodSeconds": 30,
        "dnsPolicy": "ClusterFirst",
        "serviceAccountName": "default",
        "serviceAccount": "default",
        "nodeName": "minikube",
        "securityContext": {}
      },
      "status": {
        "phase": "Running",
        "conditions": [
          {
            "type": "Initialized",
            "status": "True",
            "lastProbeTime": null,
            "lastTransitionTime": "2017-02-23T13:30:19Z"
          },
          {
            "type": "Ready",
            "status": "True",
            "lastProbeTime": null,
            "lastTransitionTime": "2017-02-23T13:30:20Z"
          },
          {
            "type": "PodScheduled",
            "status": "True",
            "lastProbeTime": null,
            "lastTransitionTime": "2017-02-23T13:30:19Z"
          }
        ],
        "hostIP": "192.168.99.100",
        "podIP": "172.17.0.4",
        "startTime": "2017-02-23T13:30:19Z",
        "containerStatuses": [
          {
            "name": "nginx",
            "state": {
              "running": {
                "startedAt": "2017-02-23T13:30:20Z"
              }
            },
            "lastState": {},
            "ready": true,
            "restartCount": 0,
            "image": "nginx:1.11.10",
            "imageID": "docker://sha256:db079554b4d2f7c65c4df3adae88cb72d051c8c3b8613eb44e86f60c945b1ca7",
            "containerID": "docker://698cb7ce47477b39a7696e838733d106c2ed42e8b4dcbd2e8bda3c33b578816d"
          }
        ]
      }
    },
	...
  ]
}
```

#### Scale number of pod replicas

The command `kubectl scale --replicas=2 -n default rc/nginx` would correspond to 
a REST API call similar to: 

    curl --insecure --cert ${CLIENT_CERT} --key ${CLIENT_KEY} -X PATCH --header 'Content-Type: application/merge-patch+json' -d '{"spec": {"replicas": 2}}' ${APISERVER}/api/v1/namespaces/default/replicationcontrollers/nginx





### Sample API calls to manage a Deployment

First set `${CLIENT_CERT}` and `${CLIENT_KEY}` to refer to 
an admin cert and key for the apiserver. Also set `${APISERVER}`
to the base URL of the apiserver, such as `https://<host>:<port>`.
	
#### Get metadata about deployment

The command `kubectl get deployment -n default nginx -o json` would correspond to 
a REST API call similar to: 

    curl --insecure --cert ${CLIENT_CERT} --key ${CLIENT_KEY} ${APISERVER}/apis/extensions/v1beta1/namespaces/default/deployments/nginx
	

which may yield output similar to this. Pay special attention to the `spec.selector`,
which is used by the deployment's replica set to define the set of pods it 
controls. If empty, it defaults to `spec.template.metadata.labels`. The 
`matchLabels` and `matchExpressions` are `AND`ed together.
See https://kubernetes.io/docs/user-guide/deployments/#selector.


```
{
  "kind": "Deployment",
  "apiVersion": "extensions/v1beta1",
  "metadata": {
    "name": "nginx",
    "namespace": "default",
    "selfLink": "/apis/extensions/v1beta1/namespaces/default/deployments/nginx",
    "uid": "83dcc018-fa60-11e6-8fbc-0800273bacdc",
    "resourceVersion": "2548",
    "generation": 1,
    "creationTimestamp": "2017-02-24T07:11:51Z",
    "labels": {
      "app": "nginx",
      "version": "1.11.10"
    },
    "annotations": {
      "deployment.kubernetes.io/revision": "1"
    }
  },
  "spec": {
    "replicas": 1,
    "selector": {
      "matchLabels": {
        "app": "nginx"
      },
      "matchExpressions": [
        {
          "key": "version",
          "operator": "In",
          "values": [
            "1.11.10",
            "1.11.11"
          ]
        }
      ]
    },
    "template": {
      "metadata": {
        "creationTimestamp": null,
        "labels": {
          "app": "nginx",
          "version": "1.11.10"
        }
      },
      "spec": {
        "containers": [
          {
            "name": "nginx",
            "image": "nginx:1.11.10",
            "ports": [
              {
                "containerPort": 80,
                "protocol": "TCP"
              }
            ],
            "resources": {},
            "terminationMessagePath": "/dev/termination-log",
            "imagePullPolicy": "IfNotPresent"
          }
        ],
        "restartPolicy": "Always",
        "terminationGracePeriodSeconds": 30,
        "dnsPolicy": "ClusterFirst",
        "securityContext": {}
      }
    },
    "strategy": {
      "type": "RollingUpdate",
      "rollingUpdate": {
        "maxUnavailable": 1,
        "maxSurge": 1
      }
    }
  },
  "status": {
    "observedGeneration": 1,
    "replicas": 1,
    "updatedReplicas": 1,
    "availableReplicas": 1,
    "conditions": [
      {
        "type": "Available",
        "status": "True",
        "lastUpdateTime": "2017-02-24T07:11:51Z",
        "lastTransitionTime": "2017-02-24T07:11:51Z",
        "reason": "MinimumReplicasAvailable",
        "message": "Deployment has minimum availability."
      }
    ]
  }
}
```


	

#### Get metadata about deployment replicas

The command `kubectl get pods --selector="app=nginx,version in (1.11.10,1.11.11)" -n=default -o=json` would correspond to 
a REST API call similar to: 

    curl --insecure --cert ${CLIENT_CERT} --key ${CLIENT_KEY} ${APISERVER}/api/v1/namespaces/default/pods?labelSelector=app%3Dnginx%2Cversion%20in%20%281.11.10%2C1.11.11%29


which might produce output similar to:

```
{
  "kind": "PodList",
  "apiVersion": "v1",
  "metadata": {
    "selfLink": "/api/v1/namespaces/default/pods",
    "resourceVersion": "3687"
  },
  "items": [
    {
      "metadata": {
        "name": "nginx-251118140-gb4np",
        "generateName": "nginx-251118140-",
        "namespace": "default",
        "selfLink": "/api/v1/namespaces/default/pods/nginx-251118140-gb4np",
        "uid": "83de052c-fa60-11e6-8fbc-0800273bacdc",
        "resourceVersion": "2546",
        "creationTimestamp": "2017-02-24T07:11:51Z",
        "labels": {
          "app": "nginx",
          "pod-template-hash": "251118140",
          "version": "1.11.10"
        },
        "annotations": {
          "kubernetes.io/created-by": "{\"kind\":\"SerializedReference\",\"apiVersion\":\"v1\",\"reference\":{\"kind\":\"ReplicaSet\",\"namespace\":\"default\",\"name\":\"nginx-251118140\",\"uid\":\"83dd205a-fa60-11e6-8fbc-0800273bacdc\",\"apiVersion\":\"extensions\",\"resourceVersion\":\"2529\"}}\n"
        },
        "ownerReferences": [
          {
            "apiVersion": "extensions/v1beta1",
            "kind": "ReplicaSet",
            "name": "nginx-251118140",
            "uid": "83dd205a-fa60-11e6-8fbc-0800273bacdc",
            "controller": true
          }
        ]
      },
      "spec": {
        "volumes": [
          {
            "name": "default-token-rfh14",
            "secret": {
              "secretName": "default-token-rfh14",
              "defaultMode": 420
            }
          }
        ],
        "containers": [
          {
            "name": "nginx",
            "image": "nginx:1.11.10",
            "ports": [
              {
                "containerPort": 80,
                "protocol": "TCP"
              }
            ],
            "resources": {},
            "volumeMounts": [
              {
                "name": "default-token-rfh14",
                "readOnly": true,
                "mountPath": "/var/run/secrets/kubernetes.io/serviceaccount"
              }
            ],
            "terminationMessagePath": "/dev/termination-log",
            "imagePullPolicy": "IfNotPresent"
          }
        ],
        "restartPolicy": "Always",
        "terminationGracePeriodSeconds": 30,
        "dnsPolicy": "ClusterFirst",
        "serviceAccountName": "default",
        "serviceAccount": "default",
        "nodeName": "minikube",
        "securityContext": {}
      },
      "status": {
        "phase": "Running",
        "conditions": [
          {
            "type": "Initialized",
            "status": "True",
            "lastProbeTime": null,
            "lastTransitionTime": "2017-02-24T07:11:51Z"
          },
          {
            "type": "Ready",
            "status": "True",
            "lastProbeTime": null,
            "lastTransitionTime": "2017-02-24T07:11:52Z"
          },
          {
            "type": "PodScheduled",
            "status": "True",
            "lastProbeTime": null,
            "lastTransitionTime": "2017-02-24T07:11:51Z"
          }
        ],
        "hostIP": "192.168.99.100",
        "podIP": "172.17.0.4",
        "startTime": "2017-02-24T07:11:51Z",
        "containerStatuses": [
          {
            "name": "nginx",
            "state": {
              "running": {
                "startedAt": "2017-02-24T07:11:52Z"
              }
            },
            "lastState": {},
            "ready": true,
            "restartCount": 0,
            "image": "nginx:1.11.10",
            "imageID": "docker://sha256:db079554b4d2f7c65c4df3adae88cb72d051c8c3b8613eb44e86f60c945b1ca7",
            "containerID": "docker://4bd19f936a56940de132ada9ad803e59bd8329fcafa46d28c8905dd2de407a49"
          }
        ]
      }
    }
  ]
}
```

#### Scale number of deployment replicas

The command `kubectl scale --replicas=2 -n default deployment/nginx` would correspond to 
a REST API call similar to: 

    curl --insecure --cert ${CLIENT_CERT} --key ${CLIENT_KEY} -X PATCH --header 'Content-Type: application/merge-patch+json' -d '{"spec": {"replicas": 2}}' ${APISERVER}/apis/extensions/v1beta1/namespaces/default/deployments/nginx
