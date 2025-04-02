*-----------------------------------------------------------
* Cross-Platform MPS -> GAMS -> QUBO in One Script
* Usage:
*   gams convert_qb.gms --INPUT=myModel.mps
* or
*   gams convert_qb.gms --INPUT=myModel.lp  (if mps2gms supports LP)
*-----------------------------------------------------------

$if not set INPUT $set INPUT myModel.mps

* Turn the GAMS macro %INPUT% into a real OS environment variable GAMSINP:
$setEnv GAMSINP "%INPUT%"


$onEmbeddedCode Python:
import os
import sys
import subprocess
import re

# 1) Get the input file from GAMS macro or default
inp_file = os.environ.get('GAMSINP', 'myModel.mps')

# 2) Remove old "gams.gms" if it exists
if os.path.exists('gams.gms'):
    os.remove('gams.gms')

# 3) Invoke mps2gms on the input file => "gams.gms"
if not os.path.exists(inp_file):
    sys.exit(f"*** ERROR: input file not found: {inp_file}")

cmd = ['mps2gms', inp_file, 'gams.gms']
print(f"--- Running: {' '.join(cmd)}")
res = subprocess.run(
    cmd,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE
)
out = res.stdout.decode('utf-8', errors='ignore')
err = res.stderr.decode('utf-8', errors='ignore')


# 4) Rename gams.gms => convertedModel.gms
if os.path.exists('convertedModel.gms'):
    os.remove('convertedModel.gms')

if not os.path.exists('gams.gms'):
    sys.exit('*** ERROR: "gams.gms" not found - did mps2gms succeed?')

os.rename('gams.gms', 'convertedModel.gms')

# 5) Parse "convertedModel.gms" for Solve statement
new_name = 'convertedModel.gms'
with open(new_name, 'r') as f:
    content = f.read()

pattern = re.compile(
    r'Solve\s+(\S+)\s+using\s+(\S+)\s+(maximizing|minimizing)\s+(\S+)\s*;',
    re.IGNORECASE
)
match = pattern.search(content)
if not match:
    sys.exit('*** ERROR: No "Solve ... using ..." statement found in convertedModel.gms')

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

# 7) Emit GAMS macros so the main code can pick them up
print(f'$set PARSED_MODELNAME {model_name}')
print(f'$set PARSED_MODELTYPE {model_type}')
print(f'$set PARSED_SENSE    {gams_sense}')
print(f'$set PARSED_OBJVAR   {obj_var}')

# Also define a hardcoded penalty (example: 10)
print('$set HARDCODED_PENALTY 10')
$offEmbeddedCode

*-----------------------------------------------------------
* 8) Finally, call qubo_solve.gms with the macros we just set
*-----------------------------------------------------------
$batinclude qubo_solve.gms %PARSED_MODELNAME% %PARSED_MODELTYPE% \
                           %PARSED_SENSE% %PARSED_OBJVAR% %HARDCODED_PENALTY% -logOn=2
