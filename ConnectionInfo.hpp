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

class ConnectionInfo {
    public:
        ConnectionInfo();
        ConnectionInfo(int);
        ~ConnectionInfo();
        socklen_t *address_length();
        struct sockaddr *address();
        void set_sd(int cd);
    private:
        int sd;
        struct sockaddr_in addr;
        socklen_t addr_len;
};
