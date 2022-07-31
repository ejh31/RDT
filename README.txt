//TODO: Add more information to this readme.

Steps to execute on a local machine:
1. Open all 3 project source files: Sender, Network, and Receiver
2. Use the Terminal tab in IntelliJ to run. Arguments (Sender must be set up last, use tabs to auto-complete the Class names):
	java Receiver.java 60002
	java Network.java 60001 100 100 100
	java Sender.java 60000 127.0.0.1 60002 127.0.0.1 60001
3. Type your message in the Sender. In the network tab, you should see 2 packets and their contents printed to the console (outbound and inbound). In the Receiver tab, you should see the message from the sender before the receiver made any changes to it. You will see that the receiver's response on the sender tab includes the ACK bit flipped (middle bit).

STATE INFORMATION FOR SENDER STATE SWITCH:
A more thorough graph can be found in the RDT slides from the professor's lecture.
State 0: Wait for call 0 from above.
State 1: Wait for call 1 from above.
State 2: Wait for ACK0.
State 3: Wait for ACK1.