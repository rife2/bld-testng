#!/bin/bash

main="org.testng.TestNG"
new=/tmp/checkcliargs-new
old=/tmp/checkcliargs-old

java -cp "lib/test/*" $main 2>/dev/null >$new
java -cp "examples/lib/test/*" $main 2>/dev/null >$old

if [ "$1" = "-v" ]; then
	code --wait --diff $old $new
else
	diff $old $new
fi

rm -rf $new $old
