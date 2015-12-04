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

std::vector<std::shared_ptr<ConnectionInfo>> clients;

void handle_error(std::string msg) {
    std::cerr << msg << std::endl;
    exit(1);
}

void accept_client(int sd) {
    std::shared_ptr<ConnectionInfo> c(new ConnectionInfo());
    int c_sd = accept(sd, c->address(), c->address_length());
    c->set_sd(c_sd);
    clients.push_back(c);
}

void listen_for_clients(int sd) {
    while (1) {
        accept_client(sd);
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
