#!/bin/bash
# this script is made for the CI-pipeline, do not use this to install dependencies on your private machine!

# exit on error
set -e

# get base directory of the repository
REPO_DIR=$(dirname "$(dirname "$(readlink -f "$0")")")

# make sure to install to the gams conda env
source /opt/conda/bin/activate gams

# install solver dependencies / quantum frameworks and python wrappers
python3 "$REPO_DIR/scripts/ci-setup-solvers.py"

