import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CabBookingSystem {
    private List<Cab> cabs = new ArrayList<>();
    private List<Booking> bookings = new ArrayList<>();
    private Connection connection;

    // Inner class to define cab properties
    class Cab {
        private String cabId;
        private String cabType;
        private static final double ECONOMY_RATE = 10.0;
        private static final double LUXURY_RATE = 20.0;

        public Cab(String cabId, String cabType) {
            this.cabId = cabId;
            this.cabType = cabType;
        }

        public String getCabType() {
            return cabType;
        }

        public double getRatePerKm() {
            return cabType.equalsIgnoreCase("Luxury") ? LUXURY_RATE : ECONOMY_RATE;
        }

        public String getCabId() {
            return cabId;
        }
    }

    // Inner class to store booking details
    class Booking {
        private String customerId;
        private String customerName;
        private String cabId;
        private String pickupLocation;
        private String dropLocation;
        private double distance;
        private double fare;

        public Booking(String customerId, String customerName, String cabId, String pickupLocation, String dropLocation, double distance, double fare) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.cabId = cabId;
            this.pickupLocation = pickupLocation;
            this.dropLocation = dropLocation;
            this.distance = distance;
            this.fare = fare;
        }

        public String toString() {
            return "Booking Details:\n" +
                    "Customer ID: " + customerId + "\n" +
                    "Customer Name: " + customerName + "\n" +
                    "Cab ID: " + cabId + "\n" +
                    "Pickup Location: " + pickupLocation + "\n" +
                    "Drop Location: " + dropLocation + "\n" +
                    "Distance: " + distance + " km\n" +
                    "Fare: $" + fare;
        }
    }

    // Constructor to establish the connection with MySQL
    public CabBookingSystem() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish the connection
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/CabBookingDB", "root", "1212"); // Replace with your MySQL credentials

            System.out.println("Connected to the database successfully!");
        } catch (Exception e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
        }
    }

    // Add a cab to the system and save to the database
    public void addCab(String cabId, String cabType) {
        try {
            // Check if the cab already exists
            String checkQuery = "SELECT COUNT(*) FROM Cabs WHERE cabId = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, cabId);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("Cab with ID " + cabId + " already exists in the database.");
                    return;
                }
            }

            // Insert the cab into the database
            String query = "INSERT INTO Cabs (cabId, cabType) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, cabId);
                stmt.setString(2, cabType);
                stmt.executeUpdate();
                cabs.add(new Cab(cabId, cabType));
                System.out.println("Cab added successfully: " + cabId + " (" + cabType + ")");
            }
        } catch (SQLException e) {
            System.out.println("Error adding cab to database: " + e.getMessage());
        }
    }

    // Calculate distance between pickup and drop-off (dummy logic)
    private double calculateDistance(String pickupLocation, String dropLocation) {
        return Math.abs(pickupLocation.hashCode() - dropLocation.hashCode()) % 50 + 1;
    }

    // Book a cab and save booking to the database
    public void bookCab(String customerId, String customerName, String cabType, String pickupLocation, String dropLocation) {
        try {
            // Select a cab from the database based on type
            String query = "SELECT * FROM Cabs WHERE cabType = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, cabType);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    System.out.println("No cab available of type " + cabType + ". Try again.");
                    return;
                }

                String cabId = rs.getString("cabId");
                double distance = calculateDistance(pickupLocation, dropLocation);
                double fare = distance * (cabType.equalsIgnoreCase("Luxury") ? 20.0 : 10.0);

                // Insert the booking into the Bookings table
                query = "INSERT INTO Bookings (customerId, customerName, cabId, pickupLocation, dropLocation, distance, fare) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(query)) {
                    insertStmt.setString(1, customerId);
                    insertStmt.setString(2, customerName);
                    insertStmt.setString(3, cabId);
                    insertStmt.setString(4, pickupLocation);
                    insertStmt.setString(5, dropLocation);
                    insertStmt.setDouble(6, distance);
                    insertStmt.setDouble(7, fare);
                    insertStmt.executeUpdate();

                    Booking booking = new Booking(customerId, customerName, cabId, pickupLocation, dropLocation, distance, fare);
                    bookings.add(booking);

                    System.out.println("Booking confirmed!");
                    System.out.println(booking);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error while booking cab: " + e.getMessage());
        }
    }

    // Close database connection
    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing database connection: " + e.getMessage());
        }
    }

    // Main menu
    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nCab Booking System");
            System.out.println("1. Add Cab");
            System.out.println("2. Book Cab");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.print("Enter cab ID: ");
                    String cabId = scanner.nextLine();
                    System.out.print("Enter cab type (Economy/Luxury): ");
                    String cabType = scanner.nextLine();
                    addCab(cabId, cabType);
                    break;
                case 2:
                    System.out.print("Enter customer name: ");
                    String customerName = scanner.nextLine();
                    System.out.print("Enter cab type (Economy/Luxury): ");
                    cabType = scanner.nextLine();
                    System.out.print("Enter pickup location: ");
                    String pickupLocation = scanner.nextLine();
                    System.out.print("Enter drop location: ");
                    String dropLocation = scanner.nextLine();
                    String customerId = "CUST" + (int) (Math.random() * 1000);
                    bookCab(customerId, customerName, cabType, pickupLocation, dropLocation);
                    break;
                case 3:
                    closeConnection();
                    System.out.println("Exiting... Thank you!");
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    public static void main(String[] args) {
        CabBookingSystem system = new CabBookingSystem();
        system.showMenu();
    }
}
