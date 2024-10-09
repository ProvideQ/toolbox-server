import os
import subprocess

base_dir = os.path.join(os.path.dirname(os.getcwd()), 'solvers')

for root, dirs, files in os.walk(base_dir):
    if root == base_dir:  # iterate only one level deep
        for directory in dirs:
            req_file = os.path.join(base_dir, directory, 'requirements.txt')
            if os.path.exists(req_file):
                print(f"Found requirements.txt in {directory}, installing dependencies...")
                try:
                    subprocess.run(['pip', 'install', '-r', req_file], check=True)  # single thread to avoid dep. errors
                except subprocess.CalledProcessError as e:
                    print(f"Error installing requirements for {directory}: {e}")
