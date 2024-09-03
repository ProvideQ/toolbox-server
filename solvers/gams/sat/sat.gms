*input can be set through --INPUT=<myfile.cnf>
*if no input is provided, use default input for demo
$if not set INPUT $set INPUT uf50-01.cnf

set c             'clauses'
    pn            'positive/negative' / '+', '-' /,
    l             'literals'
    cnf(c<,l,pn)  'clauses to literals mapping'
;

* read cnf file and load content into GAMS data structures
$onEmbeddedCode Python:
cnf = list()
cnf.append(list())
maxvar = 0

with open(r'%INPUT%', 'r') as fcnf:
    for line in fcnf:
        tokens = line.split()
        if len(tokens) != 0 and tokens[0] not in ("p", "c", '%'):
            for tok in tokens:
                lit = int(tok)
                maxvar = max(maxvar, abs(lit))
                if lit == 0:
                    cnf.append(list())
                else:
                    cnf[-1].append(lit)
    
# drop empty clauses
cnf = [c for c in cnf if len(c)]

# build data for GAMS cnf set
gcnf = []
for ic, c in enumerate(cnf):
    for l in c:
        if l < 0:
            gcnf.append((f'c{ic}',str(-l),'-'))
        else:
            gcnf.append((f'c{ic}',str(l),'+'))

gams.set('l',[str(i+1) for i in range(maxvar)]);
gams.set('cnf',gcnf);
$offEmbeddedCode l cnf

* Simplest SAT Model
Binary variables
    b(l)         'encodes yes (1) or no (0) for literal'
;
Variable
    obj          'dummy objective variable'
;
Equation
    defclause(c) 'define clauses'
    defobj       'defined dummy objective'
;

defclause(c).. sum(cnf(c,l,'+'), b(l)) + sum(cnf(c,l,'-'), 1-b(l)) =g= 1;

defobj..       obj =e= sum(l, b(l));

model sat / all /;

* set absolute termination criterion to a value satisfied by any feasible solution
sat.optca = card(l)+1;

* solve SAT problem as MIP
solve sat max obj using mip;

* write solution file
$setNames "%INPUT%" fp fn fe
$if not set SOLOUTPUT $set SOLOUTPUT %fp%solution%fe%
file fr / "%SOLOUTPUT%" /; put fr;
put 'c Solution %INPUT%';
if(sat.modelstat=%modelStat.Optimal% or sat.modelstat=%modelStat.Integer Solution%,
   put / 's cnf 1 ' card(l):0:0 ' ' card(c):0:0;
   loop(l,
     put$(b.l(l)<0.5) / 'v -' l.tl:0;
     put$(b.l(l)>=0.5) / 'v ' l.tl:0;
   );
elseif sat.solvestat=%solveStat.Normal Completion%,
   put / 's cnf 0 ' card(l):0:0 ' ' card(c):0:0;
else
   put / 's cnf -1 ' card(l):0:0 ' ' card(c):0:0;
);

