PYBENCH_PATH="../../PyBench"
FRAMEWORK_PATH="../../enhanced_framework"
JAVA_11_HOME="/usr/lib/jvm/java-11-openjdk-amd64"

killBackgroundJobs() {
  echo "killing background jobs..."
  sudo fuser -k 6653/tcp || true
  sudo fuser -k 8080/tcp || true
  sudo fuser -k 9000/tcp || true
  sudo pkill -9 java
}

runExperiment(){
  NUMBER_OF_APPS=$1
  MATCHING_MESSAGES=$2

  killBackgroundJobs

  echo "starting framework..."
  SSL_PARAMETER=""
  if [ "$USE_SSL" = true ] ; then
    SSL_PARAMETER="_tls"
  fi
  CONFIG_FILE="../config_files/scenario_d2_${NUMBER_OF_APPS}_${MATCHING_MESSAGES}.txt"
  export JAVA_HOME=$JAVA_11_HOME 
  $FRAMEWORK_PATH/build/install/enhanced_framework/bin/enhanced_framework $CONFIG_FILE > enhanced_framework.log &

  sleep 15

  echo "starting PyBench..."
  python3 $PYBENCH_PATH/Main.py --port 9005 --warmup 20 --duration 700 --mode packetin --tls 0

  echo "creating output files..."
  sleep 60

  mv proxy_times.txt proxy_times_${NUMBER_OF_APPS}.txt
}

echo "disabling cpu boosting..."
sudo ../turbo-boost.sh disable

APP_NUMBERS_TO_TEST="1 2 4 8 16"
MATCHING_MESSAGES="0 20 40 60 80 100"
for x in $MATCHING_MESSAGES; do
  mkdir ${x}_percent_matching
  for i in $APP_NUMBERS_TO_TEST; do
    runExperiment $i $x
    mv proxy_times_$i.txt ./${x}_percent_matching/
  done
done

python3 plot_results.py matching_messages .
