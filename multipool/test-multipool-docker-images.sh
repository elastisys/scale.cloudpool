#!/bin/bash

#
# Simple script to smoke check that each of the cloudpool docker images
# can be run as a multi cloudpool with environment variable MULTIPOOL=true.
# Runs each image and verifies some basic behavior of each multicloudpol.
#

scriptdir=$(dirname ${0})
rootdir=../${scriptdir}

# stop any docker container from prior executions
docker rm -f multicloudpool 2>&1 > /dev/null

# find all pom files that use the docker-maven-plugin
for pom in $(find .. -name 'pom.xml' | xargs  grep -l docker-maven-plugin); do
    if grep -q docker.image ${pom}; then
	# extract docker.image property
	image_name=$(grep docker.image ${pom} | sed -r 's#<docker.image>(.*)</docker.image>#\1#' | sed 's/ //g')
    else
	echo "error: cannot determine docker image name: ${pom} does not contain a <docker.image> tag"
	exit 1
    fi
    image=${image_name}:latest
    echo "trying out ${image} ..."
    docker run --name multicloudpool -d -p 8080:80 -e HTTP_PORT=80 -e MULTIPOOL=true ${image_name}
    echo "[${image}] waiting for server to start ..."
    sleep 4s
    # list cloudpool instances
    instances=$(curl --silent -X GET http://localhost:8080/cloudpools)
    num_instances=$(echo ${instances} | jq '. | length')
    if [[ ${num_instances} -ne 0 ]]; then
	echo "[${image}] error: unexpected number of cloudpool instances: expexcted: 0, was: ${instances}"
	exit 1
    fi
    echo "[${image}] initial pool instances: ${instances}"
    
    # create cloudpool instances
    echo "[${image}] creating cloudpool instances pool1 and pool2 ..."
    curl --silent -X POST http://localhost:8080/cloudpools/pool1
    curl --silent -X POST http://localhost:8080/cloudpools/pool2
    # list cloudpool instances
    instances=$(curl --silent -X GET http://localhost:8080/cloudpools)
    num_instances=$(echo ${instances} | jq '. | length')
    if  [[ ${num_instances} -ne 2 ]]; then
	echo "[${image}] error: unexpected number of cloudpool instances: expected 2, was: ${num_instances}"
	exit 1
    fi
    echo "[${image}] pool instances: ${instances}"

    # delete cloudpool instances
    echo "[${image}] deleting cloudpool instance pool1 ..."
    curl --silent -X DELETE http://localhost:8080/cloudpools/pool1
    instances=$(curl --silent -X GET http://localhost:8080/cloudpools)
    num_instances=$(echo ${instances} | jq '. | length')
    if  [[ ${num_instances} -ne 1 ]]; then
	echo "[${image}] error: unexpected number of cloudpool instances: expected 1, was: ${num_instances}"
	exit 1
    fi
    echo "[${image}] pool instances: ${instances}"


    
    docker rm -f multicloudpool 2>&1 > /dev/null
done
