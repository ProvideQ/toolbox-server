import os
import subprocess
import platform
import sys

script_dir = os.path.dirname(os.path.realpath(__file__))
root_dir = os.path.dirname(script_dir)

# Check if a specific folder argument was provided
folder_filter = sys.argv[1] if len(sys.argv) > 1 else None

base_dirs = [
    os.path.join(root_dir, 'demonstrators'),
    os.path.join(root_dir, 'solvers'),
]

def get_gamspy_license():
    # 1. Environment variable (GitHub Actions)
    license_key = os.environ.get("GAMSPY_LICENSE")
    if license_key:
        return license_key.strip()

    # 2. Local file (ignored by git)
    license_file = os.path.join(root_dir, ".gamspy_license")
    if os.path.exists(license_file):
        with open(license_file) as f:
            return f.read().strip()

    raise RuntimeError(
        "No GAMSPY license found. "
        "Set the GAMSPY_LICENSE environment variable or create a .gamspy_license file."
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

                req_file = os.path.join(solver_dir, 'requirements.txt')
                if os.path.exists(req_file):
                    venv_name = f"{os.path.basename(root)}_{framework_name}_{solver_name}"
                    print(f"Setting up virtual environment '{venv_name}' for {solver_dir}...")

                    try:
                        venv_path = os.path.join('venv', venv_name)
                        subprocess.run(['python', '-m', 'venv', venv_path], check=True)
                        if platform.system() == 'Windows':
                            pip_executable = os.path.join(venv_path, 'Scripts', 'pip.exe')
                            python_executable = os.path.join(venv_path, 'Scripts', 'python.exe')
                        else:
                            pip_executable = os.path.join(venv_path, 'bin', 'pip')
                            python_executable = os.path.join(venv_path, 'bin', 'python')

                        # install dependencies from requirements.txt
                        subprocess.run([pip_executable, 'install', '-r', req_file], check=True)

                        # install GAMSPy license if this is a GAMS environment
                        if "gams" in venv_name.lower():
                            license_key = get_gamspy_license()

                            print("Installing GAMSPy license...")
                            subprocess.run(
                                [
                                    python_executable,
                                    "-m",
                                    "gamspy",
                                    "install",
                                    "license",
                                    license_key,
                                ],
                                check=True,
                            )
                            print("GAMSPy license activated in '%s'" % venv_name)

                    except subprocess.CalledProcessError as e:
                        print(f"Error setting up virtual environment for {solver_dir}: {e}")
                        exitCode = 1
                        
# let pipeline fail if there was an error in the venv setup.
exit(exitCode)
