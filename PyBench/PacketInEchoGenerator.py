from threading import Thread
import socket
import ssl
from MessageGenerator import MessageGenerator
import time
import select
import random

TIMEOUT_S = 3

class PacketInEchoGenerator(Thread):

    def __init__(self, upstream_ip, upstream_port, enable_tls=False):
        Thread.__init__(self)
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.messageGenerator = MessageGenerator()
        self.upstream_ip = upstream_ip
        self.upstream_port = upstream_port
        self.should_stop = False
        self.enable_tls = enable_tls
        self.current_xid = 1
        try:
            self.socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        except (OSError, NameError):
            pass

    def run(self):
        if self.enable_tls:
            context = ssl.SSLContext(ssl.PROTOCOL_TLS)
            context.check_hostname = False
            socket = context.wrap_socket(self.socket, server_hostname=self.upstream_ip)
        else:
            socket = self.socket

        try:
            socket.connect((self.upstream_ip, self.upstream_port))
            print("flowmodgenerator connected!")
        except socket.error as e:
            if e.errno != 115 and e.errno != 36:
                print("failed to connect: " + str(e))
                return

        socket.setblocking(False)

        time.sleep(1)

        while True:
            ready_to_read, ready_to_write, error = select.select([socket], [socket], [], TIMEOUT_S)
            if ready_to_write:
                message = self.messageGenerator.generate_benchmark_message(self.current_xid)

                socket.sendall(message)
                self.current_xid += 1

            if self.should_stop: break
            time.sleep(0.01)

        socket.close()

    def stop(self):
        self.should_stop = True
