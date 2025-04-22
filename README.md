[![Build](https://img.shields.io/github/actions/workflow/status/ProvideQ/toolbox-server/deploy-main.yml?style=for-the-badge)](https://github.com/ProvideQ/toolbox-server/actions/workflows/deploy-main.yml)
[![Release](https://img.shields.io/github/v/release/ProvideQ/toolbox-server?style=for-the-badge)](https://github.com/ProvideQ/toolbox-server/releases/)

# ProvideQ Toolbox Server
This repository contains the source code for the [ProvideQ](https://provideq.org) Toolbox server.
A web-based user interface can be found in our
[ProvideQ/ProvideQ repository](https://github.com/ProvideQ/ProvideQ).

## Development setup
1. Install Java 17 or newer (check with `java -version`)
2. Clone this repository
3. [Optional, Solver Installation - install the Solvers that you want/need]
   1. Python-based Solvers (Qiskit, Cirq, Dwave, Qrisp)
      1. Install Python
      2. Install Python dependencies by running `python scripts/install-python-dependencies.py` <br>
         **Note:** Ensure that you execute the script from the root directory! Otherwise the created venv/ directory might be created at the wrong path.
   2. Compiled Solvers (e.g. used for VRP and TSP)
      1. Solvers implemented in compiled languages must be executed via binaries that are compiled for your operating system.
         For those types of solvers we usually include pre-compiled binaries for windows, mac (only arm), and unix.
      2. In case the pre-compiled versions do not work on your machine: re-compile them.
         Detailed compliation guides can be found in the *solvers/custom/[solver-name]* directory.
   3. GAMS (multiple solvers are build on this):
      1. Install a python env that works with GAMS (skip this step if you don't need GAMS)
      2. Install GAMS. (https://www.gams.com/download/)
      3. Install miniconda (or anaconda, if you prefer that):
         https://conda.io/projects/conda/en/stable/user-guide/install/index.html
      4. Create a GAMS conda environment: `conda create --name gams python=3.10 --yes`
      5. Activate your conda environment: `conda activate gams`.
      6. Make GAMS use that python environment by setting the `GMSPYTHONLIB=<path-to-conda>/envs/gams/lib/libpython3.10.so`
            environment variable.
      7. Install GAMS packages to the GAMS conda env:
            `pip install gams[core,connect] --find-links <path-to-gams>/api/python/bdist`
            * If you get an error building `psycopg2`, try to install these postgres packages:
              `sudo apt-get install postgresql libpq-dev` and run the `pip install ...` command again
      8. Install the python dependencies we use in our python packages: `pip install -r ./solvers/gams/python/requirements.txt`
4. Run the server using `./gradlew bootRun`

## Deployment
This repository is designed to be deployed with [Dokku](https://dokku.com/), but you can also run 
the Java application directly or inside a docker container (`Dockerfile` is included!).
The docker container can be built and run as follows:
```shell
# we assume that you have a gamslice.txt file in this directory containing a valid GAMS license (typically 6 lines)
docker build --tag provideq-toolbox-backend --build-arg GAMS_LICENSE=$(base64 --wrap=0 ./gamslice.txt) .
docker run --publish 8080:5000 provideq-toolbox-backend
```

## Releasing a new version
1. Create a release branch from develop: `git checkout -b release/x.y.z`.
2. Bump the version number in the `build.gradle` file to the new version number and commit it to the release branch.
3. Push to GitHub and create a pull request to merge the release branch into `main`.
4. Make sure to test your new version!
5. Write a changelog.
   The PR can help you identify differences between the last release (`main`) and the next one (your release branch).
6. Merge the PR into main.
7. [Create a new GitHub release](https://github.com/ProvideQ/toolbox-server/releases/new) with a new tag named like your
   version number `x.y.z` and use the changelog as the description.
8. Pull the main branch (`git checkout main && git pull`),
   merge it into the develop branch (`git checkout develop && git pull && git merge main`)
   and push it (`git push`).

## CI / CD
We're using GitHub Actions to automate the execution of our validation tools and the deployment of our software.
To use this, enable GitHub Actions and configure the following secrets in the GitHub repository settings:

* `GAMS_LICENSE`:
  1. Get a license for GAMS and put it in a `gamslice.txt` file.
     A free [demo or community license](https://www.gams.com/try_gams/) should be sufficient.
  2. Convert the license text to base64 using this command:
     ```shell
     base64 --wrap=0 ./gamslice.txt
     ```
  3. Copy the printed base64 string to the `GAMS_LICENSE` secret in your GitHub repository.
* `DOKKU_SERVER_ADDRESS`:
  1. Install [Dokku](https://dokku.com/) on your deployment server.
     Make sure it can be reached from GitHub Actions!
  2. Set the `DOKKU_SERVER_ADDRESS` secret to the address of your server.
     
     *Tip: If you use an IP address, the IP address will be obfuscated in the Actions logs.
     Counterintuitively, using a domain name as an address might leak your IP address due to SSH address resolution
     logs.*
* `DOKKU_DEPLOYMENT_KEY`:
  1. Generate an ssh key and register it as a dokku user.
  2. Make sure this user has push access the `toolbox-frontend`, `toolbox-backend`, `toolbox-frontend-staging` and
     `toolbox-backend-staging` apps.
  3. Set `DOKKU_DEPLOYMENT_KEY` to the private key generated in step i.

## License
Copyright (c) 2022 - 2023 ProvideQ

This project is available under the [MIT License](./LICENSE).
