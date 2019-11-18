# PyBench

PyBench is a tool that can generate, send and receive different types of OpenFlow messages. It saves timestamps when sending or receiving messages and can be used to perform benchmarks with the enhanced framework or an SDN controller. PyBench supports three different modes for generating and sending OpenFlow messages:

* In the first mode, PyBench simulates an SDN switch and sends Packet-In and Echo-Request OpenFlow messages for measuring the control plane delay. After sending a benchmarking message, PyBench waits for the controller to respond by sending a Packet-Out and Echo-Response message before the next message is generated. In this mode PyBench also simulates a real SDN switch. This means that it tries to respond correctly to all requests an SDN controller might send.
* In the second mode, PyBench continuously generates Packet-In and Echo-Request OpenFlow messages without simulating an SDN switch. Therefore this mode can only be used for measuring the delay of the framework without forwarding any message to an SDN controller.
* In the third mode, PyBench continuously generates Flow-Modification messages. Since this OpenFlow message type cannot be sent by an SDN switch, PyBench does not simulate a switch in this mode. However, generating Flow-Modification messages with varying contents is required to test the filtering performance of the enhanced framework.

## Installation
Python 3.6 or later and pip are required.

    pip install -r requirements.txt

## Usage

    python Main.py --ip <IP> --port <port> --warmup <warmup> --duration <duration> --mode <mode> --tls <tls> --percentage_of_same_ips <percentage_of_same_ips>
|Argument|Required|Description|Default|
|--|--|--|--|
|ip|no|IP address of an SDN controller to connect to|127.0.0.1|
|port|no|Port of an SDN controller to connect to|6653|
|warmup|yes|Warmup time in seconds during which messages are sent but no measurements are made|-|
|duration|yes|Duration in seconds until the experiment is stopped|-|
|mode|no|Either "latency", "packetin" or "flowmod" (see description above)|latency|
|tls|no|Use TLS or plain TCP|false|
|percentage_of_same_ips|no|Only relevant in flowmod mode. Refers to IP addresses in the generated Flow-Modification messages match fields|0|
