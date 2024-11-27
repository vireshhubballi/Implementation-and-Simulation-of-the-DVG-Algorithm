import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

public class DVR {

	public static void main(String[] args) {

		HashSet<Integer> hashSetPorts = new HashSet<>();
		if (args.length != 1) {
            System.out.println("No more than one argument must be given!");
            return;
        }
		String path = args[0];
		File dir = new File(path);

		if (!dir.isDirectory()) {
			System.out.println("Incorrect Directoy Path! Enter Correct path!");
			return;
		}
		File data[] = dir.listFiles();
		int size = data.length;
		int[] ports = new int[size];
		String allNodes = "";

		System.out.println("Initilize the Port Number for all " + size + " Routers");
		Scanner scanner = new Scanner(System.in);

		for (int i = 0; i < size; i++) {
			String val = data[i].getName();
			if (val.contains(".dat")) {
				val = val.substring(0, val.indexOf(".dat"));
			} else {
				System.out.println("File format error: " + val);
				return;
			}
			
			System.out.println("Enter Port No for Router: " + val);

			boolean status = true;

			while (status) {
				try {
					int num = Integer.parseInt(scanner.nextLine());

					if (num <= 1024 || num >= 65536) {
						throw new NumberFormatException();
					}
					if (hashSetPorts.contains(num)) {
						throw new Exception();
					}
					ports[i] = num;
					hashSetPorts.add(num);
					status = false;
				} catch (NumberFormatException e) {
					System.out.println("Enter a valid Port Number Higher than 1024 and less than 65536");
					status = true;
				} catch (Exception e) {
					System.out.println("Port Number is Already in Use:");
					status = true;
				}
			}
			allNodes += " " + val + ":" + ports[i];
		}

		scanner.close();

		for (int i = 0; i < size; i++) {
			ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "start java MainRouter " + (i + 1) + " \""
					+ data[i].getParent().replace("\\", "/") + "\" " + size + allNodes);
			try {
				processBuilder.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Distance Vector Algorithm Started");

	}
}
