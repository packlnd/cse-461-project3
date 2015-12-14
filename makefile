all: server

server: master_server.cpp
	g++ -std=c++11 -Wall -pedantic -o server ConnectionInfo.cpp master_server.cpp
