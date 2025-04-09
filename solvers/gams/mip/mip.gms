$setEnv GAMSINP "%INPUT%"
$setEnv GAMSPEN "%PENALTY%"

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

# Create the replacement line using the matched groups
penalty = os.environ.get("GAMSPEN", "100")  # Default to 100 if not set
new_line = f'$batinclude %IDIR% {model_name} {model_type} {gams_sense} {obj_var} {penalty} -logOn=2'

# Replace the matching "Solve ..." line with the new line (only the first occurrence)
new_content = pattern.sub(new_line, content, count=1)

solution_block = r'''
$setNames "%INPUT%" fp fn fe
$if not set SOLOUTPUT $set SOLOUTPUT %fp%solution%fe%
file solFile / "%SOLOUTPUT%" /;
put solFile 'c Solution of %INPUT%';
put solFile / 's m ';

if((m.modelstat = %modelStat.Optimal% or m.modelstat = %modelStat.IntegerSolution%) or m.solvestat = %solveStat.NormalCompletion%,
   put solFile / 'Optimal solution found.';
   put solFile / 'Objective value: ' obj.l:0:0;
   put solFile / 'Variable values:';
   loop(j,
       put solFile / j.tl:0 ' = ' xi.l(j):0:0;
   );
else
   put solFile / 'Error: no solution was returned.';
);
'''

# Append the solution parsing code at the end of the .gms file.
new_content += "\n" + solution_block
# Write the modified content back to the .gms file
with open(new_name, 'w', encoding='utf-8', errors='ignore') as f:
    f.write(new_content)
$offEmbeddedCode
