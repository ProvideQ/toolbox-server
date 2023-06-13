#!/bin/bash

# exit on error
set -e

# get base directory of the repository
REPO_DIR=$(dirname "$(dirname "$(readlink -f "$0")")")

# make sure to install to the gams conda env
source /opt/conda/bin/activate gams

# install solver dependencies
pip install -r "$REPO_DIR/gams/requirements.txt"
pip install -r "$REPO_DIR/qiskit/requirements.txt"
