import json
import sys
from pathlib import Path
from typing import Any

from run import run


def _read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, data: dict[str, Any]) -> None:
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: api_file.py <input_file_path> <output_file_path>",
              file=sys.stderr)
        return 2

    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])
    payload = _read_json(input_path)

    result = run(
        strategy=payload["strategy"],
        qasm_a=payload["qasmA"],
        qasm_b=payload["qasmB"],
        qcec_options=payload.get("qcecOptions", {}),
    )
    _write_json(output_path, result)

    return 0


if __name__ == "__main__":
    sys.exit(main())
