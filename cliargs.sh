#!/bin/bash

java -cp "lib/test/*" org.testng.TestNG 2>/dev/null |\
grep "^    -.*" |\
sed -e "s/    -/-/" -e "s/, -/\n-/" |\
sed "/testRunFactory/d" |\
sort |\
sed '$s/,//' > "src/test/resources/testng-args.txt"

