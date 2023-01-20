# Solving MAXCUT Problems using GAMS

## Input
Graph in graph modeling langueagte (.gml) format. Weight of edges can be encoded via edge attribute "weight". If the attribute is ommitted, a default value of 1 is assumed. Other target attributes to consider for computing the max cut (e.g. length) can be considered via parameter `--TARGETATTRRIBUTE`.

## Output

For input file `myfile.gml` and outout file `myfile_sol.gml` will be created. The output file will contain the input graph plus additional attributes.
- graph attributes:
   - `cut_value`: value of the computed cut
   - `bound`: best known upper bound for the cut value
- node attribute:
   - `partition`: nodes are assigned to either partition 1 or 2


## How to run

- Download GAMS from https://www.gams.com/download/
- Install GAMS
- Run maxcut.gms with the .gml file you would like to solve as input
  - from GAMS Studio: Open maxcut.gms in GAMS studio, enter `--INPUT=<myfile.gml>` in the [parameter editor](https://www.gams.com/latest/docs/T_STUDIO.html#STUDIO_TOOLBAR) and hit the run button (or press F9)
  - from the command line
    ```
    gams maxcut.gms --INPUT=<myfile.gml>
    ```

## Parameters

In addition to the mandatory parameter `--INPUT=<myfile.gml>`, some optional parameters can also be set.
- `--OUTPUT`: Can be set to any string that will then define the name of the output gml file. If not set, the name of the input file `<myfile.gml>` will be combined with a `_sol` suffix such that the output is written to `myfile_sol.gml` **(default=`<input>_sol.gml)`**
- `--TARGETATTRRIBUTE`: Can be set to any string that defines an edge attribute in the input gml graph **(default=weight)**
- `--SOLVESDP`: Can be set to either 1 (an SDP relaxation  of the max cut problem according to Goemans/Williamson will be solved) or 0 (no SDP relaxation of the max cut problem will be solve). **(default=1)**
- `--SOLVEMIP`: Can be set to either 1 (an MIP formulation of the max cut problem will be solved) or 0 (no MIP formulation of the max cut problem will be solved). If the SDP Relaxation and the MIP solve are activated, the cut computed via the SDP relaxation serves as a starting point for the MIP. **(default=0)**
- `--SOLVEQUBO`: Can be set to either 1 (a QUBO formulation of the max cut problem will be solved) or 0 (no QUBO formulation of the max cut problem will be solved). If the SDP Relaxation and the QUBI solve are activated, the cut computed via the SDP relaxation serves as a starting point for the QUBO. **(default=1)**
- `--SDPSOLVER`: Can be set to either `MOSEK` or `CSDP`.  **(default=MOSEK)**
- `--TIMELIMIT`: Time limit in seconds. **(default=60)**

**NOTE:** By default, the MIP and the QUBO formulation will be solved with CPLEX. Other solvers can be specifief through GAMS command line parameters [MIP](https://www.gams.com/latest/docs/UG_GamsCall.html#GAMSAOmip) and [MIQCP](https://www.gams.com/latest/docs/UG_GamsCall.html#GAMSAOmiqcp)

