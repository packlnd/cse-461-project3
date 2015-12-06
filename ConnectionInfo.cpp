#include "ConnectionInfo.hpp"

ConnectionInfo::ConnectionInfo() {}

ConnectionInfo::~ConnectionInfo() {}

ConnectionInfo::ConnectionInfo(int pnr) {
    memset((char *)&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(pnr);
    addr.sin_addr.s_addr = INADDR_ANY;
}

struct sockaddr *ConnectionInfo::address() {
    return (struct sockaddr *)&addr;
}

socklen_t *ConnectionInfo::address_length() {
    addr_len = sizeof(addr);
    return &addr_len;
}

void ConnectionInfo::set_sd(int cd) {
    sd = cd;
}

int ConnectionInfo::get_sd() const {
    return sd;
}

std::string ConnectionInfo::get_ip() const {
    char *c = inet_ntoa(addr.sin_addr);
    std::string s(c);
    return s;
}

int ConnectionInfo::get_port() const {
    return ntohs(addr.sin_port);
}

std::string ConnectionInfo::get_address_string() const {
    return get_ip() + ":" + std::to_string(get_port());
}
