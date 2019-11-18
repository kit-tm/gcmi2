from threading import Thread
import socket
import ssl
from MessageGenerator import MessageGenerator
import time
import select
import itertools

TIMEOUT_S = 3

class FlowModGenerator(Thread):

    def __init__(self, upstream_ip, upstream_port, enable_tls=False, percentage_of_same_ips=0):
        Thread.__init__(self)
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.messageGenerator = MessageGenerator()
        self.upstream_ip = upstream_ip
        self.upstream_port = upstream_port
        self.should_stop = False
        self.enable_tls = enable_tls
        self.current_xid = 1
        self.percentage_of_same_ips = percentage_of_same_ips
        self.last_sent_messages = []

        self.matching_ips = []
        self.non_matching_ips = []

        for ip in itertools.product(range(231), range(3), range(256)):
            self.matching_ips.append('{}.{}.0.{}'.format(ip[0], ip[1], ip[2]))

        for ip in itertools.product(range(250, 256), range(63, 256), range(256)):
            self.non_matching_ips.append("{}.{}.{}.0".format(ip[0], ip[1], ip[2]))

        self.same_matching_ip = self.matching_ips[100]
        self.same_non_matching_ip = self.non_matching_ips[100]

        try:
            self.socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        except (OSError, NameError):
            pass

    def recent_same_message_percentage(self):
        if len(self.last_sent_messages) == 0: return 0
        number_of_same_messages = self.last_sent_messages.count(True)
        return number_of_same_messages * 100 / len(self.last_sent_messages)

    def get_next_ip(self, matching):
        next_message_is_same = True

        if self.recent_same_message_percentage() >= self.percentage_of_same_ips or self.percentage_of_same_ips == 0:
            next_message_is_same = False

        if self.percentage_of_same_ips == 100:
            next_message_is_same = True

        self.last_sent_messages.append(next_message_is_same)
        if len(self.last_sent_messages) > 100: del self.last_sent_messages[0]

        if next_message_is_same:
            if matching:
                return self.same_matching_ip
            else:
                return self.same_non_matching_ip
        else:
            if matching:
                return self.matching_ips.pop(0)
            else:
                return self.non_matching_ips.pop(0)

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
                src_ip = self.get_next_ip(self.current_xid % 2 == 0)
                message = self.messageGenerator.generate_flow_mod_message(xid=self.current_xid, srcIpAddress=src_ip)
                socket.sendall(message)
                self.current_xid += 1

            if self.should_stop: break
            time.sleep(0.01)

        socket.close()

    def stop(self):
        self.should_stop = True