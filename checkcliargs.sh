#!/bin/bash

MAIN="org.testng.TestNG"
TMPNEW=/tmp/checkcliargs-new
TMPOLD=/tmp/checkcliargs-old

java -cp "lib/test/*" $MAIN >$TMPNEW
java -cp "examples/lib/test/*" $MAIN >$TMPOLD

if [ "$1" = "-v" ]; then
	code --wait --diff $TMPOLD $TMPNEW
else
	diff $TMPOLD $TMPNEW
fi

rm -rf $TMPNEW $TMPOLD
