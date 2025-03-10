import os
import subprocess
import argparse

parser = argparse.ArgumentParser(
    prog="OR-Tools CBC MIP Solver",
    description="A CLI Program to solve MIP problems in lp or mips format with Or-Tools CBC"
)

parser.add_argument("input_file")
parser.add_argument("--output-file")
args = parser.parse_args()


input_file_abs = os.path.abspath(args.input_file)
output_file_abs = os.path.abspath(args.output_file)
# Directory where we'll run GAMS so it creates .gms in that same folder
work_dir = os.path.dirname(input_file_abs)
# GAMS by default names the new .gms file after the base of the input file
base_name = os.path.splitext(os.path.basename(input_file_abs))[0]
intermediate_gms = os.path.join(work_dir, base_name + ".gms")
# 1) Run GAMS to convert the LP/MPS -> GMS
#    "task=ConvertIn" instructs GAMS to read the file and produce a .gms.
subprocess.run(["gams", input_file_abs, "task=ConvertIn"], check=True, cwd=work_dir)
# 2) Move or rename the newly created .gms to the user-specified output_file
#    If you prefer to copy instead, you can use shutil.copyfile().
if not os.path.exists(intermediate_gms):
    raise FileNotFoundError(f"Expected GAMS output file not found: {intermediate_gms}")
os.replace(intermediate_gms, output_file_abs)



