#!/usr/bin/env bash

WATER=0
SODA=0
COKE=0
for i in "$@"
do
case $i in
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

for ((i=1; i<= SODA; i++)); do
    curl -X POST 127.0.0.1:9999/drinkRequest -d '{"drink": "soda"}' -w "\n" &
done

for ((i=1; i <=$WATE WATER; i++)); do
    curl -X POST 127.0.0.1:9999/drinkRequest -d '{"drink": "water"}' -w "\n" &
done

for ((i=1; i <= COKE; i++)); do
    curl -X POST 127.0.0.1:9999/drinkRequest -d '{"drink": "coke"}' -w "\n" &
done
