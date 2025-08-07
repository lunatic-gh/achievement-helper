import json
import sys

import vdf

if __name__ == '__main__':
  with open(sys.argv[1], "rb") as f:
    schema = vdf.binary_load(f)
    print(json.dumps(schema))
