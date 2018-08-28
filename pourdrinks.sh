#!/usr/bin/env bash

WATER=0
SODA=0
COKE=0
MAX_PARALLEL=1
for i in "$@"
do
case $i in
    --par=*)
    MAX_PARALLEL="${i#*=}"
    shift # past argument=value
    ;;
    --water=*)
    WATER="${i#*=}"
    shift # past argument=value
    ;;
    --soda=*)
    SODA="${i#*=}"
    shift # past argument=value
    ;;
    --coke=*)
    COKE="${i#*=}"
    shift # past argument=value
    ;;
    *)
          # unknown option
    ;;
esac
done

ACC_STR=''
for ((i=1; i<= SODA; i++)); do
    ACC_STR="soda\n$ACC_STR"
done

for ((i=1; i <=$WATER; i++)); do
    ACC_STR="water\n$ACC_STR"
done

for ((i=1; i <=$COKE; i++)); do
    ACC_STR="coke\n$ACC_STR"
done

printf $ACC_STR | gshuf - | xargs -P $MAX_PARALLEL -I {} curl -X POST 127.0.0.1:9999/drinkRequest -d '{"drink": "{}"}' -w "\n"