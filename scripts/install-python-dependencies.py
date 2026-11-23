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
    # 1. Environment variable (GitHub Actions, Docker build)
    license_key = os.environ.get("GAMSPY_LICENSE")
    if license_key:
        return license_key.strip()

    # 2. Local file (ignored by git)
    license_file = os.path.join(repo_dir, ".gamspy_license")
    if os.path.exists(license_file):
        with open(license_file) as f:
            return f.read().strip()

    raise RuntimeError(
        "No GAMSPY license found. "
        "Set the GAMSPY_LICENSE environment variable or create a .gamspy_license file."
    )


def install_gamspy_license_and_scip(venv_path):
    """Installs the GAMSPY license and the SCIP solver into a GAMS venv."""
    if platform.system() == 'Windows':
        python_executable = os.path.join(venv_path, 'Scripts', 'python.exe')
    else:
        python_executable = os.path.join(venv_path, 'bin', 'python')

    license_key = get_gamspy_license()
    print("Installing GAMSPy license and scip solver ...", flush=True)
    subprocess.run(
        [python_executable, '-m', 'gamspy', 'install', 'license', license_key],
        check=True, capture_output=True,
    )
    subprocess.run(
        [python_executable, '-m', 'gamspy', 'install', 'solver', 'scip'],
        check=True, capture_output=True,
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
                        exitCode = 1
                    except FileNotFoundError:
                        print("Error: 'uv' not found. Install it via https://docs.astral.sh/uv/getting-started/installation/")
                        exit(1)

# let pipeline fail if there was an error in the venv setup.
exit(exitCode)
