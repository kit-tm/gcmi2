
# Enhanced Framework

## Usage

    ./gradlew installDist
    ./build/install/enhanced_framework/bin/enhanced_framework <path_to_config_file>

## Configuration
The path of a configuration file can be passed as an argument when starting the framework. Otherwise, the default configuration is used. A configuration file looks like this:

    # Use tls for all connections between switch, all proxies and controller
    useTls = false
    
    # IP and Port for upstream connection to SDN controller
    upstreamIp = 127.0.0.1
    upstreamPort = 6653
    
    # Port to listen on for Switches to connect
    downstreamPort = 9005
    
    # If set to true, each GCMI App uses a separate proxy. If set to false, all GCMI Apps use the same proxy.
    multipleProxies = true
    
    # List of GCMI Apps that intercept all messages sent from a switch in this order
    # and messages sent from a controller in reverse order
    gcmiApps = \
     com.dgeiger.enhanced_framework.apps.examples.ScenarioD1App,\
     com.dgeiger.enhanced_framework.apps.examples.ScenarioD1App,\
     com.dgeiger.enhanced_framework.apps.examples.ScenarioD1App
     
    # Parameter that is passed to the constructor of all GCMI Apps (optional)
    #appIntegerParameter = 40
    
    # If set to true, a initialMessage has to match at least one filter of a GCMI App to be forwarded to that GCMI App.
    # If set to false, any initialMessage hast to match all filters of a GCMI App to be forwarded to that GCMI App.
    matchAgainstAtLeastOneFilter = false
    
    # use caching to speed up filtering
    useCache = false
    
    # Save a textfile containing timestamps for received and sent messages for benchmarking experiments
    saveTimestamps = true

## GCMI Apps
New GCMI Apps can be created easily: Create a new Class implementing the "App" or "FilterApp" interface, then insert this new GCMI App in the configuration file.

### Examples

 - [LogAndForward GCMI App](./src/main/java/com/dgeiger/enhanced_framework/apps/examples/LogAndForwardApp.java): This GCMI App logs all intercepted messages to the console and forwards them without performing any modifications.
 - [LogAndForwardFilter GCMI App](./src/main/java/com/dgeiger/enhanced_framework/apps/examples/LogAndForwardFilterApp.java): This GCMI App applies two filters to only receive messages of a specific type and size.
 - [Payless GCMI App](./src/main/java/com/dgeiger/enhanced_framework/apps/examples/PaylessApp.java): A modified version of the [existing Payless implementation](https://github.com/kit-tm/gcmi/blob/master/examples/payless/src/main/java/com/github/sherter/jcon/examples/payless/Payless.java) adapted for the enhanced framework. Payless is a caching layer for monitoring requests inspired by [this Paper](https://ieeexplore.ieee.org/document/6838227).