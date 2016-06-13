#!/bin/sh

test__do_download=true

while [ $1 ]
do
	case "$1" in
		-D) test__do_download=""
			;;
		*) echo unrecognised argument: $1
			;;
	esac
	
	shift
done

if [ $test__do_download ] 
then
	echo downloading
	mkdir -p temp
	cd temp
	curl http://herrlock.github.io/MangaDist/snapshot/Manga-1.4.0-BETA-SNAPSHOT.zip -LO#
	cd ..
fi
java -jar MangaTest*.jar
