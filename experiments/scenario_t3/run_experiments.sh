CBENCH_PATH="../../../oflops/cbench"
FRAMEWORK_PATH="../../enhanced_framework"
FLOODLIGHT_PATH="../../floodlight"
JAVA_11_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
JAVA_8_HOME="/usr/lib/jvm/java-8-openjdk-amd64"

killBackgroundJobs() {
  echo "killing background jobs..."
  sudo fuser -k 6653/tcp || true
  sudo fuser -k 8080/tcp || true
  sudo fuser -k 9000/tcp || true
  sudo pkill -9 java
}

runExperiment(){
  NUMBER_OF_APPS=$1
  MATCHING=$2
  CBENCH_PORT="9005"

  killBackgroundJobs

  if [ "$NUMBER_OF_APPS" = "0" ]; then 
    CBENCH_PORT="6653"
  else
    echo "starting framework..."
    CONFIG_FILE="../config_files/scenario_t3_${NUMBER_OF_APPS}_${MATCHING}.txt"
    export JAVA_HOME=$JAVA_11_HOME
    $FRAMEWORK_PATH/build/install/enhanced_framework/bin/enhanced_framework $CONFIG_FILE > /dev/null &

    sleep 15
  fi

  echo "starting floodlight..."
  $JAVA_8_HOME/bin/java -Djava.library.path=$FLOODLIGHT_PATH -jar $FLOODLIGHT_PATH/target/floodlight.jar -cf $FLOODLIGHT_PATH/src/main/resources/learningswitch.properties > floodlight.log &

  until nc -z localhost 6653; do sleep 0.1; done;
  sleep 10

  echo "starting cbench"
  $CBENCH_PATH/cbench --delay 2000 --loops 700 --warmup 20 --switches 1 -p $CBENCH_PORT -t > cbench_throughput_${NUMBER_OF_APPS}.log
  sleep 5
}

echo "disabling cpu boosting"
sudo ../turbo-boost.sh disable


MATCHING_MESSAGES="0 20 40 60 80 100"
APP_NUMBERS_TO_TEST="1 2 4 8 16"
for x in $MATCHING_MESSAGES; do
  mkdir ${x}_percent_matching
  for i in $APP_NUMBERS_TO_TEST; do
    runExperiment $i $x
    mv cbench_throughput_${i}.log ./${x}_percent_matching/
  done
done

python3 plot_results.py throughput_filter .
