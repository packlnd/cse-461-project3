#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include <memory>
#include <functional>
#include <queue>
#include <thread>

#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <poll.h>
#include <arpa/inet.h>

#include "./ConnectionInfo.hpp"

uint32_t uw_ips[10] = {2161115136, 1602224128, 2391539712, 3325050880, 3450822656, 1163624448, 2918875136, 1823703040, 3427076096, 2887516160};
uint32_t masks[5] = {4294901760, 4294934528, 4294950912, 4294959104, 4294965248};

typedef std::shared_ptr<ConnectionInfo> Connection;
typedef std::vector<std::pair<Connection, int>*> RelayList;

std::vector<Connection> clients;
RelayList relay_servers;
int pair_number = 0;

void handle_error(std::string msg) {
    std::cerr << msg << std::endl;
    exit(1);
}

bool valid_uw_ip(Connection c) {
  bool valid = false;
  uint32_t ip = c->get_bip();
  std::cout<<c->get_ip()<<" --> "<<ip<<std::endl;
  for (auto uw_ip : uw_ips) {
    for (auto mask : masks) {
      if ((ip & mask) == uw_ip) {
	valid = true;
	std::cout<<"found matching UW ip"<<std::endl;
      }
    }
  }
  if (!valid) {
    std::cout<<"Client not from UW"<<std::endl;
  }
  return valid;
}

Connection accept_connection(int sd, bool client) {
    Connection c(new ConnectionInfo());
    int c_sd = accept(sd, c->address(), c->address_length());
    c->set_sd(c_sd);
    if (client) {
      if (valid_uw_ip(c)) {
	std::cout<<"Client "<<c<<" accepted"<<std::endl;
	clients.push_back(c);
      }
    }
    return c;
}

bool can_match() {
    return clients.size() >= 2;
}

void send_to_connection(Connection c, std::string s) {
    
    s.append("\n");
    const char *msg = s.c_str();
    int len = strlen(msg);
    write(c.get()->get_sd(), msg, len);
}

Connection get_min_relay() {
    int bestIndex=0;
    int lowestCount=0;
    for (unsigned int i=0; i<relay_servers.size(); ++i) {
        if (relay_servers[i]->second < lowestCount) {
            lowestCount = relay_servers[i]->second;
            bestIndex=i;
        }
    }
    ++relay_servers[bestIndex]->second;
    std::cout << "Best relay is index " << bestIndex << ": " << relay_servers[bestIndex]->first.get()->get_ip() << std::endl;
    return relay_servers[bestIndex]->first;
}

void pair_up(int i1, int i2) {
    Connection c1 = clients[i1];
    Connection c2 = clients[i2];
    Connection c = get_min_relay();
    std::string msg = c.get()->get_ip() + " " + std::to_string(pair_number++);
    std::cout << c1.get()->get_address_string() << " <-- " << msg << " --> " <<
        c2.get()->get_address_string() << std::endl;
    send_to_connection(c1, msg);
    send_to_connection(c2, msg);
    clients.erase(clients.begin()+std::max(i1,i2));
    clients.erase(clients.begin()+std::min(i1,i2));
}

void naive_matching_algorithm() {
    std::cout << "Matching clients" << std::endl;
    pair_up(0, 1);
}

void listen_for_clients(int sd) {
    while (true) {
        std::cout << "Number of clients: " << clients.size() << std::endl;
	accept_connection(sd, true);
        if (can_match()) naive_matching_algorithm();
    }
}

void update_count_from_relay(std::pair<Connection, int> *r) {
    int count = 13;
    
    recv(r->first.get()->get_sd(), &count, sizeof count, 0);
    std::cout << "Server: " << r->first.get()->get_address_string() <<
        ". Number of active pairs: " << count << std::endl;
    r->second = count;
}

void poll_relays() {
    for (auto r : relay_servers) {
        send_to_connection(r->first, "1");
        update_count_from_relay(r);
    }
}

void listen_for_relays(int sd) {
    while (1) {
        std::cout << "Number of relays: " << relay_servers.size() << std::endl;
        std::pair<Connection, int> *p = new std::pair<Connection, int>(accept_connection(sd, false), 0);
        relay_servers.push_back(p);
    }
}

int create_socket(int port_no) {
    ConnectionInfo port(port_no);
    int sd = socket(AF_INET, SOCK_STREAM, 0);
    if (sd < 0)
        handle_error("Couldn't create socket");
    if (bind(sd, port.address(), *port.address_length()) < 0)
        handle_error("Couldn't bind socket");
    std::cout << "Listening for connections on port " << port_no << std::endl;
    if (listen(sd, 0) != 0)
        handle_error("Listen");
    return sd;
}

int main(int argc, char **argv) {
    std::thread relay_thr(listen_for_relays, create_socket(11234));
    relay_thr.detach();
    std::thread client_thr(listen_for_clients, create_socket(11235));
    client_thr.detach();
    while (true) {
        poll_relays();
        sleep(30);
    }
    return 0;
}
