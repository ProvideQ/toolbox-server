NAME          EXAMPLE
ROWS
 N  COST             <- (N) indicates the objective row
 G  C1               <- (G) 'Greater than or equal' constraint
 L  C2               <- (L) 'Less than or equal' constraint
COLUMNS
    X      COST     1       <- Objective coefficient for X is 1
    X      C1       1       <- X's coefficient in constraint C1
    Y      COST     3       <- Objective coefficient for Y is 3
    Y      C1       1       <- Y's coefficient in constraint C1
    Y      C2       1       <- Y's coefficient in constraint C2
RHS
    RHS    C1       4       <- Right-hand side of constraint C1
    RHS    C2       3       <- Right-hand side of constraint C2
BOUNDS
 LO BND    X        0       <- Lower bound of X is 0
 LO BND    Y        0       <- Lower bound of Y is 0
ENDATA
