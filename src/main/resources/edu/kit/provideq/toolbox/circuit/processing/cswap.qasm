OPENQASM 2.0;
include "qelib1.inc";
qreg q[3];
crz(0.5) q[0], q[1];
t q[2];
cswap q[2], q[0], q[1];
