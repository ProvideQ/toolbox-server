import json
from datetime import datetime
from math import sqrt
from typing import Literal

from dimod import BinaryQuadraticModel, Sampler, SampleSet, SimulatedAnnealingSampler
from dwave.embedding import minorminer
from dwave.system import DWaveSampler, FixedEmbeddingComposite, LeapHybridSampler
from embeddings import cached_embeddings
from hybrid import SimplifiedQbsolv, State
from hybrid.profiling import make_timeit

solvertype = Literal["sim", "hybrid", "qbsolv", "direct"]


def solve_with(bqm: BinaryQuadraticModel, type: solvertype, label: str) -> SampleSet:
    if type == "sim":
        last = datetime.now().timestamp()
        sampler: Sampler = SimulatedAnnealingSampler()
        print(f"sampler created took {datetime.now().timestamp() - last}")
        return sampler.sample(bqm)
    elif type == "direct":
        last = datetime.now().timestamp()
        sampler: Sampler = DWaveSampler(solver={"topology__type": "zephyr"})
        print(f"sampler created took {datetime.now().timestamp() - last}")
        last = datetime.now().timestamp()
        tsp_size = int(sqrt(bqm.num_variables))
        if tsp_size in cached_embeddings.keys():
            embedding = cached_embeddings[tsp_size]
        else:
            print("start embedding")
            embedding = minorminer.find_embedding(
                list(bqm.quadratic) + [(v, v) for v in bqm.linear],
                sampler.edgelist,
            )
            print(f"found new embedding for {tsp_size}")
            print(embedding)
        print(f"got embedding {datetime.now().timestamp() - last}")
        max_tries = 3

        while max_tries > 0:
            sampleset = FixedEmbeddingComposite(sampler, embedding).sample(
                bqm,
                num_reads=250,
                label=f"DWaveSampler with embedding num_reads=1000 {label}",
            )
            values = list(sampleset.first.sample.values())
            print(f"checking out sample: {values}")
            valid = True
            for i in range(0, tsp_size):
                x = sum(values[i * tsp_size : (i + 1) * tsp_size])
                print(x)
                if x != 1:
                    valid = False

            if valid:
                return sampleset
            max_tries -= 1
            print(f"{max_tries} left")
    elif type == "hybrid":
        last = datetime.now().timestamp()
        sampler: Sampler = LeapHybridSampler()
        print(f"sampler created took {datetime.now().timestamp() - last}")
        return sampler.sample(
            bqm, time_limit=10, label=f"LeapHybridSampler num_reads=250: {label}"
        )
    elif type == "qbsolv":
        last = datetime.now().timestamp()
        init_state = State.from_problem(bqm)
        workflow = SimplifiedQbsolv(max_iter=3, max_time=10)
        print(f"workflow created took {datetime.now().timestamp() - last}")
        final_state = workflow.run(init_state).result()

        print(json.dumps(workflow.timers))

        return final_state.samples
