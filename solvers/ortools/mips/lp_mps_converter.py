import cplex

def convert_model(input_file, output_file):
    """
    Loads a model (LP or MPS) into CPLEX and writes it back out
    in the requested format (.lp or .mps).
    """
    # Create a CPLEX object and read in the model
    model = cplex.Cplex(input_file)
    model.write(output_file)
