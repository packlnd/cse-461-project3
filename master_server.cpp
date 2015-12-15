#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include <memory>
#include <functional>
#include <queue>

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

typedef std::shared_ptr<ConnectionInfo> Connection;
typedef std::vector<std::pair<Connection, int>*> RelayList;

std::vector<Connection> clients;
RelayList relay_servers;
long pair_number = 0;

void handle_error(std::string msg) {
    std::cerr << msg << std::endl;
    exit(1);
}

Connection accept_connection(int sd) {
    Connection c(new ConnectionInfo());
    int c_sd = accept(sd, c->address(), c->address_length());
    c->set_sd(c_sd);
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
    std::cout << "Address: " << &relay_servers << std::endl;
    std::cout << "Size: " << relay_servers.size() << std::endl;
    for (unsigned int i=0; i<relay_servers.size(); ++i) {
        std::cout << "Crash next line" << std::endl;
        if (relay_servers[i]->second < lowestCount) {
            lowestCount = relay_servers[i]->second;
            bestIndex=i;
        }
        std::cout << "Didnt crash previous line" << std::endl;
    }
    ++relay_servers[bestIndex]->second;
    std::cout << "Print best relay" << std::endl;
    std::cout << "Best relay is index " << bestIndex << ": " << relay_servers[bestIndex]->first.get()->get_ip() << std::endl;
    return relay_servers[bestIndex]->first;
}

void pair_up(int i1, int i2) {
    Connection c1 = clients[i1];
    Connection c2 = clients[i2];
    Connection c = get_min_relay();
    std::string msg = c.get()->get_ip() + "$" + std::to_string(pair_number++);
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
    //while (1) {
        std::cout << "Number of clients: " << clients.size() << std::endl;
        clients.push_back(accept_connection(sd));
        if (can_match()) naive_matching_algorithm();
    //}
}

void update_count_from_relay(std::pair<Connection, int> *r) {
    const int size = 1024;
    char response[size];
    read(r->first.get()->get_sd(), response, size);
    std::cout << "count for " << r->first.get()->get_address_string() <<
        " " << r->second << " --> " << atoi(response) << std::endl;
    r->second = atoi(response);
}

void poll_relays() {
    //if (fork() != 0) return;
    //while (true) {
        for (auto r : relay_servers) {
            send_to_connection(r->first, "1");
            update_count_from_relay(r);
        }
    //}
}

void listen_for_relays(int sd) {
    //if (fork() != 0) return;
    //while (1) {
        std::cout << "Number of relays: " << relay_servers.size() << std::endl;
        std::pair<Connection, int> *p = new std::pair<Connection, int>(accept_connection(sd), 0);
        relay_servers.push_back(p);
    //}
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
    listen_for_relays(create_socket(1234));
    while (true) {
        poll_relays();
        listen_for_clients(create_socket(1235));
    }
    return 0;
}
