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

#include <string>

class ConnectionInfo {
    public:
        ConnectionInfo();
        ConnectionInfo(int);
        ~ConnectionInfo();
        socklen_t *address_length();
        struct sockaddr *address();
        void set_sd(int cd);
        int get_sd() const;
        std::string get_ip() const;
        int get_port() const;
        std::string get_address_string() const;
     private:
        int sd;
        struct sockaddr_in addr;
        socklen_t addr_len;
};
