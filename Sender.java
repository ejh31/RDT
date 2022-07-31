//TODO: headers, comments, accessors/mutators, resolve all warnings on all classes
//TODO: implement other parts of the project
//TODO: IP address needs to be in this format: 127.000.000.001

import java.io.IOException;
import java.net.*;

/**
 * //TODO Sender.java...does stuff
 *
 * @author Ricky Gal & Emily Hightower
 * @date 8/9/22
 * @info Course COP5518
 */
public class Sender {

    private static final int BUFFER_SIZE = 256;
    private static final int IP_BYTES = 16;
    private static final int PORT_BYTES = 6;
    private static final int END_OF_HEADER = 44;
    private DatagramSocket _socket; // the socket for communication with a server
    private final int TIMEOUT = 100; // socket timeout in milliseconds
    /**
     * Constructs a Sender object.
     */
    public Sender() {
    }

    /**
     * Creates a datagram socket and binds it to a free port.
     *
     * @return - 0 or a negative number describing an error code if the connection could not be established
     * @param portNum
     */
    public int createSocket(int portNum) {
        try {
            _socket = new DatagramSocket(portNum);
            System.out.println("New Client socket created at port " + _socket.getLocalPort());
        } catch (SocketException ex) {
            System.err.println("unable to create and bind socket");
            return -1;
        }

        return 0;
    }

    /**
     * Sends a request for service to the server. Do not wait for a reply in this function. This will be
     * an asynchronous call to the server.
     *

     *
     * @return - 0, if no error; otherwise, a negative number indicating the error
     */
    public int sendRequest(String sourceIP, int sourcePort, String networkIP, int networkPort, String receiverIP, int receiverPort, String segment) {
        DatagramPacket newDatagramPacket = createDatagramPacket(sourceIP, sourcePort, networkIP, networkPort, receiverIP, receiverPort, segment);
        if (newDatagramPacket != null) {
            try {
                _socket.send(newDatagramPacket);
            } catch (IOException ex) {
                System.err.println("unable to send message to server");
                return -1;
            }

            return 0;
        }

        System.err.println("unable to create message");
        return -1;
    }

    /**
     * Receives the server's response following a previously sent request.
     *
     * @return - the server's response or NULL if an error occurred
     */
    public String receiveResponse() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
        try {
            // set timeout and try to receive a packet
            _socket.setSoTimeout(TIMEOUT);
            _socket.receive(newDatagramPacket);
        }
        catch (SocketTimeoutException ex) {
            System.err.println("Timeout reached.");
            return null;
        }
        catch (IOException ex) {
            System.err.println("unable to receive message from server");
            return null;
        }

        return new String(buffer).trim();

    }

    /*
     * Prints the response to the screen in a formatted way.
     *
     * response - the server's response as an XML formatted string
     *
     */
    public static void printResponse(String response) {

        System.out.println("FROM SERVER: " + response);
    }


    /*
     * Closes an open socket.
     *
     * @return - 0, if no error; otherwise, a negative number indicating the error
     */
    public int closeSocket() {
        _socket.close();

        return 0;
    }

    /**
     * The main function. Use this function for
     * testing your code. We will provide a new main function on the day of the lab demo.
     */
    public static void main(String[] args)
    {
        Sender client;
        String sourceIP = "127.0.0.1";
        int portNum;
        String receiverIP;
        int receiverPort;
        String networkIP;
        int networkPort;
        String segment;
        boolean continueService = true;

        // transport-layer header for segment
        int sequenceNumber = 0; // sequence number, incremented each message; 0 - 9
        int ack = 0;            // un-acknowledged by default
        int checksum = 1;       // checksum is correct by default

        int NUM_ARGS = 5;
        int state = 0; // state of the Sender state machine; we start at 0

        if (args.length != NUM_ARGS) {
            System.err.println("Usage: java Sender <sender port> <receiver IP> <receiver port> <network IP> <network port>\n");
            return;
        }

        // ARG 0: portNum
        try {
            portNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException xcp) {
            System.err.println("Usage: java Sender <sender port> <receiver IP> <receiver port> <network IP> <network port>\n");
            return;
        }

        // ARG 1: receiver IP
        try {
            receiverIP = args[1];
        } catch (NullPointerException xcp) {
            System.err.println("Usage: java Sender <sender port> <receiver IP> <receiver port> <network IP> <network port>\n");
            return;
        }

        // ARG 2: receiver port
        try {
            receiverPort = Integer.parseInt(args[2]);

        } catch (NumberFormatException xcp) {
            System.err.println("Usage: java Sender <sender port> <receiver IP> <receiver port> <network IP> <network port>\n");
            return;
        }

        // ARG 3: network IP
        try {
            networkIP = args[3];
        } catch (NullPointerException xcp) {
            System.err.println("Usage: java Sender <sender port> <receiver IP> <receiver port> <network IP> <network port>\n");
            return;
        }

        // ARG 4: network port
        try {
            networkPort = Integer.parseInt(args[4]);

        } catch (NumberFormatException xcp) {
            System.err.println("Usage: java Sender <sender port> <receiver IP> <receiver port> <network IP> <network port>\n");
            return;
        }

        // construct client and client socket
        client = new Sender();
        if (client.createSocket(portNum) < 0) {
            return;
        }

        // keep looping until "quit" or "exit" is typed by the user
        while (continueService) {
            // switch cases for the state machine; refer to README for more detail about states
            switch(state) {
                case 0:

                    System.out.print("Enter a request: ");
                    segment = System.console().readLine();

                    // quit if requested
                    if (segment.equalsIgnoreCase("quit") || segment.equalsIgnoreCase("exit")) {
                        continueService = false;
                        break;
                    }

                    // add transport layer header to segment
                    segment = String.valueOf(sequenceNumber) + ack + checksum + segment;

                    // increment sequence number for the next segment
                    sequenceNumber = incrementSequenceNumber(sequenceNumber);

                    if (client.sendRequest(sourceIP, portNum, receiverIP, receiverPort, networkIP, networkPort, segment) < 0) {
                        client.closeSocket();
                        System.out.println("Socket was closed.");
                        return;
                    }

                    String response = client.receiveResponse();
                    if (response != null) {
                        // parse the packet to get the response
                        response = response.substring(END_OF_HEADER);

                        Sender.printResponse(response);
                    } else {
                        System.err.println("Error: Bad or no response from server.");
                    }
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:

            }
        }
        client.closeSocket();
        System.out.println("Socket closed.");

    }

    /**
     * increments sequence number, which ranges from 0 to 9
     * @param sequenceNumber
     * @return
     */
    private static int incrementSequenceNumber(int sequenceNumber) {
        if (sequenceNumber == 9) {
            sequenceNumber = 0;
        }
        else {
            sequenceNumber++;
        }
        return sequenceNumber;
    }

    /**
     * Creates a datagram from the specified request and destination host and port information.
     *
     * @param segment - the request to be submitted to the server
     * @param destIP - the hostname of the host receiving this datagram
     * @param destPort - the port number of the host receiving this datagram
     *
     * @return a complete datagram or null if an error occurred creating the datagram
     */
    private DatagramPacket createDatagramPacket(String sourceIP, int sourcePort, String networkIP, int networkPort, String destIP, int destPort, String segment) {
        byte[] buffer = new byte[BUFFER_SIZE];

        // empty message into buffer
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = '\0';
        }

        // create packet with concatenation
        String packetContents;
        packetContents = expandIP(sourceIP) + getStringPort(sourcePort) + expandIP(networkIP) + getStringPort(networkPort) + segment;

        // copy transport layer segment into buffer
        byte[] packet = packetContents.getBytes();
        System.arraycopy(packet, 0, buffer, 0, Math.min(packet.length, buffer.length));

        InetAddress hostAddr;
        try {
            hostAddr = InetAddress.getByName(destIP);
        } catch (UnknownHostException ex) {
            System.err.println("invalid host address");
            return null;
        }

        return new DatagramPacket(buffer, BUFFER_SIZE, hostAddr, destPort);
    }

    /**
     * Convert a 16-byte "long" IP address with trailing x to a normal
     * IP address that can be used by the DatagramPacket.
     * @param longIP long IP with 16 characters and a trailing x
     * @return normal IP address
     */
    public static String compressIP(String longIP) {
        // make sure this is a proper 16-byte "long" IP address
        if (longIP.length() != IP_BYTES) {
            System.err.println("Error: Long IP is not properly formatted.");
            return null;
        }
        else {
            // remove the trailing X
            longIP = longIP.substring(0, 15);

            // split IP address into an array
            String[] arr = longIP.split("\\.");

            // in each section, remove leading zeroes
            for (int i = 0; i < 4; i++) {
                while (arr[i].charAt(0) == '0' && arr[i].length() > 1){
                    arr[i] = arr[i].substring(1);
                }
            }
            // reconstruct and return the compressed IP
            return arr[0] + "." + arr[1] + "." + arr[2] + "." + arr[3];
        }
    }

    /**
     * Convert a normal IP address into the "proper" 16-byte IP address that will
     * be sent as part of the header.
     * @param shortIP normal IP address
     * @return long IP with 16 characters and a trailing x
     */
    public static String expandIP(String shortIP) {

        // if the IP is already 16 characters long, nothing to expand
        if (shortIP.length() == IP_BYTES) {
            return shortIP;
        }
        // else, make sure each section has 3 characters
        else {
            // split IP address into an array
            String[] arr = shortIP.split("\\.");

            // add zeroes to the front of each section until it is 3 digits long
            for (int i = 0; i < 4; i++) {
                while (arr[i].length() < 3){
                    arr[i] = 0 + arr[i];
                }
            }

            // put the IP address back together and add an X to the end
            return arr[0] + "." + arr[1] + "." + arr[2] + "." + arr[3] + "x";
        }
    }

    /**
     * Convert a 6-byte "long" port name with leading zeroes to a normal integer
     * name that can be used by the DatagramPacket.
     * @param stringPort long String port with leading zeroes
     * @return integer port number
     */
    public static int getIntPort(String stringPort) {
        return Integer.parseInt(stringPort);
    }

    /**
     * Convert a normal integer port number to a 6-byte "long" String port name
     * with leading zeroes that can be used in the packet header.
     * @param intPort normal port number
     * @return 6-byte "long" String port name
     */
    public static String getStringPort(int intPort){
        // convert the integer port to a String
        String port = Integer.toString(intPort);

        // if the String is already 6 bytes, nothing to do
        if (port.length() == PORT_BYTES) {
            return port;
        }
        else {
            // add leading zeroes until it's 6 bytes/characters
            while (port.length() < PORT_BYTES) {
                port = "0" + port;
            }
            return port;
        }
    }

}
