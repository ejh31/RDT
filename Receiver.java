
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * //TODO Receiver.java...does stuff
 *
 * @author Ricky Gal & Emily Hightower
 * @date 8/9/22
 * @info Course COP5518
 */
public class Receiver {

    private static final int BUFFER_SIZE = 256;
    private static final int IP_BYTES = 16;
    private static final int PORT_BYTES = 6;
    private DatagramSocket _socket; // the socket for communication with clients
    private final int      _port;   // the port number for communication with this server
    private boolean        _continueService;

    // packet components
    private String sourceIP;
    private int sourcePort;
    private String destIP;
    private int destPort;
    private String segment;

    // transport-layer header for segment
    private int sequenceNumber; // sequence number, range 0 - 9
    private int ack;            // acknowledgement; 0 if un-ack'd, 1 if ack'd
    private int checksum;       // simulated checksum; 0 if corrupt packet, 1 if good packet
    private String payload;     // message from sender to receiver

    /**
     * Constructs a Receiver object.
     */
    public Receiver(int port) {
        _port = port;
    }

    /**
     * Creates a datagram socket and binds it to a free port.
     *
     * @return - 0 or a negative number describing an error code if the connection could not be established
     */
    public int createSocket() {
        try {
            _socket = new DatagramSocket(_port);
            System.out.println("New Receiver socket created at port " + _socket.getLocalPort());
        } catch (SocketException ex) {
            System.err.println("unable to create and bind socket");
            return -1;
        }

        return 0;
    }

    public void run()
    {
        // run server until gracefully shut down
        _continueService = true;

        while (_continueService) {
            DatagramPacket newDatagramPacket = receiveRequest();

            String packet = new String (newDatagramPacket.getData()).trim();

            System.out.println ("sender IP: " + newDatagramPacket.getAddress().getHostAddress());
            System.out.println ("raw data: " + packet);
            System.out.println("parsing message from sender...");

            // pull the parameters out of the packet
            parsePacket(packet);

            // pull the segment parameters out of the segment
            parseSegment(getSegment());




            if (packet.equals("<shutdown/>")) {
                _continueService = false;
            }

            if (packet != null) {

                // send acknowledgement instead of the segment
                setSegment("ACK");

                // craft a new packet to send to the receiver
                String newPacket = createNewPacket();

                sendResponse(
                        newPacket,
                        newDatagramPacket.getAddress().getHostName(),
                        newDatagramPacket.getPort());
            }
            else {
                System.err.println ("incorrect response from server");
            }
        }
    }

    public String createNewPacket() {
        // update segment to acknowledge, and remove payload since it's not needed
        setAck(1);
        setSegment(String.valueOf(getSequenceNumber()) + getAck() + getChecksum());

        // create packet with concatenation
        String newPacket;
        newPacket = expandIP(getDestIP()) + getStringPort(getDestPort()) +
                expandIP(getSourceIP()) + getStringPort(getSourcePort()) + getSegment();

        return newPacket;
    }

    public void parsePacket(String packet) {
        setSourceIP(compressIP(packet.substring(0,16)));
        System.out.println("sourceIP = " + getSourceIP());

        setSourcePort(getIntPort(packet.substring(16,22)));
        System.out.println("sourcePort = " + getSourcePort());

        setDestIP(compressIP(packet.substring(22,38)));
        System.out.println("destIP = " + getDestIP());

        setDestPort(getIntPort(packet.substring(38, 44)));
        System.out.println("destPort = " + getDestPort());

        setSegment(packet.substring(44));
        System.out.println("segment = " + getSegment());
    }

    public void parseSegment(String segment) {
        setSequenceNumber(Integer.parseInt(segment.substring(0,1)));
        System.out.println("sequence number = " + getSequenceNumber());

        setAck(Integer.parseInt(segment.substring(1,2)));
        System.out.println("ack = " + getAck());

        setChecksum(Integer.parseInt(segment.substring(2,3)));
        System.out.println("checksum = " + getChecksum());

        setPayload(segment.substring(3));
        System.out.println("payload = " + getPayload());
    }
    /**
     * Sends a request for service to the server. Do not wait for a reply in this function. This will be
     * an asynchronous call to the server.
     *
     * @param response - the response to be sent
     * @param hostAddr - the ip or hostname of the server
     * @param port - the port number of the server
     *
     * @return - 0, if no error; otherwise, a negative number indicating the error
     */
    public int sendResponse(String response, String hostAddr, int port) {
        DatagramPacket newDatagramPacket = createDatagramPacket(response, hostAddr, port);
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
     * Receives a client's request.
     *
     * @return - the datagram containing the client's request or NULL if an error occured
     */
    public DatagramPacket receiveRequest() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
        try {
            _socket.receive(newDatagramPacket);
        } catch (IOException ex) {
            System.err.println("unable to receive message from server");
            return null;
        }

        return newDatagramPacket;
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
        Receiver server;
        String    serverName;
        String    req;

        if (args.length != 1) {
            System.err.println("Usage: java Receiver <receiver port>\n");
            return;
        }

        int portNum;
        try {
            portNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException xcp) {
            System.err.println("Usage: java Receiver <receiver port>\n");
            return;
        }

        // construct client and client socket
        server = new Receiver(portNum);
        if (server.createSocket() < 0) {
            return;
        }

        server.run();
        server.closeSocket();
        System.out.println("Socket closed.");
    }

    /**
     * Creates a datagram from the specified request and destination host and port information.
     *
     * @param request - the request to be submitted to the server
     * @param hostname - the hostname of the host receiving this datagram
     * @param port - the port number of the host receiving this datagram
     *
     * @return a complete datagram or null if an error occurred creating the datagram
     */
    private DatagramPacket createDatagramPacket(String request, String hostname, int port)
    {
        byte[] buffer = new byte[BUFFER_SIZE];

        // empty message into buffer
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = '\0';
        }

        // copy message into buffer
        byte[] data = request.getBytes();
        System.arraycopy(data, 0, buffer, 0, Math.min(data.length, buffer.length));

        InetAddress hostAddr;
        try {
            hostAddr = InetAddress.getByName(hostname);
        } catch (UnknownHostException ex) {
            System.err.println ("invalid host address");
            return null;
        }

        return new DatagramPacket (buffer, BUFFER_SIZE, hostAddr, port);
    }


    /**
     * Convert a 16-byte "long" IP address with trailing x to a normal
     * IP address that can be used by the DatagramPacket.
     * @param longIP long IP with 16 characters and a trailing x
     * @return normal IP address
     */
    public String compressIP(String longIP) {
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
    public String expandIP(String shortIP) {

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
    public int getIntPort(String stringPort) {
        return Integer.parseInt(stringPort);
    }

    /**
     * Convert a normal integer port number to a 6-byte "long" String port name
     * with leading zeroes that can be used in the packet header.
     * @param intPort normal port number
     * @return 6-byte "long" String port name
     */
    public String getStringPort(int intPort){
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


    // accessors and mutators

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getDestIP() {
        return destIP;
    }

    public void setDestIP(String destIP) {
        this.destIP = destIP;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getAck() {
        return ack;
    }

    public void setAck(int ack) {
        this.ack = ack;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
