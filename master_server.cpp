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
typedef std::vector<std::pair<Connection, int>> RelayList;

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

void pair_up(int i1, int i2) {
    Connection c1 = clients[i1];
    Connection c2 = clients[i2];
    std::cout << c1.get()->get_address_string() <<
        " <---> " << c2.get()->get_address_string() << std::endl;
    //TODO: Send relay info.
    //send_to_connection(c1, c2.get()->get_address_string());
    //send_to_connection(c2, "$");//c1.get()->get_address_string());
    clients.erase(clients.begin()+std::max(i1,i2));
    clients.erase(clients.begin()+std::min(i1,i2));
}

void naive_matching_algorithm() {
    std::cout << "Matching clients" << std::endl;
    pair_up(0, 1);
}

void listen_for_clients(int sd) {
    while (1) {
        std::cout << "Number of clients: " << clients.size() << std::endl;
        clients.push_back(accept_connection(sd));
        if (can_match()) naive_matching_algorithm();
    }
}

void update_count_from_relay(std::pair<Connection, int> &r) {
    const int size = 1024;
    char response[size];
    read(r.first.get()->get_sd(), response, size);
    r.second = atoi(response);
}

void poll_relays() {
    if (fork() != 0) return;
    while (true) {
        for (auto r : relay_servers) {
            send_to_connection(r.first, "$$");
            update_count_from_relay(r);
        }
    }
}

void listen_for_relays(int sd) {
    if (fork() != 0) return;
    while (1) {
        std::cout << "Number of relays: " << relay_servers.size() << std::endl;
        relay_servers.push_back({accept_connection(sd), 0});
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
    listen_for_relays(create_socket(1234));
    poll_relays();
    listen_for_clients(create_socket(1235));
    return 0;
}