import java.util.List;

import write_database.DatabaseWriter;
import load_csv.CSVLoader;

public class Main {

    public static void main(String[] args) {

        // Declare important variables
        // Like URL to CSV Files and login credentials and URL for the database

        String baseUrlToCSVFiles = "https://raw.githubusercontent.com/Thom-Max/l21s_migration/main/L21s_migration/";
        String[] csvFiles = { "users.csv", "roles.csv" };

        String dbUrl = "jdbc:mysql://db4free.net:3306/l21s_migration";
        String dbUser = "l21s_migration";
        String dbPassword = "l21s_migration";
        String[] dbTables = { "l21s_users", "l21s_user_permissions" };

        // Parameters
        int batchSize = 1000; // Anzahl der Zeilen pro Batch

        // ###############################
        // ###############################

        // Iterate over CSV Files to import them and save them to the database
        for (String csvFile : csvFiles) {

            String urlToCSVFile = baseUrlToCSVFiles + csvFile;

            // Load csv and save data into array

            CSVLoader csvLoader = new CSVLoader();
            List<String[]> csvData = csvLoader.loadCSVData(urlToCSVFile);

            System.out.println("Import from " + csvFile + " in DataArray completed successfully.");

            // Write CSV data to database with delta-method
            // for efficiency, data integrity, flexibility

            // That means,
            // ... only insert when dataset not already exists
            // ... only update when there's something to update
            // ... only delete not more existing rows by soft delete

            DatabaseWriter databaseWriter = new DatabaseWriter();

            if (csvFile.equals("users.csv")) {
                databaseWriter.insertDataInUsers(csvData, dbUrl, dbUser, dbPassword, dbTables[0], batchSize);
            }

            if (csvFile.equals("roles.csv")) {
                databaseWriter.insertDataInUserPermissions(csvData, dbUrl, dbUser, dbPassword, dbTables[1], batchSize);
            }

            System.out.println("Database import for " + csvFile + " completed successfully.");

        }
    }
}
