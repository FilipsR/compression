#!/bin/sh
(
	for file in test/*.html test/dev-random-1k test/lorem-ipsum-7k; do
		echo 'comp'
		echo "$file"
		echo "$file.compressed"
		echo 'decomp'
		echo "$file.compressed"
		echo "$file.original"
		echo 'size'
		echo "$file.original"
		echo 'size'
		echo "$file.compressed"
		echo 'equal'
		echo "$file"
		echo "$file.original"
	done
	echo 'about'
	echo 'exit'
) | java -ea -cp build Main | grep -vE '(command|name):' | nl
