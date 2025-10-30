#!/bin/bash
# *****************************************************************************
# Launch the Jeandle dev container(image from $DOCKIMG) and run the given command.
# If no command is provided, an interactive Bash shell is started.
# The container uses your host UID/GID, mounts the current directory, and
# self-deletes on exit. The script aborts if pwd is not inside HOME.
# *****************************************************************************
set -euo pipefail

SCRIPT=$(basename "$0")
USER=$(whoami)
ARCH=$(uname -m)

usage() {
  cat <<EOF
Usage: $SCRIPT [-H] [--] [COMMAND [ARG]...]

Options:
  -H    Use Docker's random hostname instead of the image name.

Environment:
  DOCKIMG   Image to run (default: jeandle-dev:latest).
  DOCKOPTS  Extra 'docker run' options (default: -i -t --rm).

Example:
  $SCRIPT make -j\$(nproc) check-all
EOF
}

# argument parsing 
use_docker_hostname=0
while getopts "H" opt; do
  case $opt in
    H) use_docker_hostname=1 ;;
    *) usage; exit 1 ;;
  esac
done
shift $((OPTIND - 1))

# prerequisites
DOCKER=$(command -v docker)
if [[ -z $DOCKER ]]; then
  echo "$SCRIPT: docker not found" >&2
  exit 1
fi

# defaults
DOCKIMG=${DOCKIMG:-jeandle-dev:latest}
DOCKOPTS=${DOCKOPTS:--i -t --rm}

# hostname
if (( use_docker_hostname )); then
  HOST_OPT=""
else
  host=${DOCKIMG##*/}   # strip registry
  host=${host%:*}        # strip tag
  HOST_OPT="-h $host"
fi

# the same UID/GID as the host
PASSWD=$(mktemp)
GROUP=$(mktemp)
trap 'rm -f "$PASSWD" "$GROUP"' EXIT INT TERM

echo "root:x:0:0::/root:/bin/bash"  > "$PASSWD"
echo "$USER:x:$(id -u):$(id -g)::$HOME:/bin/bash" >> "$PASSWD"
echo "root:x:0:"   > "$GROUP"
echo "users:x:$(id -g):" >> "$GROUP"

# launch
CONTAINER=$USER-$(date +%s)

$DOCKER run $DOCKOPTS \
  --name "$CONTAINER" \
  -u "$(id -u):$(id -g)" \
  -w "$PWD" \
  -v "$HOME:$HOME" \
  -v "$PASSWD:/etc/passwd:ro" \
  -v "$GROUP:/etc/group:ro" \
  $HOST_OPT \
  --security-opt seccomp=unconfined \
  --ulimit core=0 --ulimit stack=-1 \
  --cap-add=SYS_ADMIN --cap-add=SYS_NICE --cap-add=SYS_PTRACE \
  "$DOCKIMG" "$@"
