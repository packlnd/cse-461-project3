#include "ConnectionInfo.hpp"

ConnectionInfo::ConnectionInfo() {
}

ConnectionInfo::~ConnectionInfo() {
}

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
