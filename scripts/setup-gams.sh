#!/bin/bash

# Expected env variables:
# - GAMS_LICENSE: base64-encoded GAMS license
# Exported env variables:
# - GMSPYTHONLIB
# - PATH (for conda and GAMS)

# exit on error
set -e

# === Install GAMS ===
# GAMS Installation script is based on the official installation guide
# (https://www.gams.com/latest/docs/UG_UNIX_INSTALL.html) and adapts some lines from
# iisaa/gams-docker (https://github.com/iiasa/gams-docker/blob/master/Dockerfile, GPL-3.0 licensed)

# Configure the GAMS version here
GAMS_VERSION_RELEASE_MAJOR=42.1
GAMS_VERSION_HOTFIX=0

# download the self-extracting archive to /opt/gams/gams.exe and run/extract it
curl --show-error --output /opt/gams/gams.exe --create-dirs "https://d37drm4t2jghv5.cloudfront.net/distributions/${GAMS_VERSION_RELEASE_MAJOR}.${GAMS_VERSION_HOTFIX}/linux/linux_x64_64_sfx.exe"
cd /opt/gams
chmod +x gams.exe
sync
./gams.exe -q # -q = quietly, see https://linux.die.net/man/1/unzipsfx
rm -rf gams.exe

# Install GAMS license
GAMS_PATH=/opt/gams/gams${GAMS_VERSION_RELEASE_MAJOR}_linux_x64_64_sfx
echo "${GAMS_LICENSE}" | base64 --decode > "$GAMS_PATH/gamslice.txt"

# Add GAMS to PATH
ln -s "$GAMS_PATH/gams" /usr/local/bin/gams
echo "export PATH=\$PATH:$GAMS_PATH" >> ~/.bashrc

# Run GAMS installer
cd "$GAMS_PATH" &&\
./gamsinst -a



# === Install conda ===
# download installer, verify it and make it executable
curl --show-error --output /opt/conda-installer/install.sh --create-dirs "https://repo.anaconda.com/miniconda/Miniconda3-py310_23.1.0-1-Linux-x86_64.sh"
cd /opt/conda-installer
echo "32d73e1bc33fda089d7cd9ef4c1be542616bd8e437d1f77afeeaf7afdb019787 install.sh" | sha256sum --check
chmod +x ./install.sh

# Install python (with python -> python3 alias) and pip from miniconda
# ("-b" = non-interactive batch mode, "-p /opt/conda" = installation directory)
./install.sh -b -p /opt/conda
PATH=$PATH:/opt/conda/bin # for now
echo "export PATH=\$PATH:/opt/conda/bin" >> ~/.bashrc # for subsequent build steps
conda init

# Remove installer
cd /app
rm --recursive /opt/conda-installer



# === Set up conda env for gams ===
# make sure to use the same python version as for the conda installer
conda create --name gams python=3.10 --yes
# since we cannot restart the shell within the script, we'll have to use this instead of "conda activate"
# (https://askubuntu.com/a/1464306)
source /opt/conda/bin/activate gams

# install GAMS links for python
pip install gams[core,connect] --find-links /opt/gams/gams${GAMS_VERSION_RELEASE_MAJOR}_linux_x64_64_sfx/api/python/bdist

# make GAMS use our python version
echo "export GMSPYTHONLIB=/opt/conda/envs/gams/lib/libpython3.10.so" >> ~/.bashrc
