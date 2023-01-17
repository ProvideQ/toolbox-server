$title Goemans/Williamson Randomized Approximation Algorithm for MaxCut (MAXCUT,SEQ=338)

$onText
Let G(N, E) denote a graph. A cut is a partition of the vertices N
into two sets S and T. Any edge (u,v) in E with u in S and v in T is
said to be crossing the cut and is a cut edge. The size of the cut is
defined to be sum of weights of the edges crossing the cut.

This model presents a simple MIP formulation of the problem that is
seeded with a solution from the Goemans/Williamson randomized
approximation algorithm based on a semidefinite programming
relaxation. By default CSDP is used to solve the SDP.
Use --SDPSOLVER=MOSEK to switch to Mosek.

The MaxCut instance tg20_7777 is available from the Biq Mac Library
and comes from applications in statistical physics.


Wiegele A., Biq Mac Library - Binary Quadratic and Max Cut Library.
http://biqmac.uni-klu.ac.at/biqmaclib.html

Goemans M.X., and Williamson, D.P., Improved Approximation Algorithms
for Maximum Cut and Satisfiability Problems Using Semidefinite
Programming. Journal of the ACM 42 (1995), 1115-1145.
http://www-math.mit.edu/~goemans/PAPERS/maxcut-jacm.pdf

Keywords: mixed integer linear programming, approximation algorithms,
          convex optimization, randomized algorithms, maximum cut problem,
          mathematics
$offText

$if not set INPUT $abort Please provide an input file via --INPUT=<myFile>
$if not set TARGETATTRIBUTE $set TARGETATTRIBUTE weight
$if not set SOLVESDP        $set SOLVESDP  1
$if not set SOLVEMIP        $set SOLVEMIP  0
$if not set SOLVEQUBO       $set SOLVEQUBO 1
$if not set SDPSOLVER       $set SDPSOLVER MOSEK
$if not set TIMELIMIT       $set TIMELIMIT 60

Set n 'nodes';

Alias (n,i,j);

Parameter w(i,j) 'edge weights';

Set e(i,j) 'edges';

Set S(n), T(n), bestS(n);

Scalar
    wS      weight of cut S / -inf /
    maxwS   best weight / -inf /
    mingapS min gap / inf /;
    
Scalar SDPRelaxation / inf /;

parameter rep(*) / 'value of cut'              -inf
                   'value of best known bound' +inf
                   'relative gap'              NA   /;


$ onembeddedCode Python:
import networkx as nx
graph = nx.read_gml("%INPUT%", label=None)
gams.set('n', [str(n) for n in graph.nodes])
gams.set('w', [(str(i),str(j),float(v)) for i,j,v in graph.edges.data("%TARGETATTRIBUTE%", default=1)])
$ offembeddedCode n w

* We want all edges to be i-j with i<j;
e(i,j)    = ord(i) < ord(j);
w(e(i,j)) = w(i,j) + w(j,i);
w(i,j)$(not e(i,j)) = 0;

option e < w;
*option e:0:0:1, w:8:0:1; display n,e,w;

* Simple MIP model
Variable
   x(n)     'decides on what side of the cut'
   cut(i,j) 'edge is in the cut'
   z        'objective';

Binary Variable x;

Equation obj, xor1(i,j), xor2(i,j), xor3(i,j), xor4(i,j);

obj..          z      =e= sum(e, w(e)*cut(e));

xor1(e(i,j)).. cut(e) =l= x(i) + x(j);

xor2(e(i,j)).. cut(e) =l= 2 - x(i) - x(j);

xor3(e(i,j)).. cut(e) =g= x(i) - x(j);

xor4(e(i,j)).. cut(e) =g= x(j) - x(i);

Model maxcut / all /;


* QUBO Model
equation defQUBO;
defQUBO.. z =e= sum(e(i,j), w(e)*(sqr(x(i)) + sqr(x(j)) -2*x(i)*x(j)));
model qubo / defQUBO /;


$ifthene.solvesdp %SOLVESDP%==1
$onText
Set up the SDP
   max W*Y s.t. Y_ii = 1, Y positive semidefinite (psd)
We need to pass on the dual to csdp
   min x1 + x2 + ... + xn s.t. X = F1*x1 + F2*x2 + ... + Fn*xn - W, X psd
with F_i = 1 for F_ii and 0 otherwise
$offText


Parameter L(i,j) 'Cholesky factor of Y';

$ifthen.sdpsolver %SDPSOLVER% == CSDP
Parameter
   c(n)     'cost coefficients'
   F(n,i,j) 'constraint matrix'
   F0(i,j)  'constant term'
   Y(i,j)   'dual solution';

c(n)     =  1;
F(n,n,n) =  1;
F0(i,j)  = -w(i,j);

$set ISYM n
$set ASYM Y
$set LSYM L

execute_unload 'csdpin.gdx' n = m, n, c, F, F0;
execute.checkErrorLevel 'gams runcsdp.inc lo=%gams.lo% --strict=1'
execute_load 'csdpout.gdx'  Y;
$libInclude linalg cholesky n Y L

SDPRelaxation = 0.5*sum(e, w(e)*(1 - Y(e)));

$else.sdpsolver

Variable Y(i,j)    'PSDMATRIX';
Variable sdpobj    'objective function variable';
Equation sdpobjdef 'objective function W*Y';
sdpobjdef.. sum(e(i,j),w(i,j)*(Y(i,j)+Y(j,i))/2.0) + sum((i,j),eps*Y(i,j)) =E= sdpobj;
Y.fx(i,i) = 1.0;
Model sdp / sdpobjdef /;

option lp = %SDPSOLVER%;
sdp.limrow   = 0;
sdp.limcol   = 0;
sdp.solprint = 0;
sdp.reslim   = %TIMELIMIT%-timeelapsed;
Solve sdp min sdpobj using lp;

Parameter Yl(i,j)   'level values of Y as parameter';
Yl(i,j) = Y.l(i,j);
$libInclude linalg cholesky n Yl L

SDPRelaxation = 0.5*sum(e, w(e)*(1 - Y.l(e)));

$endif.sdpsolver

display SDPRelaxation;

* Now do the random hyperplane r
Parameter r(n);


set hp      hyperplanes / hp1*hp10 /;
parameter rep_hp(hp,*)
loop(hp,
   r(n) = uniform(-1,1);
   S(n) = sum(i, L(n,i)*r(i)) < 0;
   T(n) = yes;
   T(S) =  no;
   wS   = sum(e(S,T), w(S,T)) + sum(e(T,S), + w(T,S));
   rep_hp(hp,'value of cut')              = ws;
   rep_hp(hp,'value of best known bound') = SDPRelaxation;
   rep_hp(hp,'relative gap')              = (SDPRelaxation-ws)/SDPRelaxation;
   if(wS > maxwS, maxwS = wS; mingapS = (SDPRelaxation-ws)/SDPRelaxation; bestS(n) = S(n););
);
option clear=S;
S(bestS) = yes;
T(n)     = yes;
T(S)     =  no;

display maxwS, mingapS, rep_hp;

rep('value of cut')              = maxwS;
rep('value of best known bound') = SDPRelaxation;
rep('relative gap')              = (rep('value of best known bound')-rep('value of cut'))/rep('value of best known bound');

put_utility 'log' / '### Value of cut:              ' rep('value of cut'):0:4;
put_utility 'log' / '### Value of best known bound: ' rep('value of best known bound'):0:4;
put_utility 'log' / '### Relative gap:              ' rep('relative gap'):0:16;


* use computed feasible solution as starting point for MIP solve
x.l(bestS)    = 1;
cut.l(e(i,j)) = x.l(i) xor x.l(j);
$endif.solvesdp

$ifthene.solvemip %SOLVEMIP%==1
* SCIP and COPT do this by default, for other solvers we need to enable it
$set MIPSTART
$ifthen.defaultMIPSolver x%gams.mip% == x
file cpxopt / cplex.opt /;
putclose cpxopt 'mipstart 1' /
                'polishaftertime 1'/
$ife %SOLVESDP%==1 'upperobjstop ' SDPRelaxation:0:16
putclose cpxopt;
maxcut.optFile = 1;
$else.defaultMIPSolver     
$  if %gams.mip%  == cplex  $set MIPSTART mipStart
$  if %gams.mip%  == cbc    $set MIPSTART mipStart
$  if %gams.mip%  == gurobi $set MIPSTART mipStart
$  if %gams.mip%  == xpress $set MIPSTART loadmipsol
$  ifThen not x%MIPSTART% == x
$    echo %MIPSTART% 1 > %gams.mip%.opt
     maxcut.optFile = 1;
$  endIf
$endif.defaultMIPSolver
maxcut.limrow   = 0;
maxcut.limcol   = 0;
maxcut.solprint = 0;
maxcut.reslim   = %TIMELIMIT%-timeelapsed;
solve maxcut max z using mip;

option clear=S;
S(n) = x.l(n) > 0.5;
T(n)     = yes;
T(S)     =  no;

parameter rep(*);
rep('value of cut')              = max(rep('value of cut'),z.l);
rep('value of best known bound') = min(rep('value of best known bound'),maxcut.objest);
rep('relative gap')              = (rep('value of best known bound')-rep('value of cut'))/rep('value of best known bound');
$endif.solvemip


$ifthene.solvequbo %SOLVEQUBO%==1
* SCIP and COPT do this by default, for other solvers we need to enable it
$set MIPSTART
$ifthen.defaultMIQCPSolver x%gams.miqcp% == x
option miqcp=cplex;
file cpxopt2 / cplex.op2 /;
putclose cpxopt2 'mipstart 1' /
$ife %SOLVESDP%==1 'upperobjstop ' SDPRelaxation:0:16
putclose cpxopt2;
qubo.optFile = 2;
$else.defaultMIQCPSolver     
$  if %gams.miqcp%  == cplex  $set MIPSTART mipStart
$  if %gams.miqcp%  == cbc    $set MIPSTART mipStart
$  if %gams.miqcp%  == gurobi $set MIPSTART mipStart
$  if %gams.miqcp%  == xpress $set MIPSTART loadmipsol
$  ifThen not x%MIPSTART% == x
$    echo %MIPSTART% 1 > %gams.miqcp%.op2
     qubo.optFile = 2;
$  endIf
$endif.defaultMIQCPSolver
qubo.limrow   = 0;
qubo.limcol   = 0;
qubo.solprint = 0;
qubo.reslim   = %TIMELIMIT%-timeelapsed;
solve qubo max z using miqcp;

option clear=S;
S(n) = x.l(n) > 0.5;
T(n)     = yes;
T(S)     =  no;

parameter rep(*);
rep('value of cut')              = max(rep('value of cut'),z.l);
rep('value of best known bound') = min(rep('value of best known bound'),qubo.objest);
rep('relative gap')              = (rep('value of best known bound')-rep('value of cut'))/rep('value of best known bound');
$endif.solvequbo

put_utility 'log' / '### Value of cut:              ' rep('value of cut'):0:4;
put_utility 'log' / '### Value of best known bound: ' rep('value of best known bound'):0:4;
put_utility 'log' / '### Relative gap:              ' rep('relative gap'):0:16;

*write solution
$setNames "%INPUT%" fp fn fe
$if not set OUTPUT $set OUTPUT %fp%%fn%_sol%fe%
embeddedCode Python:
import networkx as nx
# read input graph
g = nx.read_gml("%INPUT%", label=None)
# get cut value and bound ant turn them into graph attributes
attrs_g = {"cut_value": list(gams.get('rep'))[0][1], "bound": list(gams.get('rep'))[1][1]}
g.graph.update(attrs_g)
# get node partition
s = list(gams.get("S"))
t = list(gams.get("T"))
# turn node partitions into dicts
attr1 = {}
for n in s:
    try:
        attr1[int(n)] = {"Partition": 1}
    except ValueError:
        attr1[n] = {"Partition": 1}
attr2 = {}
for n in t:
    try:
        attr2[int(n)] = {"Partition": 2}
    except ValueError:
        attr2[n] = {"Partition": 2}
# make node partition a node attribute
nx.set_node_attributes(g, attr1)
nx.set_node_attributes(g, attr2)
lines = list(nx.generate_gml(g, stringizer=None))
with open("%OUTPUT%", 'w') as fout:
    if "Comment" in lines[1] or "id" in lines[2]:
        for line in lines[:5]:
            fout.write(line + '\n')
    else:
        for line in lines[:3]:
            fout.write(line + '\n')
            
    for node in g.nodes(data=True):
        if type(node[0]) == int:
            strNode = "  node [\n    id " + f'{node[0]}' + "\n"
        else:
            strNode = "  node [\n    id " + "\"{}\"".format(node[0]) + "\n"
        for key, val in node[-1].items():
            if type(val) == int or type(val) == float:
                strNode += "    "+ str(key) + " " + f'{val}' + "\n"
            else:
                strNode += "    "+ str(key) + " " + "\"{}\"".format(str(val)) + "\n"
        strNode += "  ]"
        fout.write(strNode + '\n')
    for edge in g.edges(data=True):
        if type(edge[0]) == int:
            strEdge = "  edge [\n    source " + f'{edge[0]}' + "\n    target " + f'{edge[1]}' + "\n"
        else:
            strEdge = "  edge [\n    source " + "\"{}\"".format(edge[0]) + "\n    target " + "\"{}\"".format(edge[1]) + "\n"
        for key, val in edge[-1].items():
            if type(val) == int or type(val) == float:
                strEdge += "    " + str(key) + " " + f'{val}' + "\n"
            else: 
                strEdge += "    " + str(key) + " " + "\"{}\"".format(str(val)) + "\n"
        strEdge += "  ]"
        fout.write(strEdge + '\n')
    fout.write("]")
endembeddedCode
 
