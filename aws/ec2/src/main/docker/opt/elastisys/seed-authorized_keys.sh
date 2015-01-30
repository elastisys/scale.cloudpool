#!/bin/bash

if [ "${SSH_KEY}" = "" ]; then
    echo "WARNING: No SSH_KEY environment variable set, you will be unable to login."
    exit 1
else
    echo "NOTE: SSH_KEY added to authorized_keys for elastisys."
    mkdir -p /home/elastisys/.ssh/
    echo ${SSH_KEY} > /home/elastisys/.ssh/authorized_keys
    exit 0
fi
