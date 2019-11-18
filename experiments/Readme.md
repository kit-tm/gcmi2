# Experiments
These installation steps and scripts have been tested on Ubuntu 18.04.

## Installation
First, the following packages have to be installed:

    sudo apt install ant openjdk-8-jdk python3-pip dh-autoreconf libsnmp-dev libpcap-dev libconfig-dev
    pip3 install setuptools

### Floodlight
    cd floodlight
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
    ant

### Ryu
    git clone https://github.com/osrg/ryu.git
    cd ryu
    git checkout 1c008060fa3dab51c3a59c1485a7529b13cf0dd1
    git apply ~/ma-daniel-geiger/ryu.patch
    pip3 install -r ./tools/pip-requires
    python3 setup.py install

### Enhanced Framework
    cd enhanced_framework
    ./gradlew installDist
    
### PyBench
    cd PyBench
    pip3 install -r requirements.txt

### CBench
    cd ~
    git clone https://github.com/mininet/openflow.git
    git clone https://github.com/mininet/oflops.git
    cd oflops
    git checkout 762d51786f88b6da834b3eee1e1ac7212ef31dea
    git apply ~/ma-daniel-geiger/oflops.patch
    ./boot.sh
    ./configure
    make
    sudo make install

## Usage
For each scenario, the "run_experiments.sh" script executes the experiments and generates a plot. The paths declared at the top of each script might have to be adapted.