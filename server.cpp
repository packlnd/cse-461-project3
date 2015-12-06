#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include <memory>

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

#define PORT 1234

typedef std::shared_ptr<ConnectionInfo> Connection;

std::vector<Connection> clients;

void handle_error(std::string msg) {
    std::cerr << msg << std::endl;
    exit(1);
}

void accept_client(int sd) {
    Connection c(new ConnectionInfo());
    int c_sd = accept(sd, c->address(), c->address_length());
    c->set_sd(c_sd);
    clients.push_back(c);
}

bool can_match() {
    return clients.size() >= 2;
}

void send_to_client(Connection c, std::string s) {
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
    send_to_client(c1, c2.get()->get_address_string());
    send_to_client(c2, c1.get()->get_address_string());
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
        accept_client(sd);
        if (can_match())
            naive_matching_algorithm();
    }
}

int create_socket() {
    ConnectionInfo port(PORT);
    int sd = socket(AF_INET, SOCK_STREAM, 0);
    if (sd < 0)
        handle_error("Couldn't create socket");
    if (bind(sd, port.address(), *port.address_length()) < 0)
        handle_error("Couldn't bind socket");
    std::cout << "Listening for connections on port " << PORT << std::endl;
    if (listen(sd, 0) != 0)
        handle_error("Listen");
    return sd;
}

int main(int argc, char **argv) {
    int sd = create_socket();
    listen_for_clients(sd);
    return 0;
}
