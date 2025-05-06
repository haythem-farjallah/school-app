#!/bin/bash
set -e

# If Docker socket is mounted, add the gid to jenkins
if [ -S /var/run/docker.sock ]; then
  SOCK_GID=$(stat -c '%g' /var/run/docker.sock)
  getent group "$SOCK_GID" >/dev/null || groupadd -for -g "$SOCK_GID" dockersock
  usermod -aG "$SOCK_GID" jenkins
fi

# Now hand off to the stock Jenkins script (it will drop to the 'jenkins' user)
exec /usr/local/bin/jenkins.sh "$@"