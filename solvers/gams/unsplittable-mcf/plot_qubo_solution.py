import os
import sys
from unsplittable_mcf import UnsplittableMCF

arg_count = len(sys.argv) - 1
if arg_count != 3:
    raise TypeError(
        f"This script expects exactly 3 arguments but got {arg_count}. "
        "Unsplittable MCF instance file (argument 1), solution file (argument 2), and output HTML file (argument 3)."
    )

unsplittable_mcf_path = sys.argv[1]
solution_path = sys.argv[2]
output_path = sys.argv[3]

if os.path.exists(solution_path):
    mapper = UnsplittableMCF(instance_file=unsplittable_mcf_path)
    mapper.build_equations_and_model()

    mapper.plot_json_solution(
        json_path=solution_path, output_html=output_path
    )
else:
    print(f"Error: '{solution_path}' not found. Please run the external solver first.")
