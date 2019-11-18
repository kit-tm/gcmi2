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
  NUMBER_OF_FILTERS=$1
  CACHEABLE_MESSAGES=$2
  USE_CACHE=$3

  echo "running experiment with ${NUMBER_OF_FILTERS} filters and ${CACHEABLE_MESSAGES} cacheable messages"
  killBackgroundJobs

  echo "starting framework..."
  CACHE_PARAMETER="without_cache"
  if [ "$USE_CACHE" = true ] ; then
    CACHE_PARAMETER="with_cache"
  fi
  CONFIG_FILE="../config_files/scenario_d3_${CACHE_PARAMETER}_${NUMBER_OF_FILTERS}.txt"
  export JAVA_HOME=$JAVA_11_HOME 
  $FRAMEWORK_PATH/build/install/enhanced_framework/bin/enhanced_framework $CONFIG_FILE > enhanced_framework.log &

  sleep 15

  echo "starting PyBench..."
  python3 $PYBENCH_PATH/Main.py --port 9005 --warmup 20 --duration 700 --mode flowmod --tls 0 --percentage_of_same_ips $CACHEABLE_MESSAGES

  echo "creating output files..."
  sleep 60

  mv proxy_times.txt proxy_times_cache_${CACHEABLE_MESSAGES}.txt
  echo "saved file proxy_times_cache_${CACHEABLE_MESSAGES}.txt"
}

echo "disabling cpu boosting..."
sudo ../turbo-boost.sh disable

NUMBERS_OF_FILTERS="1000 4000 8000"
CACHEABLE_MESSAGES="0 20 40 60 80 100"
ALL_FOLDERS=""

for i in $NUMBERS_OF_FILTERS; do
  mkdir ${i}_filters
  mkdir ${i}_filters/with_cache
  mkdir ${i}_filters/without_cache
  ALL_FOLDERS="$ALL_FOLDERS ${i}_filters/with_cache ${i}_filters/without_cache"
done

for x in $CACHEABLE_MESSAGES; do
  for i in $NUMBERS_OF_FILTERS; do
    runExperiment $i $x true
    mv proxy_times_cache_$x.txt ./${i}_filters/with_cache/
    runExperiment $i $x false
    mv proxy_times_cache_$x.txt ./${i}_filters/without_cache/
  done
done

python3 plot_results.py filter_caches_same_messages $ALL_FOLDERS

