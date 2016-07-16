#!/bin/sh

test__do_download=""

while [ $1 ]
do
	case "$1" in
		-D) test__do_download=true
			;;
		-h|--help)
			echo 'Usage: ./run.sh [ -h | --help ] [ -D ]'
			echo 'Run the MangaTest-application'
			echo 'Parameters:'
			echo '  -h, --help Show this help and exit'
			echo '  -D automatically download a version of the MangaDownloader with curl'
			exit
			;;
		 *) echo unrecognised argument: $1
			exit
			;;
	esac
	
	shift
done

# optionally download the current version
if [ $test__do_download ] 
then
	echo downloading
	mkdir -p temp
	cd temp
	curl http://herrlock.github.io/MangaDist/snapshot/Manga-1.5.0-BETA-SNAPSHOT.zip -LO#
	cd ..
fi

# execute the test
java -jar MangaTest*.jar

# assert that no error was written
test__errorsize=$(stat -c %s temp/err.txt)
if [ $test__errorsize -gt 0 ]
then
	echo ERROR
else
	echo SUCCESS
fi

