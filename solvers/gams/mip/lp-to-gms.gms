$setEnv GAMSINP "%INPUT%"

$onEmbeddedCode Python:
import os
import sys
import subprocess
import re

inp_file = os.environ.get('GAMSINP')
print(f"{inp_file}")

if not os.path.exists(inp_file):
    sys.exit(f"*** ERROR: input file not found: {inp_file}")

# Replace .lp with .gms
base, ext = os.path.splitext(inp_file)
new_name = base + '.gms'

cmd = ['mps2gms', inp_file]
print(f"--- Running: {' '.join(cmd)}")
res = subprocess.run(
    cmd,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE
)
#out = res.stdout.decode('utf-8', errors='ignore')
#err = res.stderr.decode('utf-8', errors='ignore')

with open(new_name, 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

pattern = re.compile(
    r'Solve\s+(\S+)\s+using\s+(\S+)\s+(maximizing|minimizing)\s+(\S+)\s*;',
    re.IGNORECASE
)
match = pattern.search(content)
if not match:
    sys.exit('*** ERROR: No "Solve ... using ..." statement found in generated .gms file')

model_name  = match.group(1)  # e.g. "m"
model_type  = match.group(2)  # e.g. "LP" or "MIP"
sense_text  = match.group(3)  # "maximizing" or "minimizing"
obj_var     = match.group(4)  # e.g. "x7"

if sense_text.lower().startswith('max'):
    gams_sense = 'max'
else:
    gams_sense = 'min'

# 6) Show info to GAMS listing:
print(f'--- Detected from "{new_name}":')
print(f'    Model name    = {model_name}')
print(f'    Model type    = {model_type}')
print(f'    Sense         = {gams_sense}')
print(f'    Objective var = {obj_var}')

with open("parsed_macros.gms", "w") as macrofile:
    macrofile.write(f"$set PARSED_MODELNAME {model_name}\n")
    macrofile.write(f"$set PARSED_MODELTYPE {model_type}\n")
    macrofile.write(f"$set PARSED_SENSE {gams_sense}\n")
    macrofile.write(f"$set PARSED_OBJVAR {obj_var}\n")
    macrofile.write(f"$set HARDCODED_PENALTY 10\n")

$offEmbeddedCode
