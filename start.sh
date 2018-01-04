#!/usr/bin/env bash

hadoop jar h2o-hadoop/h2o-hdp2.2-assembly/build/libs/h2odriver.jar -nodes 2 -notify notif.txt -mapperXmx 6g -disown -output test2 -jobname HangApp
head -n 1 notif.txt > flatfile.txt

java -cp h2o-hadoop/h2o-hdp2.2-assembly/build/libs/h2odriver.jar water.HangApp -flatfile flatfile.txt -client -name HangApp

app=$(tail -n 1 notif.txt | cut -f2- -d"_")

echo "Clean up"
hadoop fs -rmr test2
yarn application -kill application_${app}





#!/usr/bin/env bash

hadoop jar h2o-hadoop/h2o-hdp2.2-assembly/build/libs/h2odriver.jar -nodes 2 -notify notif.txt -mapperXmx 6g -disown -output test2 -jobname HangApp
head -n 1 notif.txt > flatfile.txt

java -cp h2o-hadoop/h2o-hdp2.2-assembly/build/libs/h2odriver.jar water.H2OApp -flatfile flatfile.txt -name HangApp

java -cp h2o-hadoop/h2o-hdp2.2-assembly/build/libs/h2odriver.jar water.HangApp -flatfile flatfile.txt -client -name HangApp

app=$(tail -n 1 notif.txt | cut -f2- -d"_")

echo "Clean up"
hadoop fs -rmr test2
yarn application -kill application_${app}


172.16.2.184:54321
172.16.2.185:54321


java -cp h2o-hadoop/h2o-hdp2.2-assembly/build/libs/h2odriver.jar water.HangApp -flatfile flatfile.txt -client -name HangApp
