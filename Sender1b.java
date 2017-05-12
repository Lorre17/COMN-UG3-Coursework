/**
 * Created by Lorena on 01/03/2017.
 */
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Sender1b {

    // the maximum packet size
    public static final int MAX_PACKET_SIZE = 1027;

    // the arguments
    private int port;
    private String fileName;
    private int retryTimeout;
    private InetAddress host;

    // the socket for sending / receiving
    private DatagramSocket socket = null;
    //private DatagramSocket serverSocket = null;

    private long startTime;
    private int retransmissions = 0;

    public Sender1b(String host, int port, String fileName, int retryTimeout) {
        this.port = port;
        this.fileName = fileName;
        this.retryTimeout = retryTimeout;
        try {
            this.host = InetAddress.getByName(host);
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    public static byte[] createHeader(byte[] data, int seqNum, int eof) {
        data[0] = (byte) ((seqNum >> 8) & 0xff);
        System.out.println(" Data[0] =  " + data[0]);
        data[1] = (byte) (seqNum & 0xff);
        System.out.println(" Data[1] =  " + data[1]);
        System.out.println(" **************************");
        data[2] = (byte) eof;
        return data;
    }

    // Alternate between 0 and 1
    public static short alternateNumber(short expectedNumber) {
        return (expectedNumber == 0) ? (short)1 : (short)0;
    }

    // Check ACK sequence number against expected sequence number
    public static boolean getAckStatus(byte[] data, short expectedNumber) {
        short ackSequenceNumber = ByteBuffer.wrap(data).getShort();
        if (ackSequenceNumber == expectedNumber){
            return true;
        }
        return false;
    }

    public void readAndSendData() {
        System.out.println("The client has started...");

        try {
        // read in the image to be sent
        FileInputStream inputFile = new FileInputStream(fileName);

        float fileSize = (float) inputFile.available()/1024;

        // Network components
        socket = new DatagramSocket(port + 1);
        socket.setSoTimeout(retryTimeout);

        byte[] sendData;
        byte[] ackData = new byte[2];

        DatagramPacket receivePacket = new DatagramPacket(ackData, ackData.length);

        // Tracking sequence number, timeouts and EOF
        short sequenceNumber = 0;
        int eof = 0;

        // the length of the current packet
        int packet_length;
        int total_packets_sent = 0;
        int totalBytesSent = 0; // Total number of bytes sent so far -- debugging purposes
        int dataLeft; // data remaining at each loop

        // while there's still more packets to send
        while (inputFile.available() > 0) {
            dataLeft = inputFile.available();

            // If the amount we have remaining is greater than the packet size
            if (dataLeft > MAX_PACKET_SIZE - 3) {

                // the packet size is 1024 bytes
                packet_length = MAX_PACKET_SIZE - 3;

            } else {
                // if less than max size, we make the packet equal to the amount of data left < 1024 bytes
                packet_length = dataLeft;
                eof = 1; // we signal the end of file
            }
            // to hold the packet
            byte[] data = new byte[packet_length + 3];

            // read data from the 3rd byte onwards byte 0,1,2 are header
            inputFile.read(data, 3, packet_length);

            totalBytesSent += packet_length;

            // Put headers in packet
            sendData = createHeader(data, sequenceNumber, eof);

            // Create packet
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, host, port);

            if (eof == 0) {
                System.out.println(
                        " Number of Bytes Sent: " + totalBytesSent
                                + "\n Sequence Number: " + sequenceNumber
                                + "\n Packet Length:  " + packet_length
                                + "\n Data Length:  " + data.length
                                + "\n ********************************");
            } else {
                System.out.println(
                        " Number of Bytes Sent: " + totalBytesSent
                                + "\n Sequence Number: " + sequenceNumber
                                + "\n Packet Length:  " + packet_length
                                + "\n Data Length:  " + data.length
                                + "\n Total Packets Sent:  " + total_packets_sent
                                + "\n ********************************"
                                + "\n FILE SENT"
                );
            }

            // While no correct ACK is received
            boolean ack = false;
            int incorrectack = 0;
            while(!ack){

                // Send packet
                socket.send(sendPacket);
                total_packets_sent++;
                incorrectack++;

                if (total_packets_sent == 1) {
                    startTime = System.currentTimeMillis();
                }

                if(incorrectack >= 10) {
                    // Clean up
                    System.out.println("\n Number of retransmissions:  " + retransmissions);
                    System.out.println(" Throughput:  " + (totalBytesSent / 1024) / ((System.currentTimeMillis() - startTime)/1000));
                    inputFile.close();
                    socket.close();
                    System.out.println("\n THE CLIENT WAS CLOSED \n");
                }

                retransmissions++;

                // Try receive ACK
                try{
                    // Receive and get data
                    socket.receive(receivePacket);
                    ackData = receivePacket.getData();

                    // Correct ACK?
                    ack = getAckStatus(ackData, sequenceNumber);
                    if (ack == true) retransmissions--;
                } catch (SocketTimeoutException e) {
                    System.out.println(e);
                }
            }

            // Update data
            sequenceNumber = alternateNumber(sequenceNumber);
        }

            // Clean up
            System.out.println("\n Number of retransmissions:  " + retransmissions);
            System.out.println(" Throughput:  " + (totalBytesSent / 1024) / ((System.currentTimeMillis() - startTime)/1000));
            inputFile.close();
            socket.close();
            System.out.println("\n THE CLIENT WAS CLOSED \n");

        } catch (Exception e) {
            System.err.print(e);
        }
    }


    public static void main(String args[]) throws Exception {
        if (args.length == 4) { // valid arguments, specify host
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int retryTimeout = Integer.parseInt(args[3]);
            Sender1b sender1b = new Sender1b(host, port, fileName, retryTimeout);
            sender1b.readAndSendData();

        } else if (args.length > 4) {
            try {
                int windowSize = Integer.parseInt(args[4]);
            } catch (Exception e) {
                System.err.println(e);
            }
        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            " java Sender1b localhost <Port> <Filename> [RetryTimeout]");
        }
    }
}
