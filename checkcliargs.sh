#!/bin/bash

MAIN="org.testng.TestNG"
TMPNEW=/tmp/checkcliargs-new
TMPOLD=/tmp/checkcliargs-old

java -cp "lib/test/*" $MAIN >$TMPNEW
java -cp "examples/lib/test/*" $MAIN >$TMPOLD

diff $TMPOLD $TMPNEW

rm -rf $TMPNEW $TMPOLD
