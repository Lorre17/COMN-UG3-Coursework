/* Lorena Strechie s1419115 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Receiver1b {

    public static final int MAX_PACKET_SIZE = 1027;

    private String fileName;
    private DatagramSocket socket;
    private int port;
    private InetAddress host;


    public Receiver1b(int port, String fileName) {
        this.port = port;
        this.fileName = fileName;
    }

    public void sendAck(short seqNum, int port) throws Exception {
        // put in a packet the acknowledged packet's sequence number
        byte[] ackData;
        ackData = ByteBuffer.allocate(2).putShort(seqNum).array();
        // send the ack to the sender
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, host, port);
        socket.send(ackPacket);
    }

    // Alternate between 0 and 1
    public static short alternateNumber(short expectedNumber) {
        return (expectedNumber == 0) ? (short)1 : (short)0;
    }

    public void receiveData() {
        System.out.println("The server was started...");
        try {

            // the maximum size of a buffer.
            byte[] dataBuffer = new byte[MAX_PACKET_SIZE];

            // Open a datagram socket on the specified port number to listen for packets
            socket = new DatagramSocket(port);


            // Create a new file to store the received data in
            File file = new File(fileName);
            FileOutputStream outputFile = new FileOutputStream(file);

            // the received packet
            DatagramPacket receivedPacket = new DatagramPacket(dataBuffer, dataBuffer.length);

            // a count of the number of bytes stored for debug
            int totalBytesReceived = 0;

            boolean eof = false;
            short expectedNumber = 0;

            // until we've seen the byte flag
            while (!eof) {

                // receive a new packet
                socket.receive(receivedPacket);

                // get the data
                byte[] receivedData = receivedPacket.getData();

                int receivedPort = receivedPacket.getPort();

                // get the length of each packet
                int packetLength = receivedPacket.getLength();

                try {
                    host = receivedPacket.getAddress();
                } catch (Exception e) {
                    System.out.print(e);
                }

                // decode the headers in the packet data
                byte[] header = Arrays.copyOfRange(receivedData, 0, 2);

                // identify the sequence number
                short seqNum = ByteBuffer.wrap(header).getShort();

                // find the flag byte and change the bit for last packet
                eof = (receivedData[2] > 0) ? true : false;
                int last_packet = (int) receivedData[2];

                // if the packet we receive is the expected one
                System.out.println("Expected Number is:  " + expectedNumber + " and seqNum is:  " + seqNum);

                // Put ack in packet and send it back
                sendAck(seqNum, receivedPort);

                if(expectedNumber == seqNum) {

                    // Alternate between 0 and 1
                    expectedNumber = alternateNumber(expectedNumber);

                    // Write to file
                    outputFile.write(receivedData, 3, receivedData.length - 3);
                    totalBytesReceived += packetLength - 3;
                }


                if (!eof) {
                    System.out.println(
                            " Received Sequence Number: " + seqNum
                                    + "\n Received Packet Length: " + packetLength
                                    + "\n Received Data Length: " + (packetLength - 3)
                                    + "\n EOF Flag:" + last_packet
                                    + "\n Total Bytes Received: " + totalBytesReceived
                                    + "\n ********************************");
                } else {
                    System.out.println(
                            " Received Sequence Number: " + seqNum
                                    + "\n Received Packet Length: " + packetLength
                                    + "\n Received Data Length: " + (packetLength - 3)
                                    + "\n EOF Flag:" + last_packet
                                    + "\n Total Bytes Received: " + totalBytesReceived
                                    + "\n ********************************"
                                    + "\n FILE RECEIVED"
                    );
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) throws Exception {

        if(args.length == 2) { // we have the 2 valid arguments
            int port = Integer.parseInt(args[0]);
            String fileName = args[1];
            Receiver1b receiver1b = new Receiver1b(port, fileName);
            receiver1b.receiveData();

        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            "java Receiver1b <Port> <Filename> [WindowSize]");
        }
    }
}