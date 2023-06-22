package write_database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseWriter {

    /**
     * Inserts the CSV data into the "l21s_users" table or updates existing records.
     *
     * @param csvData    The CSV data to insert into the database.
     * @param dbUrl      The database URL.
     * @param dbUser     The username for the database connection.
     * @param dbPassword The password for the database connection.
     * @param tableName  The name of the table to insert the data into.
     * @param batchSize  The batch size for executing the statements in batches.
     * @throws SQLException If an error occurs while executing the database query.
     * @throws Exception    If an unexpected error occurs while executing
     */
    public void insertDataInUsers(List<String[]> csvData, String dbUrl, String dbUser, String dbPassword,
            String tableName, int batchSize) {

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            // Set the timezone to 'Europe/Berlin'
            String timeZoneQuery = "SET time_zone = 'Europe/Berlin'";
            PreparedStatement timeZoneStatement = connection.prepareStatement(timeZoneQuery);
            timeZoneStatement.executeUpdate();

            String insertQuery = "INSERT INTO " + tableName
                    + " (id, mail, created) VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE "
                    + "mail = CASE WHEN mail <> VALUES(mail) THEN VALUES(mail) ELSE mail END, "
                    + "edited = CASE WHEN mail <> VALUES(mail) THEN NOW() ELSE edited END, "
                    + "deleted = CASE WHEN deleted IS NOT NULL THEN NULL ELSE deleted END";
            PreparedStatement statement = connection.prepareStatement(insertQuery);

            // Batch processing for the INSERT statements
            int count = 0; // Counter for the number of added rows

            for (int i = 1; i < csvData.size(); i++) {
                String[] data = csvData.get(i);
                statement.setString(1, data[0]);
                statement.setString(2, data[1]);
                statement.addBatch();

                count++;

                // Execute the batch if the maximum batch size is reached
                if (count % batchSize == 0) {
                    statement.executeBatch();
                }
            }

            // Execute the remaining batch if there are still rows remaining
            if (count % batchSize != 0) {
                statement.executeBatch();
            }

            // Soft-delete for no longer existing records
            String softDeleteQuery = "UPDATE " + tableName + " SET deleted = NOW() WHERE id NOT IN (";
            for (int i = 1; i < csvData.size(); i++) {
                softDeleteQuery += "?, ";
            }
            softDeleteQuery = softDeleteQuery.substring(0, softDeleteQuery.length() - 2);
            softDeleteQuery += ")"; // Remove the last comma and space

            PreparedStatement softDeleteStatement = connection.prepareStatement(softDeleteQuery);
            for (int i = 1; i < csvData.size(); i++) {
                String[] data = csvData.get(i);
                softDeleteStatement.setString(i, data[0]);
            }
            softDeleteStatement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("An error occurred while executing the database query: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Inserts the CSV data into the "l21s_user_permissions" table or updates
     * existing records
     *
     * @param csvData    The CSV data to insert into the database.
     * @param dbUrl      The database URL.
     * @param dbUser     The username for the database connection.
     * @param dbPassword The password for the database connection.
     * @param tableName  The name of the table to insert the data into.
     * @param batchSize  The batch size for executing the statements in batches.
     * @throws SQLException If an error occurs while executing the database query.
     * @throws Exception    If an unexpected error occurs while executing
     */
    public void insertDataInUserPermissions(List<String[]> csvData, String dbUrl, String dbUser, String dbPassword,
            String tableName, int batchSize) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            // Set the timezone to 'Europe/Berlin'
            String timeZoneQuery = "SET time_zone = 'Europe/Berlin'";
            PreparedStatement timeZoneStatement = connection.prepareStatement(timeZoneQuery);
            timeZoneStatement.executeUpdate();

            // Create the permission types HashMap
            Map<String, Integer> permissionTypeMap = createPermissionTypeMap(connection);

            // Create the SQL statement with the DuplicateKey part
            String insertQuery = "INSERT INTO " + tableName
                    + " (id, user_id, permission_type_id, created) VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE "
                    + "edited = CASE WHEN deleted IS NOT NULL THEN NOW() ELSE edited END, "
                    + "deleted = CASE WHEN deleted IS NOT NULL THEN NULL ELSE deleted END";
            PreparedStatement statement = connection.prepareStatement(insertQuery);

            // Batch processing for the INSERT statements
            int count = 0; // Counter for the number of added rows

            for (int i = 1; i < csvData.size(); i++) {
                String[] data = csvData.get(i);

                // Create the key from user_id and permission_type
                String key = data[0] + "_" + permissionTypeMap.get(data[1]);

                statement.setString(1, key);
                statement.setString(2, data[0]); // User_ID
                statement.setInt(3, permissionTypeMap.get(data[1])); // Permission Type (as Integer value)

                statement.addBatch();

                count++;

                // Execute the batch if the maximum batch size is reached
                if (count % batchSize == 0) {
                    statement.executeBatch();
                }
            }

            // Execute the remaining batch if there are still rows remaining
            if (count % batchSize != 0) {
                statement.executeBatch();
            }

            // Soft-delete for no longer existing records
            String softDeleteQuery = "UPDATE " + tableName + " SET deleted = NOW() WHERE id NOT IN (";
            for (int i = 1; i < csvData.size(); i++) {
                softDeleteQuery += "?, ";
            }
            softDeleteQuery = softDeleteQuery.substring(0, softDeleteQuery.length() - 2);
            softDeleteQuery += ")"; // Remove last comma and space

            PreparedStatement softDeleteStatement = connection.prepareStatement(softDeleteQuery);
            for (int i = 1; i < csvData.size(); i++) {
                String[] data = csvData.get(i);
                String key = data[0] + "_" + permissionTypeMap.get(data[1]);
                softDeleteStatement.setString(i, key);
            }

            softDeleteStatement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("An error occurred while executing the database query: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Creates a HashMap with the existing permission types from the database.
     * 
     * @param connection The database connection.
     * @return A HashMap containing the permission types, where the permission type
     *         is the key and the ID is the value.
     */
    private Map<String, Integer> createPermissionTypeMap(Connection connection) throws SQLException {

        Map<String, Integer> permissionTypeMap = new HashMap<>();

        // Create the SQL statement to retrieve the current permission types
        String permissionTypesQuery = "SELECT id, permission_type FROM l21s_permission_types";
        PreparedStatement permissionTypesStatement = connection.prepareStatement(permissionTypesQuery);
        ResultSet permissionTypesResult = permissionTypesStatement.executeQuery();

        while (permissionTypesResult.next()) {
            int permissionTypeId = permissionTypesResult.getInt("id");
            String permissionType = permissionTypesResult.getString("permission_type");
            permissionTypeMap.put(permissionType, permissionTypeId);
        }

        return permissionTypeMap;
    }

}
