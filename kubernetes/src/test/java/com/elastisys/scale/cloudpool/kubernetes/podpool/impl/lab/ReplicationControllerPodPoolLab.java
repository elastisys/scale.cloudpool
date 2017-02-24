package com.elastisys.scale.cloudpool.kubernetes.podpool.impl.lab;

import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elastisys.scale.cloudpool.kubernetes.apiserver.ApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.apiserver.impl.StandardApiServerClient;
import com.elastisys.scale.cloudpool.kubernetes.config.AuthConfig;
import com.elastisys.scale.cloudpool.kubernetes.podpool.impl.ReplicationControllerPodPool;
import com.elastisys.scale.cloudpool.kubernetes.types.Pod;

/**
 * Simple lab program for experimenting with a
 * {@link ReplicationControllerPodPool}.
 */
public class ReplicationControllerPodPoolLab {
    private static final Logger LOG = LoggerFactory.getLogger(ReplicationControllerPodPoolLab.class);

    /** TODO: set to Kubernetes API server URL. */
    private static String apiServerUrl = "https://192.168.99.100:8443";
    /** TODO: set to client cert */
    private static String clientCertPath = Paths.get(System.getenv("HOME"), ".minikube", "apiserver.crt").toString();
    /** TODO: set to client key */
    private static String clientKeyPath = Paths.get(System.getenv("HOME"), ".minikube", "apiserver.key").toString();

    /** TODO: set to Kubernetes namespace where ReplicationController exists */
    private static String namespace = "default";
    /** TODO: set to name of ReplicationController */
    private static String name = "nginx-rc";

    public static void main(String[] args) {
        AuthConfig auth = AuthConfig.builder().certPath(clientCertPath).keyPath(clientKeyPath).build();
        ApiServerClient apiServerClient = new StandardApiServerClient();
        apiServerClient.configure(apiServerUrl, auth);

        ReplicationControllerPodPool podPool = new ReplicationControllerPodPool();
        podPool.configure(apiServerClient, namespace, name);

        List<Pod> pods = podPool.getPods();
        LOG.info("pods: {}", pods);

        podPool.setDesiredSize(2);
    }
}
