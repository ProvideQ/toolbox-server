import os
import subprocess
import sys

script_dir = os.path.dirname(os.path.realpath(__file__))
root_dir = os.path.dirname(script_dir)

# Check if a specific folder argument was provided
folder_filter = sys.argv[1] if len(sys.argv) > 1 else None

base_dirs = [
    os.path.join(root_dir, 'demonstrators'),
    os.path.join(root_dir, 'solvers'),
]

# uv resolves the python version for each venv from the solver's
# requires-python in pyproject.toml and downloads a matching interpreter
# if none is installed.
env = os.environ.copy()
env.setdefault('UV_PYTHON_DOWNLOADS', 'automatic')

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
                        venv_path = os.path.join('venv', venv_name)
                        env['UV_PROJECT_ENVIRONMENT'] = os.path.abspath(venv_path)
                        subprocess.run(['uv', 'sync', '--project', solver_dir],
                                       check=True, env=env)
                        pyvenv_cfg = os.path.join(venv_path, 'pyvenv.cfg')
                        with open(pyvenv_cfg) as f:
                            version = next((line.split('=', 1)[1].strip() for line in f
                                            if line.strip().startswith('version')), 'unknown')
                        print(f"Created '{venv_name}' with python {version}", flush=True)
                    except subprocess.CalledProcessError as e:
                        print(f"Error setting up virtual environment for {solver_dir}: {e}")
                        exitCode = 1
                    except FileNotFoundError:
                        print("Error: 'uv' not found. Install it via https://docs.astral.sh/uv/getting-started/installation/")
                        exit(1)

# let pipeline fail if there was an error in the venv setup.
exit(exitCode)
