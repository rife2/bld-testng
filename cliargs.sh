#!/bin/bash

MAIN="org.testng.TestNG"
TMP=/tmp/cliargs

java -cp "lib/test/*" $MAIN >$TMP 2>/dev/null

cat $TMP |\
grep "^    -.*" |\
sed -e "s/    -/-/" -e "s/, -/\n-/" |\
sed "/testRunFactory/d" |\
sort |\
sed '$s/,//' > "src/test/resources/testng-args.txt"

rm -rf $TMP
