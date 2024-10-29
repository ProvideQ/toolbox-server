import os
import subprocess

script_dir = os.path.dirname(os.path.realpath(__file__))
base_dir = os.path.join(script_dir, '..', 'solvers')

for root, dirs, files in os.walk(base_dir):
    # Check if 'requirements.txt' is in the current directory
    req_file = os.path.join(root, 'requirements.txt')
    if os.path.exists(req_file):
        print(f"Found requirements.txt in {root}, installing dependencies...")
        try:
            subprocess.run(['pip', 'install', '-r', req_file], check=True) # single thread to avoid dep. errors
        except subprocess.CalledProcessError as e:
            print(f"Error installing requirements for {root}: {e}")




