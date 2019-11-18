import threading
from threading import Thread
import socket
import ssl
from pyof.v0x01.common.utils import unpack_message
from pyof.v0x01.symmetric.hello import Hello
from pyof.v0x01.common.header import Type
from MessageGenerator import MessageGenerator
from datetime import datetime
import select
import time
from threading import Timer

BUFFER_SIZE = 1024
TIMEOUT_S = 3

class Switch(Thread):

    def __init__(self, dpid, warmup_time_s, upstream_ip, upstream_port, initial_xid, throughput_mode, enable_tls=False):
        Thread.__init__(self)
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.dpid = dpid
        self.currentXid = initial_xid
        self.should_stop = self.getCurrentMicroSeconds() + 5000 * 1000000
        self.results = []
        self.warmup_time_s = warmup_time_s
        self.upstream_ip = upstream_ip
        self.upstream_port = upstream_port
        self.messageGenerator = MessageGenerator()
        self.last_packetin_sent = self.getCurrentMicroSeconds()
        self.throughput_mode = throughput_mode
        self.enable_tls = enable_tls

        try:
            self.socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        except (OSError, NameError):
            pass

        self.messages_to_send = [(2, Hello().pack())]

    def on_message_received(self, data):
        receive_time = self.getCurrentMicroSeconds()
        try:
            msg = unpack_message(data)
        except:
            print("received invalid message!")
            return

        print("received " + str(msg.header.message_type) + " "  + str(msg.header.xid))

        if msg.header.message_type == Type.OFPT_PACKET_OUT or msg.header.message_type == Type.OFPT_ECHO_REPLY:
            if receive_time - self.start_time > self.warmup_time_s * 1000000 and receive_time < self.should_stop:
                self.results.append([receive_time, msg.header.xid])
            if not self.throughput_mode and msg.header.xid == self.currentXid: self.send_next()
        elif msg.header.message_type == Type.OFPT_FEATURES_REQUEST:
            self.messages_to_send.insert(0, (msg.header.xid, self.messageGenerator.generate_features_reply_message(msg.header.xid, self.dpid)))
        elif msg.header.message_type == Type.OFPT_SET_CONFIG:
            self.messages_to_send.insert(0, (msg.header.xid, self.messageGenerator.generate_config_reply_message(msg.header.xid, msg.flags, msg.miss_send_len)))
        elif msg.header.message_type == Type.OFPT_ECHO_REQUEST:
            self.messages_to_send.insert(0, (msg.header.xid, self.messageGenerator.generate_echo_reply_message(msg.header.xid)))
        elif msg.header.message_type == Type.OFPT_STATS_REQUEST:
            self.messages_to_send.insert(0, (msg.header.xid, self.messageGenerator.generate_stats_reply_message()))
        elif msg.header.message_type == Type.OFPT_VENDOR:
            self.messages_to_send.insert(0, (msg.header.xid, self.messageGenerator.generate_vendor_reply_message(msg.header.xid)))

        if msg.header.message_type != Type.OFPT_PACKET_OUT and msg.header.message_type != Type.OFPT_ECHO_REPLY:
            print("received " + str(msg.header.message_type) + " after " + str((self.getCurrentMicroSeconds() - self.start_time) / 1000000) + "s")

    def getCurrentMicroSeconds(self):
        return int(datetime.timestamp(datetime.now()) * 1000000)

    def send_next(self):
        # time to establish openflow channel
        if self.getCurrentMicroSeconds() - self.start_time < 5 * 1000000: return

        self.currentXid += 50
        self.messages_to_send.append((self.currentXid, self.messageGenerator.generate_benchmark_message(self.currentXid)))

    def run(self):

        if self.enable_tls:
            context = ssl.SSLContext(ssl.PROTOCOL_TLS)
            #context.load_cert_chain(certfile="/home/daniel/ma-daniel-geiger/floodlight/tls/client.crt",
            #                              keyfile="/home/daniel/ma-daniel-geiger/floodlight/tls/client.key")
            #context.options |= ssl.OP_NO_TLSv1 | ssl.OP_NO_TLSv1_1 | ssl.CERT_OPTIONAL
            context.check_hostname = False
            socket = context.wrap_socket(self.socket, server_hostname=self.upstream_ip)
        else:
            socket = self.socket

        try:
            socket.connect((self.upstream_ip, self.upstream_port))
            print(self.dpid + " connected!")
        except Exception as e:
            print("failed to connect: " + str(e))
            return

        socket.setblocking(False)

        time.sleep(1)

        self.start_time = self.getCurrentMicroSeconds()

        while True:
            ready_to_read, ready_to_write, error = select.select([socket], [socket], [], TIMEOUT_S)
            if ready_to_read:
                try:
                    data = socket.recv(BUFFER_SIZE)
                except ssl.SSLWantReadError:
                    print("SSLWantReadError")
                    continue
                self.on_message_received(data)
            elif ready_to_write:
                if len(self.messages_to_send) > 0:
                    xid, message_to_send = self.messages_to_send.pop(0)
                    if self.last_packetin_sent - self.start_time > self.warmup_time_s * 1000000 and self.last_packetin_sent < self.should_stop:
                        self.results.append([self.getCurrentMicroSeconds(), xid])
                    socket.sendall(message_to_send)
                    print("sent " + str(xid))
                    self.last_packetin_sent = self.getCurrentMicroSeconds()
                if self.throughput_mode:
                    threading.Thread(target=self.send_next).start()
            else:
                print("socket error!")

            if self.getCurrentMicroSeconds() - self.should_stop > 10000000: break

            if not self.throughput_mode and self.getCurrentMicroSeconds() - self.last_packetin_sent > 2000000:
                print("timeout!")
                self.send_next()

        socket.close()

    def get_results(self):
        return self.results

    def stop(self):
        self.should_stop = self.getCurrentMicroSeconds()

