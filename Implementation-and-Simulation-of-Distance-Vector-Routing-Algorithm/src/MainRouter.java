import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

class MainThread extends Thread {
    private String vector;
    private int port;
    private String type;
    private int k;

    public MainThread(String type, String vector, int port) {
        this.type = type;
        this.vector = vector;
        this.port = port;
        this.k = 0;
    }

    public MainThread(String type) {
        this.type = type;
    }

    public void run() {
        try {
            if (type.equalsIgnoreCase("r")) {
                MainRouter.readData();
            } else if (type.equalsIgnoreCase("w")) {
                while (k < 5) {
                    MainRouter.read();
                    MainRouter.output();
                    MainRouter.broadCast();

                    Thread.sleep(6000);
                    MainRouter.distanceAlgorithm();

                    Thread.sleep(12000);
                    k++;
                }
            } else if (type.equalsIgnoreCase("u")) {
                MainRouter.updateNetworkVectors(vector.split(":"), port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class MainRouter {
    public static int router_DisplayCount = 1;
    public static double[][] router_NetworkVectors;
    public static int[] router_Ports;
    public static String[] router_Nodes;
    public static int router_id;
    public static double[] router_MyVector;
    public static String[] router_MyHopList;
    public static DatagramSocket router_Socket;
    public static File router_File;
    public static String[] router_Neighbours;

    public static void setParameters(int len, String[] args, int id, String parent) {
        router_NetworkVectors = new double[len][len];
        router_Ports = new int[len];
        router_Nodes = new String[len];

        for (int i = 0; i < len; i++) {
            Arrays.fill(router_NetworkVectors[i], Double.MAX_VALUE);
            router_NetworkVectors[i][i] = 0.0;
            String[] temp = args[i + 3].split(":");
            router_Nodes[i] = temp[0];
            router_Ports[i] = Integer.parseInt(temp[1]);
        }

        router_id = id;
        router_MyVector = new double[len];
        router_MyHopList = new String[len];
        Arrays.fill(router_MyVector, Double.MAX_VALUE);
        router_MyVector[router_id - 1] = 0.0;
        router_File = new File(parent + "/" + router_Nodes[router_id - 1] + ".dat");

        try {
            router_Socket = new DatagramSocket(router_Ports[router_id - 1]);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        System.out.println("Router " + router_Nodes[router_id - 1] + " is Working..!");
    }

    public static void distanceAlgorithm() {
        for (int i = 0; i < router_Neighbours.length; i++) {
            int ind = indexFinder(router_Neighbours[i]);
            for (int j = 0; j < router_MyVector.length; j++) {
                if (j == router_id - 1) {
                    continue;
                } else if (i == 0) {
                    router_MyVector[j] = router_NetworkVectors[router_id - 1][ind] + router_NetworkVectors[ind][j];
                    router_MyHopList[j] = router_Neighbours[i];
                } else {
                    if (router_MyVector[j] > router_NetworkVectors[router_id - 1][ind] + router_NetworkVectors[ind][j]) {
                        router_MyHopList[j] = router_Neighbours[i];
                        router_MyVector[j] = router_NetworkVectors[router_id - 1][ind] + router_NetworkVectors[ind][j];
                    }
                }
            }
        }
    }

    public synchronized static void updateNetworkVectors(String[] vector, int port) {
        int index = indexFinder(port);
        if (index == -1) {
            return; // port not found
        }
        for (int i = 0; i < vector.length; i++) {
            if (i < router_NetworkVectors[index].length) {
                router_NetworkVectors[index][i] = Double.parseDouble(vector[i]);
            }
        }
    }

    public static void broadCast() {
        try {
            for (String neighbour : router_Neighbours) {
                StringBuilder data = new StringBuilder();
                for (int j = 0; j < router_MyVector.length; j++) {
                    if (router_MyHopList[j] != null && neighbour.equals(router_MyHopList[j])) {
                        data.append(Double.MAX_VALUE).append(":");
                    } else {
                        data.append(router_MyVector[j]).append(":");
                    }
                }
                DatagramPacket packet = new DatagramPacket(data.toString().getBytes(), data.length());
                packet.setAddress(InetAddress.getByName("localhost"));
                packet.setPort(router_Ports[indexFinder(neighbour)]);
                router_Socket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void output() {
        System.out.println("> output number " + router_DisplayCount++);
        System.out.println();
        String src = router_Nodes[router_id - 1];
        for (int i = 0; i < router_MyVector.length; i++) {
            if (i != (router_id - 1)) {
                String dest = router_Nodes[i];
                if (router_MyVector[i] == Double.MAX_VALUE) {
                    System.out.println("Optimal path " + src + "-" + dest + ": no route found");
                } else {
                    System.out.println("Optimal path " + src + "-" + dest + ": the next hop is " + router_MyHopList[i] + " and the cost is " + router_MyVector[i]);
                }
            }
        }
    }

    public static void readData() {
        boolean status = true;
        while (status) {
            try {
                String type = "u";
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                router_Socket.receive(packet);
                String vector = new String(packet.getData(), 0, packet.getLength());
                MainThread updateThread = new MainThread(type, vector, packet.getPort());
                updateThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        int total = Integer.parseInt(args[2]);
        int currentNum = Integer.parseInt(args[0]);
        String path = args[1];
        setParameters(total, args, currentNum, path);

        new MainThread("r").start();
        new MainThread("w").start();

        while (true) {
            // keep the main thread alive
        }
    }

    public static void read() {
        try {
            Arrays.fill(router_NetworkVectors[router_id - 1], Double.MAX_VALUE);
            router_NetworkVectors[router_id - 1][router_id - 1] = 0.0;

            BufferedReader br = new BufferedReader(new FileReader(router_File));
            int length = Integer.parseInt(br.readLine());
            router_Neighbours = new String[length];

            for (int i = 0; i < length; i++) {
                String[] temp = br.readLine().split(" ");
                int ind = indexFinder(temp[0]);
                router_Neighbours[i] = temp[0];

                if (ind >= 0) {
                    router_NetworkVectors[router_id - 1][ind] = Double.parseDouble(temp[1]);
                    if (router_DisplayCount == 1) {
                        router_MyHopList[ind] = temp[0];
                        router_MyVector[ind] = Double.parseDouble(temp[1]);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int indexFinder(String node) {
        for (int i = 0; i < router_Nodes.length; i++) {
            if (router_Nodes[i].equals(node)) {
                return i;
            }
        }
        return -1; // return -1 if not found
    }

    public static int indexFinder(int port) {
        for (int i = 0; i < router_Ports.length; i++) {
            if (router_Ports[i] == port) {
                return i;
            }
        }
        return -1; // return -1 if not found
    }
}
