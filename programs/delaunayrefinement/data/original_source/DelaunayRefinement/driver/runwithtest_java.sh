#! /bin/bash

if [ $# -ne 1 ]; then
  echo 1>&2 Usage: $0 "(runA | runB | runC)"
  exit -1
fi

if [ ! -f ../$1/input/refinement.sh ]; then
  echo 1>&2 Usage: $0 "(runA | runB | runC)"
  exit - 1
fi

rm -f refinement.out
../$1/input/refinement.sh v > refinement.out
if diff -q -w refinement.out ../$1/output/refinement.out > /dev/null; then
  echo "completed successfully"
  rm -f refinement.out
else
  echo "error in output"
fi
