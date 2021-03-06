#include <iostream>
#include <string>
#include <vector>
#include <memory>
#include <thread>
#include <map>
#include <mutex>
#include <atomic>

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

#define MASTER_PORT 11234
#define CLIENT_PORT 11236

// TOKEN -> SOCKET FILE DESCRIPTORS
std::map<int,std::vector<int>> client_pairs;

std::mutex pairs_mutex;
std::mutex cout_mutex;

std::atomic<int> client_count;

void handle_error(std::string msg) {
    std::cerr << msg << std::endl;
    exit(1);
}

void handle_client(int client_sd) {
  struct pollfd fds[1];
  fds[0].fd = client_sd;
  fds[0].events = POLLIN;
  int token_buf;
  char buf[25000];
  int bytes_read;
  int token, partner;

  memset(&token_buf, 0, sizeof(token_buf));
  memset(&buf, 0, sizeof(buf));

  // get the token from client
  bytes_read = read(client_sd, &token_buf, sizeof(token_buf));
  //token = ntohl(*((int*)token_buf));
  token = ntohl(token_buf);
  
  pairs_mutex.lock();
  client_pairs[token].push_back(client_sd);
  pairs_mutex.unlock();

  while (client_pairs[token].size() != 2) { }
  
  client_count++;
  for (auto it = client_pairs[token].begin(); it != client_pairs[token].end(); it++)
    if (*it != client_sd) partner = *it;

  cout_mutex.lock();
  std::cout<<client_sd<<" matched with "<<partner<<" with token "<<token<<std::endl;
  cout_mutex.unlock();
  
  do {
    // if the partner disconnects, end this session
    if ( client_pairs[token].size() != 2) break;
    
    bytes_read = read(client_sd, &buf, sizeof(buf));
    if (bytes_read > 0) {
      // std::cout<<"Read "<<bytes_read<<" bytes from client "<<client_sd<<", size = "<<ntohl(size_buf)<<std::endl;
      // process bytes
      // std::cout<<"Writing "<<bytes_read<<" bytes to Client "<<partner<<std::endl;
      // write to opposite end
      write(partner, buf, bytes_read);
      
    }
  } while (poll(fds, 1, 3000) > 0 && bytes_read != 0);

  pairs_mutex.lock();
  client_pairs[token].pop_back();
  pairs_mutex.unlock();
  
  client_count--;
  close(client_sd);
  std::cout<<"Disconnecting from client socket "<<client_sd<<std::endl;
}

void connect_to_master() {
  char buf[8];
  int master_sd = socket(AF_INET, SOCK_STREAM, 0);
  // connect to master
  struct sockaddr_in *master_addr;
  memset(master_addr, 0, sizeof(*master_addr));
  master_addr->sin_family=AF_INET;
  struct hostent *master_server;
  master_server=gethostbyname("attu1.cs.washington.edu");
  if (master_server == NULL) {
    perror("gethostbyname");
    exit(1);
  }
  memcpy(&(master_addr->sin_addr), master_server->h_addr_list[0], master_server->h_length);
  master_addr->sin_port=htons(MASTER_PORT);

  if (connect(master_sd, (struct sockaddr*)master_addr, sizeof(*master_addr)) < 0) {
    handle_error("ERROR connecting to master");
  }

  struct pollfd fds[1];
  fds[0].fd = master_sd;
  fds[0].events = POLLIN;

  std::cout << "Connected to Master server."<<std::endl;
  // connected. wait for info request
  do {
    if (read(master_sd, &buf, sizeof(buf)) > 0) {
      // check for request value
      cout_mutex.lock();
      std::cout << "Master requested traffic report. active pairs = " <<client_count/2<<std::endl;
      cout_mutex.unlock();
      int count_buf;
      count_buf = client_count/2;
      write(master_sd, &count_buf, sizeof(count_buf));
    }
  } while (poll(fds, 1, 60000) > 0);

  close(master_sd);
  std::cout<<"Disconnecting from Master"<<std::endl;
}

void listen_for_clients(int sd) {
  for (;;) {
    int client_sd = accept(sd, NULL, NULL);
    std::cout<<"New client connection on socket "<<client_sd<<std::endl;
    std::thread client_thr(handle_client, client_sd);
    client_thr.detach();
  }
}

int create_socket(ConnectionInfo * socket_info) {
    int sd = socket(AF_INET, SOCK_STREAM, 0);
    if (sd < 0)
        handle_error("Couldn't create socket");
    if (bind(sd, socket_info->address(), *(socket_info->address_length())) < 0)
        handle_error("Couldn't bind socket");
    std::cout << "Listening for connections on port " << CLIENT_PORT << std::endl;
    if (listen(sd, 0) != 0)
        handle_error("Listen");
    return sd;
}

int main(int argc, char **argv) {
  // connect to master
  std::thread master_conn_thread(connect_to_master);
  master_conn_thread.detach();

  // listen for new connections on client port
  ConnectionInfo listening_socket(CLIENT_PORT);
  int listening_sd = create_socket(&listening_socket);
  listen_for_clients(listening_sd);
  return 0;
}
