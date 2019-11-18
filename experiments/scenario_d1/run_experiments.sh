PYBENCH_PATH="../../PyBench"
FLOODLIGHT_PATH="../../floodlight"
RYU_PATH="../../../ryu"
FRAMEWORK_PATH="../../enhanced_framework"
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
  USE_SSL=$2
  CONTROLLER=$3
  PROXY_MODE=$4
  PYBENCH_PORT="9005"

  killBackgroundJobs

  if [ "$NUMBER_OF_APPS" = "0" ]; then 
    PYBENCH_PORT="6653"
  else
    echo "starting framework..."
    SSL_PARAMETER=""
    if [ "$USE_SSL" = true ] ; then
      SSL_PARAMETER="_tls"
    fi
    CONFIG_FILE="../config_files/scenario_d1_${NUMBER_OF_APPS}_${PROXY_MODE}${SSL_PARAMETER}.txt"
    export JAVA_HOME=$JAVA_11_HOME 
    $FRAMEWORK_PATH/build/install/enhanced_framework/bin/enhanced_framework $CONFIG_FILE > enhanced_framework.log &
    sleep 10
  fi;

  if [ "$CONTROLLER" = "ryu" ]; then
    echo "starting ryu..."
    /usr/local/bin/ryu-manager $RYU_PATH/ryu/app/simple_switch.py > ryu.log &
  else
    echo "starting floodlight..."
    CONFIG_FILE="learningswitch.properties"
    if [ "$USE_SSL" = true ] ; then
      CONFIG_FILE="learningswitch_tls.properties"
    fi
    $JAVA_8_HOME/bin/java -Djava.library.path=$FLOODLIGHT_PATH -jar $FLOODLIGHT_PATH/target/floodlight.jar -cf $FLOODLIGHT_PATH/src/main/resources/$CONFIG_FILE > floodlight.log &
  fi;
  
  until nc -z localhost 6653; do sleep 0.1; done;
  sleep 10

  echo "starting PyBench..."
  SSL_PARAMETER="0"
  if [ "$USE_SSL" = true ] ; then
    SSL_PARAMETER="1"
  fi
  python3 $PYBENCH_PATH/Main.py --port $PYBENCH_PORT --warmup 20 --duration 70 --mode latency --tls $SSL_PARAMETER > pybench.log

  echo "creating output files..."
  sleep 60

  if [ "$CONTROLLER" = "ryu" ]; then
    mv ryu_times.txt controller_times_${NUMBER_OF_APPS}.txt
  else
    mv floodlight_times.txt controller_times_${NUMBER_OF_APPS}.txt
  fi;

  mv cbench_times.txt cbench_times_${NUMBER_OF_APPS}.txt
  mv proxy_times.txt proxy_times_${NUMBER_OF_APPS}.txt
}

runDefault(){
  echo "disabling cpu boosting..."
  sudo ../turbo-boost.sh disable

  APP_NUMBERS_TO_TEST="1 2 4 8 16"
  for i in $APP_NUMBERS_TO_TEST; do
    runExperiment $i false floodlight single
  done

  python3 plot_results.py --path . --outputfile plot.pdf
}

runWithRyu(){
  echo "disabling cpu boosting..."
  sudo ../turbo-boost.sh disable

  APP_NUMBERS_TO_TEST="1 2 4 8 16"
  for i in $APP_NUMBERS_TO_TEST; do
    runExperiment $i false ryu single
  done

  python3 plot_results.py --path . --outputfile plot_ryu.pdf
}

runWithTls(){
  echo "disabling cpu boosting..."
  sudo ../turbo-boost.sh disable

  APP_NUMBERS_TO_TEST="1 16"
  for i in $APP_NUMBERS_TO_TEST; do
    runExperiment $i true floodlight single
  done

  python3 plot_results.py --path . --outputfile plot_tls.pdf
}

runWithMultipleProxies(){
  echo "disabling cpu boosting..."
  sudo ../turbo-boost.sh disable

  APP_NUMBERS_TO_TEST="1 2 4 8 16"
  for i in $APP_NUMBERS_TO_TEST; do
    runExperiment $i false floodlight multiple
  done

  python3 plot_results.py --path . --outputfile plot_multiple_proxies.pdf
}


runDefault

runWithRyu

runWithTls

runWithMultipleProxies
