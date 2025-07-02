import os
import subprocess
import platform

script_dir = os.path.dirname(os.path.realpath(__file__))
root_dir = os.path.dirname(script_dir)
base_dirs = [
    os.path.join(root_dir, 'demonstrators'),
    os.path.join(root_dir, 'solvers'),
]

exitCode = 0
for base_dir in base_dirs:
    for root, dirs, files in os.walk(base_dir):
        # Iterate over framework directory (cirq, gams, qiskit, etc.)
        for framework_name in dirs:
            framework_dir = os.path.join(root, framework_name)

            # Iterate over problem directory (knapsack, tsp, etc.)
            for solver_name in os.listdir(framework_dir):
                solver_dir = os.path.join(framework_dir, solver_name)
                req_file = os.path.join(solver_dir, 'requirements.txt')
                submodules_file = os.path.join(solver_dir, 'submodules.txt')
                if os.path.exists(req_file) or os.path.exists(submodules_file):
                    venv_name = f"{os.path.basename(root)}_{framework_name}_{solver_name}"
                    print(f"Setting up virtual environment '{venv_name}' for {solver_dir}...")
                    try:
                        venv_path = os.path.join('venv', venv_name)
                        subprocess.run(['python3', '-m', 'venv', venv_path], check=True)
                        if platform.system() == 'Windows':
                            pip_executable = os.path.join(venv_path, 'Scripts', 'pip.exe')
                        else:
                            pip_executable = os.path.join(venv_path, 'bin', 'pip')
                        if os.path.exists(req_file):
                          subprocess.run([pip_executable, 'install', '-r', req_file], check=True)
                        if os.path.exists(submodules_file):
                            with open(submodules_file, 'r') as f:
                                submodules = f.read().strip().splitlines()
                            for submodule in submodules:
                                submodule_path = os.path.join(solver_dir, submodule)
                                if os.path.exists(submodule_path):
                                    subprocess.run([pip_executable, 'install', submodule_path], check=True)
                    except subprocess.CalledProcessError as e:
                        print(f"Error setting up virtual environment for {solver_dir}: {e}")
                        exitCode = 1
                        
# let pipeline fail if there was an error in the venv setup.
exit(exitCode)