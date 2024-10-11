#!/bin/bash
# this script is made for the CI-pipeline, do not use this to install dependencies on your private machine!

# exit on error
set -e

# get base directory of the repository
REPO_DIR=$(dirname "$(dirname "$(readlink -f "$0")")")

# make sure to install to the gams conda env
source /opt/conda/bin/activate gams

# install solver dependencies
pip install -r "$REPO_DIR/solvers/gams/requirements.txt"
# quantum frameworks:
pip install -r "$REPO_DIR/solvers/qiskit/requirements.txt"
pip install -r "$REPO_DIR/solvers/cirq/requirements.txt"
pip install -r "$REPO_DIR/solvers/dwave/requirements.txt"
pip install -r "$REPO_DIR/solvers/qrisp/requirements.txt"
# custom solvers with python wrapper:
pip install -r "$REPO_DIR/solvers/custom/hs-knapsack/requirements.txt"
pip install -r "$REPO_DIR/solvers/custom/lkh/requirements.txt"
pip install -r "$REPO_DIR/demonstrators/cplex/requirements.txt"
