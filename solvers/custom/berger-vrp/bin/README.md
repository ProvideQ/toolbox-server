### How to Cross-compile the Rust Pipeline:

Guide is written for Mac-ARM System

1. Download Repo: https://github.com/ProvideQ/hybrid-vrp-solver
2. Install Rust: https://www.rust-lang.org/tools/install
3. Install a specific Rust nightly build (needed cause the solver uses experimental features): `rustup install nightly-2023-07-01` 
4. Check how the nightly build is called on your machine (this is shown when running the install command, on Mac it is called *nightly-2023-07-01-aarch64-apple-darwin*)
5. Set the nightly build as default: `rustup default nightly-2023-07-01(... specific version name on machine)`

#### Mac Version: (native)
1. build the source code using `cargo build --release`

#### Windows Version (cross compilation)
1. Add Target for Windows: `rustup target add x86_64-pc-windows-gnu`
2. Install Cross Toolchain: `brew install mingw-w64`
3. Create .cargo/config.toml and add the following lines: <br>
```
[target.x86_64-pc-windows-gnu]
linker = "x86_64-w64-mingw32-gcc"
```
4. run `cargo build --target x86_64-pc-windows-gnu --release`

#### Linux Version (cross compilation) 
* only gnu is supported, Lucas Bergers code does not support static linking
1. Target for Linux: `rustup target add 86_64-unknown-linux-gnu`
2. Toolchain: `arch -arm64 brew install SergioBenitez/osxct/x86_64-unknown-linux-gnu`
3. Create .cargo/config.toml and add the following lines: (Path for linker: `which x86_64-unknown-linux-gnu`)<br>
```
[target.x86_64-pc-windows-gnu]
linker = "/opt/homebrew/bin/x86_64-unknown-linux-gnu-gcc"
```
4. run `cargo build --target x86_64-unknown-linux-gnu --release`
5. When you have issues, check here: https://github.com/briansmith/ring/issues/1605

