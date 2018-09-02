#!/usr/bin/env bash
export PGPASSWORD=admin
while true
do
clear
psql -U admin -h 127.0.0.1 -p 5433 -d test -tz -c "SELECT status, COUNT(*) FROM requested_drink GROUP BY status ORDER BY status ASC;"
echo "---------------------------------"
psql -U admin -h 127.0.0.1 -p 5433 -d test -tz -c "SELECT drink, status, COUNT(*) FROM requested_drink GROUP BY drink, status ORDER BY status ASC;"
sleep 1
done