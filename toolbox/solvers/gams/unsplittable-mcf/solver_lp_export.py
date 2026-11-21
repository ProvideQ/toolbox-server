"""
LP Export for Unsplittable Multi Commodity Flow Problem.

This solver exports the MCF model to LP format (QUBO reformulation).
"""

import sys
from unsplittable_mcf import UnsplittableMCF

arg_count = len(sys.argv) - 1
if arg_count != 2:
    raise TypeError(
        f"This script expects exactly 2 arguments but got {arg_count}. "
        "Input file (argument 1) and output file (argument 2)."
    )

input_path = sys.argv[1]
output_path = sys.argv[2]

print(f"Loading instance from {input_path}...")

# Create model from instance file
model = UnsplittableMCF(instance_file=input_path)

print("Building equations and model...")
model.build_equations_and_model()

# Export to LP format (QUBO reformulation)
print(f"\n--- Exporting to LP format: {output_path} ---")
model.export_lp(output_path)
