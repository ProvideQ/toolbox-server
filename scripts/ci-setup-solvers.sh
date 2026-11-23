#!/bin/bash
# this script is made for the CI-pipeline, do not use this to install dependencies on your private machine!

# exit on error
set -e

# get base directory of the repository
REPO_DIR=$(dirname "$(dirname "$(readlink -f "$0")")")

# make sure to install to the gams conda env
source /opt/conda/bin/activate gams

# uv is used to set up the solver venvs (see scripts/install-python-dependencies.py)
command -v uv >/dev/null 2>&1 || pip install uv

# install solver dependencies / quantum frameworks and python wrappers
python3 "$REPO_DIR/scripts/install-python-dependencies.py"

