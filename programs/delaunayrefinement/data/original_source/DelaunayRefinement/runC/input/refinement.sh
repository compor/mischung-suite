#! /bin/bash
INPUT=250k.2
JFLAGS="-Xms2048M -Xmx4096M"
ABS_DIR=$(cd $(dirname $0); pwd)

source "$ABS_DIR/../../../config_java"

$JAVA $JFLAGS -cp ../../Lonestar-2.1.jar DelaunayRefinement.src.java.SerialDelaunayrefinement ../runC/input/$INPUT $1
