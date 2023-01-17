$Title A wrapper for the COIN-OR Semidefinite Programming Solver CSDP

Set m, n, mLE(m), mGE(m); alias (n,i,j);
Parameter
* Input
    c(m)      cost coefficients
    F(m,i,j)  constraint matrix
    F0(i,j)   constant term
* Output
    xvec(m)   variable vector
    X(i,j)    variable matrix
    Y(i,j)    dual matrix;


$if not set  infile $set  infile csdpin
$if not set outfile $set outfile csdpout

set s order;

$if not set strict $set strict 0

$gdxin %infile%
$load n m c F F0

$if not errorfree $abort 'Compilation errors'
$load mLE
$if errorfree $goto loadmGE
$clearerror
mLE(m) = no;

$label loadmGE
$load mGE
$if errorfree $goto startCSDP
$clearerror
mGE(m) = no;
$label startCSDP

Set upper(i,j), lower(i,j);
upper(i,j)$(ord(i)<=ord(j)) = yes;
lower(i,j)$(ord(i) >ord(j)) = yes;

* Assumption: We get permuted upper triangular matrices F and F0
Set errorF0(i,j), errorF(m,i,j);

errorF0(lower(i,j)) = F0(i,j) and F0(j,i);
abort$card(errorF0) 'F0 not permuted upper triangular', errorF0;

errorF(m,lower(i,j)) = F(m,i,j) and F(m,j,i);
abort$card(errorF) 'F not permuted upper triangular', errorF;

* Make them all upper
F0(i,j)$lower(j,i)  $= F0(j,i);  F0(lower) = 0;
F(m,i,j)$lower(j,i) $= F(m,j,i); F(m,lower) = 0;

$ontext
$load Y
Parameter rep;
rep(m,'lhs')  = sum(lower(j,i), 2*F(m,i,j)*Y(j,i)) + sum(i,F(m,i,i)*Y(i,i));
rep(m,'rhs')  = c(m);
rep(m,'diff') = rep(m,'lhs') - rep(m,'rhs');
display rep;
$exit
$offtext

file CSDPin / csdp.in /; CSDPin.nw=0; CSDPin.nd=0; CSDPin.pw=100;
put CSDPin card(m):0:0 ' =mdim'/ (1+1$(card(mLE) + card(mGE))):0 ' =nblocks' / '{' card(n):0:0
put$(card(mLE) + card(mGE)) ', -' (card(mLE) + card(mGE)):0:0
put '}' /;
loop(m, put$(CSDPin.cc > CSDPin.pw-20) '&' /; put c(m)::15 ' ' );

* F0(i,j)
loop(upper(i,j)$F0(i,j),
  put / '0 1 ' i.ord ' ' j.ord ' ' F0(i,j)::15 );

* F(m,i,j)
scalar slackID /0/;
loop(m,
  loop(upper(i,j)$F(m,i,j),
    put / m.ord ' 1 ' i.ord ' ' j.ord ' ' F(m,i,j)::15 );
  if ((mLE(m) or mGE(m)),
    slackID=slackID+1;
    put$mLE(m) / m.ord ' 2 ' slackID:0:0 ' ' slackID:0:0 '  1.0';
    put$mGE(m) / m.ord ' 2 ' slackID:0:0 ' ' slackID:0:0 ' -1.0' ));
putclose;

* AWK script to concatenate lines that end with a "&"
$onechoV > concat.awk
 /&$/ { for (i=1; i<NF; i++) printf("%s ", $i) }
!/&$/ { print $0 }
$offecho

* AWK Script to produce a GAMS readable SDP solution file
* We skip the slack variables for the inequality constraints
$onechoV > csdp2gms.awk
NR==1 { print "parameter xvec_(*) /\n$offdigit ondelim";
        for (i=1; i<=NF; i++) print "sdpm" i " " $i;
        print "/\nparameter X_(*,*) /"; }
NR>1 && $1==1 { if ($2==1) print "sdpn" $3 " sdpn" $4 " " $5; }
NR>1 && $1==2 { if (0==first) { print "/\nparameter Y_(*,*) /"; first=1; }
                if ($2==1) print "sdpn" $3 " sdpn" $4 " " $5; }
END { print "/"; }
$offecho

$call rm -f csdp.out csdpO.gms
execute 'awk -f concat.awk csdp.in > csdpX.in && csdp csdpX.in csdp.out';
$if %strict%==1 abort$(errorlevel <> 0 and errorlevel <> 3) 'Problems running csdp';
execute 'awk -f csdp2gms.awk csdp.out > csdpO.gms && gams csdpO lo=%gams.lo% gdx=csdpO.gdx';
abort$errorlevel 'Problems running csdp';

$eval nmax card(n)
$eval mmax card(m)
set n_ / sdpn1*sdpn%nmax% /
    m_ / sdpm1*sdpm%mmax% /
* nn_ is really big
    nn_(n,n_) /#n:#n_/, nn_2(n,n_), mm_(m,m_) /#m:#m_/; nn_2(nn_) = yes;
Alias (n_, i_, j_);

* SDP solution comes back as a full matrix
Parameter
    xvec_(m_)  variable vector
    X_(n_,n_)  variable matrix
    Y_(n_,n_)  dual matrix;

execute_load 'csdpO.gdx', xvec_, X_, Y_;
xvec(m) = sum(mm_(m,m_), xvec_(m_));
X(i,j)  = sum((nn_(i,i_),nn_2(j,j_)), X_(i_,j_)); X(i,j)$(ord(i)>ord(j)) = X(j,i);
Y(i,j)  = sum((nn_(i,i_),nn_2(j,j_)), Y_(i_,j_)); Y(i,j)$(ord(i)>ord(j)) = Y(j,i);

execute_unload '%outfile%', xvec, X, Y, n, m;
