Install Guide for the VRP-Pipeline (used for K-means, Two Phase Clustering, VRP to QUBO convertion):
1. Install Rust: https://www.rust-lang.org/tools/install
2. Install a specific Rust nightly build (needed cause the solver uses experimental features): `rustup install nightly-2023-07-01`
3. Check how the nightly build is called on your machine (this is shown when running the install command, on Mac it is called *nightly-2023-07-01-aarch64-apple-darwin*)
4. Set the nightly build as default: `rustup default nightly-2023-07-01(... specific version name on machine)`
5. Download source code of the VRP-Pipeline: https://github.com/ProvideQ/hybrid-vrp-solver
6. build the source code using `cargo build`
7. Put the build binary in `solvers/berger-vrp/bin`, replace the binary that matches your OS.
