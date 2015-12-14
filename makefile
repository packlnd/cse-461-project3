all: server

server: server.cpp
	g++ -std=c++11 -Wall -pedantic -o server ConnectionInfo.cpp server.cpp
