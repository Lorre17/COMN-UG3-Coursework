/* Lorena Strechie s1419115 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Sender2b {
    
    // the maximum packet size (1024B data and 3B Header -- [0] & [1] = ACK Number, [2] = EOF Flag
    public static final int MAX_PACKET_SIZE = 1027;

    // arguments
    private int port;
    private String fileName;
    private InetAddress host;
    private int windowSize;
    private int retransmissions = 0;
    
    private int timeout;
    private boolean debug = true;
    private long startTime = 0;
    private DatagramSocket socket = null;
    private ArrayList<Packet> packetsToAck = new ArrayList<Packet>();
    private int ackedPktNum;
    boolean eof = false;
    boolean running = true;
    private long endTime;

    // constructor
    public Sender2b(String host, int port, String fileName, int timeout, int windowSize) throws Exception {

        this.port = port;
        this.fileName = fileName;
        this.timeout = timeout;
        this.startTime = System.currentTimeMillis();
        this.windowSize = windowSize;
        try {
            this.host = InetAddress.getByName(host);
        } catch (Exception e) {
            System.out.print(e);
        }
    }

    // packet object to hold all information about each packet
    public class Packet {
        int number;
        DatagramPacket data;
        long timeStamp;
        boolean ACKed;

        public Packet(int number, DatagramPacket data, long timeStamp) {
            this.number = number;
            this.data = data;
            this.timeStamp = timeStamp;
            this.ACKed = false;
        }
    }

    // Thread that receives acknowledgements
    public class rcv_Ack extends Thread {

        @Override
        public void run() {
            while (running) {

                byte[] buffer = new byte[2];
                DatagramPacket incomingAck = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(incomingAck);
                    socket.setSoTimeout(timeout);
                    byte[] data = incomingAck.getData();
                    ackedPktNum = ByteBuffer.wrap(data).getShort();
                    //System.out.println("ACK NUMBER: " + ackedPktNum);
                } catch (SocketTimeoutException e) {
                    System.out.println(e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread.yield();
            }
        }
    }

    public static byte[] createHeader(byte[] data, int seqNum, boolean eof) {

        data[0] = (byte) ((seqNum >> 8) & 0xff);
        data[1] = (byte) (seqNum & 0xff);
        data[2] = (eof) ? (byte)(1) : (byte)(0);

        return data;
    }

    public void readAndSendData() throws Exception {
        socket = new DatagramSocket();

        FileInputStream inputFile = new FileInputStream(fileName);
        float fileSize = (float) inputFile.available() / 1024;
        int packetLength = 0;
        short packetNo = 0;
        byte[] sendData;
        int dataLeft;
        int totalBytesSent = 0; // Total number of bytes sent so far -- debugging purposes

        rcv_Ack ack = new rcv_Ack();
        ack.start();

        // While the file hasn't been fully transferred
        while (inputFile.available() > 0) {
            // Change acknowledgment value (from false to true)
            // in the packet object when we receive its ack number
            for (int i = 0; i < packetsToAck.size(); i++) {
                Packet packet = packetsToAck.get(i);
                if (packet.number == ackedPktNum && !packet.ACKed) {
                    packet.ACKed = true;
                    packetsToAck.set(i, packet);
                }
            }

            // Slide the window by removing the acknowledged packets
            while (packetsToAck.size() > 0 && packetsToAck.get(0).ACKed) packetsToAck.remove(0);

            // While we still have packets to send and window is not full -> send packets
            while (!eof && packetsToAck.size() < windowSize) {

                dataLeft = inputFile.available();

                // If the amount we have remaining is greater than the packet size
                if (dataLeft > MAX_PACKET_SIZE - 3) {

                    // the packet size is 1024 bytes
                    packetLength = MAX_PACKET_SIZE - 3;

                } else {
                    // if less than max size, we make the packet equal to the amount of data left < 1024 bytes
                    packetLength = dataLeft;
                    eof = true; // we signal the end of file
                    endTime = System.currentTimeMillis();
                }

                // to hold the packet
                byte[] data = new byte[packetLength + 3];

                // read data from the 3rd byte onwards byte 0,1,2 are header
                inputFile.read(data, 3, packetLength);

                totalBytesSent += packetLength;

                // Put headers in packet
                sendData = createHeader(data, packetNo, eof);

                // create Datagram packet and send it to the receiver
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, host, port);
                socket.send(sendPacket);

                // add packets to the buffer of waiting to be acknowledged
                packetsToAck.add(new Packet(packetNo, sendPacket, System.currentTimeMillis()));

                if (debug)
                    System.out.println("number of bytes sent: " + totalBytesSent
                            + "\n number of packets sent: " + packetNo
                            + "\n packet length:  " + packetLength
                            + "\n number of retransmissions: " + retransmissions
                            + "\n time delta: " + ((System.currentTimeMillis() - startTime)/ 1000.0)
                            + "\n transmission rate: " + (totalBytesSent)/((System.currentTimeMillis() - startTime)/ 1000.0)
                            + "\n remaining data: " + dataLeft
                            + "\n last packet: " + ackedPktNum
                            + "\n ********************************");

                packetNo++;
            }

            // Is retransmission needed?
            for (int i = 0; i < packetsToAck.size(); i++) {
                Packet pck = packetsToAck.get(i);
                if (!pck.ACKed && pck.timeStamp + timeout <= System.currentTimeMillis()) {

                    socket.send(pck.data);
                    retransmissions++;

                    packetsToAck.get(i).timeStamp = System.currentTimeMillis();
                    pck.timeStamp = System.currentTimeMillis();
                    packetsToAck.set(i, pck);

                    break;
                }
            }
            
        }
        double throughput =  fileSize / ((endTime - startTime)/1000.0);
        System.out.println("File " + fileName + " has been sent");
        System.out.println("Total Bytes Sent: " + totalBytesSent + ". Last Packet's Length: " + packetLength + ". Transfer time: " + ((endTime - startTime)/1000) + " seconds. Throughput: " + throughput + "KBps. Retransmissions: " + retransmissions);
        System.exit(0);
        running = false;
        socket.close();
    }

    public static void main(String args[]) throws Exception {
        if (args.length == 5) { // valid arguments, specify host
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String fileName = args[2];
            int timeout = Integer.parseInt(args[3]);
            short windowSize = (short) Integer.parseInt(args[4]);
            if (windowSize >= 32767) {
                System.out.println("Invalid window size!");
                System.exit(0);
            }
            Sender2b sender2b = new Sender2b(host, port, fileName, timeout, windowSize);
            sender2b.readAndSendData();

        } else { // invalid arguments
            System.out.println(
                    "Usage: \n" +
                            "java Sender2a_LONG localhost <Port> <fileName> [RetryTimeout] [WindowSize]");
        }
    }
}