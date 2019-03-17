Author:
Full name   : Ishan Guliani
Pen name    : ishanguliani
Email       : ig5859@rit.edu

Project Description:
Implementation of RIPv2 protocol from RFC 2453

JAVA Files:
1.  RoverManager
2.  RouterProcess
3.  RIPPacket
4.  RoutingTableEntry
5.  MyThreadPoolExecutorService
6.  Log
7.  Helper

How to execute -
OPTION 1: If you have the 'make' program installed in your machine. Here's how you can fire up a rover.
        BASE STEP. $ make nasa      # this will compile the project
        Step 1. $ make rover multicast_ip=224.0.0.9 id=1 port=4445      # this will fire up a new rover
        Step 2. $ make rover multicast_ip=224.0.0.9 id=2 port=4445      # this will fire up another rover in a new shell/vm
        Step N. $ ..............                                        # fire upto N (10) rovers
        NOTE:   -
                -   There should be no spaces on either side of '='
                -   The id has to be unique for each rover process

OPTION 2: If you do not have the 'make' program installed -
        Step 1. $ rm -rf classes ||:	    # make sure you are in the project root directory. Remove the classes directory.
        Step 2. $ mkdir -p classes	        # create a clean 'classes' directory
        Step 3. $ javac -d classes RouterProcess.java	                # compile the program and copy all .class files to 'class' directory
        Step 4. $ cd classes && java RouterProcess 224.0.0.9 1 4445 	# change directory to 'classes' and run the program
        Step 5. $ cd ..                     # come back to the project root directory for restarting program. Go to Step 1.
        NOTE:   -   The rover id must be unique for each new router

