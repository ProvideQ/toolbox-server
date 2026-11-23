import os
import platform
import subprocess
import sys

script_dir = os.path.dirname(os.path.realpath(__file__))
repo_dir = os.path.dirname(script_dir)

# The app module lives in toolbox/ inside the repository, but directly in the
# working directory inside the Docker container (no toolbox/ subdir there).
toolbox_dir = os.path.join(repo_dir, 'toolbox')
app_dir = toolbox_dir if os.path.isdir(toolbox_dir) else repo_dir

# Check if a specific folder argument was provided
folder_filter = sys.argv[1] if len(sys.argv) > 1 else None

base_dirs = [
    os.path.join(app_dir, 'demonstrators'),
    os.path.join(app_dir, 'solvers'),
]

# uv resolves the python version for each venv from the solver's
# requires-python in pyproject.toml and downloads a matching interpreter
# if none is installed.
env = os.environ.copy()
env.setdefault('UV_PYTHON_DOWNLOADS', 'automatic')


def get_gamspy_license():
    """Returns the GAMSPY license as an access code or an absolute license file path."""
    # 1. Environment variable (GitHub Actions, Docker build)
    license_key = os.environ.get("GAMSPY_LICENSE")
    if license_key:
        return license_key.strip()

    # 2. Local file (ignored by git), contains either an access code or an absolute path to a GAMS license file
    license_file = os.path.join(repo_dir, ".gamspy_license")
    if os.path.exists(license_file):
        with open(license_file) as f:
            value = f.read().strip()
        if value:
            return value

    raise RuntimeError(
        "No GAMSPY license found. "
        "Set the GAMSPY_LICENSE environment variable or create a .gamspy_license file "
        "containing either the license access code or an absolute path to a license file."
    )


def install_gamspy_license_and_scip(venv_path):
    """Installs the GAMSPY license and the SCIP solver into a GAMS venv."""
    if platform.system() == 'Windows':
        python_executable = os.path.join(venv_path, 'Scripts', 'python.exe')
    else:
        python_executable = os.path.join(venv_path, 'bin', 'python')

    # GAMSPY_GAMS_SYSDIR points the solvers at the licensed GAMS installation at
    # runtime, but it must not be set while installing: the license and solver
    # files belong to the venv's own gamspy-base installation.
    install_env = os.environ.copy()
    install_env.pop('GAMSPY_GAMS_SYSDIR', None)
    # uv-managed venvs contain no pip, tell gamspy to install via uv instead and
    # point it at the venv, since it is not the working directory's default venv.
    install_env['VIRTUAL_ENV'] = os.path.abspath(venv_path)

    license_key = None
    try:
        license_key = get_gamspy_license()
    except RuntimeError as e:
        # A GAMSPy license is separate from a GAMS license, without it the venv's
        # bundled GAMS runs in demo mode. The licensed system GAMS (GAMSPY_GAMS_SYSDIR)
        # is used at runtime instead, so missing licenses are not fatal here.
        print(f"Warning: {e} Skipping GAMSPy license installation.", flush=True)

    if license_key is not None:
        print("Installing GAMSPy license ...", flush=True)
        subprocess.run(
            [python_executable, '-m', 'gamspy', 'install', 'license', license_key],
            check=True, capture_output=True, env=install_env,
        )

    print("Installing scip solver ...", flush=True)
    subprocess.run(
        [python_executable, '-m', 'gamspy', 'install', 'solver', 'scip', '--use-uv'],
        check=True, capture_output=True, env=install_env,
    )

exitCode = 0
for base_dir in base_dirs:
    for root, dirs, files in os.walk(base_dir):
        # Iterate over framework directory (cirq, gams, qiskit, etc.)
        for framework_name in dirs:
            framework_dir = os.path.join(root, framework_name)

            # Iterate over problem directory (knapsack, tsp, etc.)
            for solver_name in os.listdir(framework_dir):
                solver_dir = os.path.join(framework_dir, solver_name)
                # If folder filter is specified, only process matching folders
                if folder_filter and folder_filter not in solver_dir:
                    continue

                pyproject_file = os.path.join(solver_dir, 'pyproject.toml')
                if os.path.exists(pyproject_file):
                    venv_name = f"{os.path.basename(root)}_{framework_name}_{solver_name}"
                    print(f"Setting up virtual environment '{venv_name}' for {solver_dir}...", flush=True)
                    try:
                        venv_path = os.path.join(app_dir, 'venv', venv_name)
                        env['UV_PROJECT_ENVIRONMENT'] = os.path.abspath(venv_path)
                        subprocess.run(['uv', 'sync', '--project', solver_dir],
                                       check=True, env=env)
                        pyvenv_cfg = os.path.join(venv_path, 'pyvenv.cfg')
                        with open(pyvenv_cfg) as f:
                            version = next((line.split('=', 1)[1].strip() for line in f
                                            if line.strip().startswith('version')), 'unknown')
                        print(f"Created '{venv_name}' with python {version}", flush=True)
                        if 'gams' in venv_name.lower():
                            install_gamspy_license_and_scip(venv_path)
                    except subprocess.CalledProcessError as e:
                        print(f"Error setting up virtual environment for {solver_dir}: {e}")
                        if e.stdout:
                            print("STDOUT:")
                            print(e.stdout.decode() if isinstance(e.stdout, bytes) else e.stdout)
                        if e.stderr:
                            print("STDERR:")
                            print(e.stderr.decode() if isinstance(e.stderr, bytes) else e.stderr)
                        exitCode = 1
                    except FileNotFoundError:
                        print("Error: 'uv' not found. Install it via https://docs.astral.sh/uv/getting-started/installation/")
                        exit(1)

# let pipeline fail if there was an error in the venv setup.
exit(exitCode)
