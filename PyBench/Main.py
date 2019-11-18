from Switch import Switch
from pyof.foundation.basic_types import DPID
from FileWriter import FileWriter
from FlowModGenerator import FlowModGenerator
from PacketInEchoGenerator import PacketInEchoGenerator
import time
import argparse


def run_latency(warmup_time_s, upstream_ip, upstream_port, duration_s, ssl):
    switch = Switch(DPID("00:00:00:00:00:00:00:01"), warmup_time_s, upstream_ip, upstream_port, 1, False, ssl)
    switch.start()

    time.sleep(duration_s)
    print("stop!")

    switch.stop()

    FileWriter().write_results_to_file(switch.get_results())

def run_flowmodgenerator(upstream_ip, upstream_port, duration_s, ssl, percentage_of_same_ips):
    flowModGenerator = FlowModGenerator(upstream_ip=upstream_ip, upstream_port=upstream_port, enable_tls=ssl, percentage_of_same_ips=percentage_of_same_ips)

    flowModGenerator.start()

    time.sleep(duration_s)
    print("stop!")

    flowModGenerator.stop()

def run_packetinechogenerator(upstream_ip, upstream_port, duration_s, ssl):
    packetInEchoGenerator = PacketInEchoGenerator(upstream_ip=upstream_ip, upstream_port=upstream_port, enable_tls=ssl)

    packetInEchoGenerator.start()

    time.sleep(duration_s)
    print("stop!")

    packetInEchoGenerator.stop()

def eval_throughput_results(switch):

    print("===== switch " + switch.dpid + " =====")
    results = switch.get_results()
    results.sort(key=lambda x: x[0])

    time_range_s = (results[-1][0] - results[0][0]) / 1000000

    messages_count = 0
    no_response_count = 0

    while len(results) > 0:
        measurement = results.pop(0)
        other_measurement = find_xid(measurement[1], results)
        if(other_measurement != -1):
            results.pop(other_measurement)
            messages_count += 1
        else:
            no_response_count += 1

    messages_per_s = messages_count / time_range_s
    print("exchanged " + str(messages_count) + " messages")
    print(str(messages_per_s) + " messages per second")
    print("did not receive a response for " + str(no_response_count) + " messages")
    print("===============================================")
    print("")

    return messages_count, messages_per_s, no_response_count


def find_xid(xid, results):
    for i, measurement in enumerate(results):
        if measurement[1] == xid:
            return i

    return -1

def str2bool(v):
  return v.lower() in ("yes", "true", "t", "1")

if __name__ == '__main__':
    ip = '127.0.0.1'
    port = 6653
    duration = 0
    warmup = 0
    number_of_switches = 1
    percentage_of_same_ips=0
    tls = False

    parser = argparse.ArgumentParser(description="Description for my parser")
    parser.add_argument("-i", "--ip", required=False)
    parser.add_argument("-p", "--port", required=False)
    parser.add_argument("-w", "--warmup", required=True)
    parser.add_argument("-d", "--duration", required=True)
    parser.add_argument("-m", "--mode", required=False)
    parser.add_argument("-t", "--tls", required=False)
    parser.add_argument("-si", "--percentage_of_same_ips", required=False)

    argument = parser.parse_args()

    if argument.ip:
        ip = argument.ip

    if argument.tls:
        tls = str2bool(argument.tls)

    if argument.port:
        port = int(argument.port)

    if argument.warmup:
        warmup = int(argument.warmup)

    if argument.duration:
        duration = int(argument.duration)

    if argument.percentage_of_same_ips:
        percentage_of_same_ips = int(argument.percentage_of_same_ips)

    if not argument.mode or argument.mode == "latency":
        run_latency(warmup, ip, port, duration, tls)
    elif argument.mode == "flowmod":
        run_flowmodgenerator(ip, port, duration, tls, percentage_of_same_ips)
    elif argument.mode == "packetin":
        run_packetinechogenerator(ip, port, duration, tls)
